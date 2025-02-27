package com.sanmer.mrepo.ui.screens.home

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sanmer.mrepo.R
import com.sanmer.mrepo.app.event.isSucceeded
import com.sanmer.mrepo.repository.LocalRepository
import com.sanmer.mrepo.viewmodel.HomeViewModel

@Composable
fun InfoItem(
    viewModel: HomeViewModel = hiltViewModel()
) = OutlinedCard(
    shape = RoundedCornerShape(20.dp)
) {
    val suEvent by viewModel.suState.collectAsStateWithLifecycle()
    val count by viewModel.count.collectAsStateWithLifecycle(LocalRepository.Count.zeros())

    Column(
        modifier = Modifier
            .padding(all = 20.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        InfoItem(
            key = stringResource(id = R.string.modules_status_repo),
            value = "${count.enable} / ${count.all}"
        )

        InfoItem(
            key = stringResource(id = R.string.modules_status_module),
            value = "${count.local} / ${count.online}"
        )

        InfoItem(
            key = stringResource(id = R.string.device_name),
            value = getDevice()
        )

        InfoItem(
            key = stringResource(id = R.string.device_system_version),
            value = if (Build.VERSION.PREVIEW_SDK_INT != 0) {
                "${Build.VERSION.CODENAME} Preview (API ${Build.VERSION.SDK_INT})"
            } else {
                "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            }
        )

        InfoItem(
            key = stringResource(id = R.string.device_system_abi),
            value = Build.SUPPORTED_ABIS[0]
        )

        InfoItem(
            key = stringResource(id = R.string.device_security_patch),
            value = Build.VERSION.SECURITY_PATCH
        )

        if (suEvent.isSucceeded) {
            InfoItem(
                key = stringResource(id = R.string.device_selinux_status),
                value = when (viewModel.enforce) {
                    0 -> stringResource(id = R.string.selinux_status_permissive)
                    1 -> stringResource(id = R.string.selinux_status_enforcing)
                    else -> stringResource(id = R.string.selinux_status_disabled)
                }
            )
        }
    }
}

@Composable
private fun InfoItem(
    key: String,
    value: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = key,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun getDevice(): String {
    var manufacturer =
        Build.MANUFACTURER[0].uppercaseChar().toString() + Build.MANUFACTURER.substring(1)
    if (Build.BRAND.lowercase() != Build.MANUFACTURER.lowercase()) {
        manufacturer += " " + Build.BRAND[0].uppercaseChar() + Build.BRAND.substring(1)
    }
    manufacturer += " " + Build.MODEL + " "
    return manufacturer
}