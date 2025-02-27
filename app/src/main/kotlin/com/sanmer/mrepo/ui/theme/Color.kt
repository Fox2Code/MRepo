package com.sanmer.mrepo.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.sanmer.mrepo.ui.theme.color.BlueDarkColorScheme
import com.sanmer.mrepo.ui.theme.color.BlueLightColorScheme
import com.sanmer.mrepo.ui.theme.color.CyanDarkColorScheme
import com.sanmer.mrepo.ui.theme.color.CyanLightColorScheme
import com.sanmer.mrepo.ui.theme.color.DeepPurpleDarkColorScheme
import com.sanmer.mrepo.ui.theme.color.DeepPurpleLightColorScheme
import com.sanmer.mrepo.ui.theme.color.OrangeDarkColorScheme
import com.sanmer.mrepo.ui.theme.color.OrangeLightColorScheme
import com.sanmer.mrepo.ui.theme.color.SakuraDarkColorScheme
import com.sanmer.mrepo.ui.theme.color.SakuraLightColorScheme

sealed class Colors(
    val id: Int,
    val lightColorScheme: ColorScheme,
    val darkColorScheme: ColorScheme
) {
    class Dynamic(context: Context) : Colors(
        id = -1,
        lightColorScheme = dynamicLightColorScheme(context),
        darkColorScheme = dynamicDarkColorScheme(context)
    ) {
        companion object {
            const val id = -1
        }
    }
    object Sakura : Colors(
        id = 0,
        lightColorScheme = SakuraLightColorScheme,
        darkColorScheme = SakuraDarkColorScheme
    )
    object DeepPurple : Colors(
        id = 1,
        lightColorScheme = DeepPurpleLightColorScheme,
        darkColorScheme = DeepPurpleDarkColorScheme
    )
    object Blue: Colors(
        id = 2,
        lightColorScheme = BlueLightColorScheme,
        darkColorScheme = BlueDarkColorScheme
    )
    object Cyan: Colors(
        id = 3,
        lightColorScheme = CyanLightColorScheme,
        darkColorScheme = CyanDarkColorScheme
    )
    object Orange: Colors(
        id = 4,
        lightColorScheme = OrangeLightColorScheme,
        darkColorScheme = OrangeDarkColorScheme
    )
}

private val colors = listOf(
    Colors.Sakura,
    Colors.DeepPurple,
    Colors.Blue,
    Colors.Cyan,
    Colors.Orange,
)

fun getColors(): List<Int> {
    return colors.map { it.id }
}

@Composable
fun getColor(id: Int): Colors {
    val context = LocalContext.current

    return if (id == Colors.Dynamic.id) {
        Colors.Dynamic(context)
    } else {
        colors[id]
    }
}