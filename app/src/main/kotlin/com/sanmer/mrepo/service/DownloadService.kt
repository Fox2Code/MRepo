package com.sanmer.mrepo.service

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import android.os.Process
import androidx.core.app.ServiceCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.sanmer.mrepo.BuildConfig
import com.sanmer.mrepo.R
import com.sanmer.mrepo.app.Const
import com.sanmer.mrepo.app.utils.MediaStoreUtils.newOutputStream
import com.sanmer.mrepo.app.utils.NotificationUtils
import com.sanmer.mrepo.ui.activity.install.InstallActivity
import com.sanmer.mrepo.ui.activity.main.MainActivity
import com.sanmer.mrepo.utils.HttpUtils
import com.sanmer.mrepo.utils.expansion.parcelable
import com.sanmer.mrepo.utils.expansion.toFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import java.io.File

class DownloadService : LifecycleService() {
    private val context by lazy { this }
    private val tasks = mutableListOf<Task>()
    private var lastTime = System.currentTimeMillis()

    init {
        val notification = NotificationUtils
            .buildNotification(context, Const.CHANNEL_ID_DOWNLOAD)
            .setContentIntent(NotificationUtils.getActivity(MainActivity::class))
            .setProgress(0, 0 , false)
            .setOngoing(true)
            .setGroup(GROUP_KEY)

        progress.sample(500)
            .flowOn(Dispatchers.IO)
            .onEach { (value, task) ->
                lastTime = System.currentTimeMillis()

                val p = (value * 100).toInt()
                if (p == 100 || p == 0) {
                    return@onEach
                }

                NotificationUtils.notify(task!!.id,
                    notification.setContentTitle(task.name)
                        .setProgress(100, p, false)
                        .build()
                )
            }.launchIn(lifecycleScope)
    }

    override fun onCreate() {
        super.onCreate()
        setForeground()

        lifecycleScope.launch(Dispatchers.Default) {
            while (isActive) {
                System.currentTimeMillis().let {
                    if (it - lastTime >= 30 * 1000) stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.parcelable<Task>(DOWNLOAD_TASK)?.let {
            if (it !in tasks) {
                downloader(it)
                tasks.add(it)
            } else {
                Timber.d("download: ${it.name} is already in list")
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
    }

    private fun downloader(task: Task) = lifecycleScope.launch {
        val notificationId = task.id
        val notificationIdFinish = notificationId + 1

        Timber.d("download: ${task.url} to ${task.path}")
        val path = task.path.toFile().apply {
            parentFile!!.let {
                if (!it.exists()) it.mkdirs()
            }
        }

        val succeeded: () -> Unit = {
            NotificationUtils.cancel(notificationId)

            val message = getString(R.string.message_download_success)
            notifyFinish(
                id = notificationIdFinish,
                name = task.name,
                message = message,
                silent = true
            )

            if (task.install && path.name.endsWith("zip")) {
                InstallActivity.start(context = context, path = path)
            }

            if (task.install && path.name.endsWith("apk")) {
                runCatching {
                    apkInstall(path)
                }.onFailure {
                    Timber.e(it, "apk install failed")
                }
            }

            tasks.remove(task)
        }

        val failed: (String?) -> Unit = {
            updateProgress(0f, task)
            NotificationUtils.cancel(notificationId)

            val message = getString(R.string.message_download_failed, it)
            notifyFinish(
                id = notificationIdFinish,
                name = task.name,
                message = message
            )

            tasks.remove(task)
        }

        val output = try {
            path.newOutputStream()!!
        } catch (e: Exception) {
            failed(e.message)
            return@launch
        }

        HttpUtils.downloader(
            url = task.url,
            output = output,
            onProgress = { updateProgress(it, task) }
        ).onSuccess {
            succeeded()
        }.onFailure {
            failed(it.message)
        }
    }

    private fun apkInstall(path: File) {
        val apk = cacheDir.resolve("app-release.apk").apply {
            delete()
        }
        contentResolver.openInputStream(path.toUri())!!.use {
            it.copyTo(apk.outputStream())
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val apkUri = FileProvider.getUriForFile(context,
            "${BuildConfig.APPLICATION_ID}.provider", apk)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")

        startActivity(intent)
    }

    private fun setForeground() {
        startForeground(Process.myPid(),
            NotificationUtils.buildNotification(this, Const.CHANNEL_ID_DOWNLOAD)
                .setSilent(true)
                .setContentIntent(NotificationUtils.getActivity(MainActivity::class))
                .setGroup(GROUP_KEY)
                .setGroupSummary(true)
                .build()
        )
    }

    private fun notifyFinish(
        id: Int,
        name: String,
        message: String,
        silent: Boolean = false
    ) = NotificationUtils.notify(this, id) {
        setChannelId(Const.CHANNEL_ID_DOWNLOAD)
        setContentTitle(name)
        setContentText(message)
        setSilent(silent)
        setContentIntent(NotificationUtils.getActivity(MainActivity::class))
        setGroup(GROUP_KEY)
        build()
    }

    companion object {
        private const val DOWNLOAD_TASK = "DOWNLOAD_TASK"
        private const val GROUP_KEY = "DOWNLOAD_SERVICE_GROUP_KEY"

        @Parcelize
        data class Task(
            val id: Int = System.currentTimeMillis().toInt(),
            val name: String,
            val path: String,
            val url: String,
            val install: Boolean
        ) : Parcelable {
            override fun equals(other: Any?): Boolean {
                return when (other) {
                    is Task -> url == other.url
                    else -> false
                }
            }

            override fun hashCode(): Int {
                return url.hashCode()
            }
        }

        private val progress = MutableStateFlow<Pair<Float, Task?>>(0f to null)
        private fun updateProgress(progress: Float, item: Task) {
            this.progress.value = progress to item
        }
        fun getProgress(get: (Task) -> Boolean) =
            progress.map { (value, task) ->
                if (task != null && get(task)) {
                    value
                } else {
                    0f
                }
            }

        fun start(
            context: Context,
            name: String, path: String,
            url: String, install: Boolean
        ) {
            val task = Task(
                name = name,
                path = path,
                url = url,
                install = install
            )
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(DOWNLOAD_TASK, task)
            }
            context.startService(intent)
        }

        fun start(
            context: Context,
            name: String, path: File,
            url: String, install: Boolean
        ) = start(context, name,  path.absolutePath, url, install)
    }
}