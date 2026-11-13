package com.maxrave.simpmusic.expect.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.maxrave.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/*
 * Desktop "Wallpaper" theme colour (fork): a desktop has no Material You, but one themed with
 * matugen has the same thing as a file. A matugen template writes every Material 3 role, dark and
 * light, as `{"dark": {"primary": "#a9c7ff", ...}, "light": {...}}` (matugen's own role names), and
 * this reads it. The file is polled rather than read once, so a new wallpaper re-themes the app
 * while it runs.
 *
 * Path: -Dsimpmusic.matugen.colors, else $SIMPMUSIC_MATUGEN_COLORS, else
 * ~/.cache/matugen/simpmusic-colors.json. Without the file, "Wallpaper" is not offered.
 */

private const val TAG = "MatugenTheme"
private const val POLL_MS = 2_000L

private val colorsFile: File =
    File(
        System.getProperty("simpmusic.matugen.colors")
            ?: System.getenv("SIMPMUSIC_MATUGEN_COLORS")
            ?: (System.getProperty("user.home") + "/.cache/matugen/simpmusic-colors.json"),
    )

/** Both halves of the palette, or null while there is no readable file. */
private class MatugenPalette(
    val dark: ColorScheme,
    val light: ColorScheme,
)

private object MatugenColors {
    private val _palette = MutableStateFlow<MatugenPalette?>(null)
    val palette: StateFlow<MatugenPalette?> = _palette.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            var seen = Long.MIN_VALUE
            while (true) {
                val modified = colorsFile.lastModified()
                if (modified != seen) {
                    seen = modified
                    // A file caught mid-write fails to parse; the previous palette stays until the
                    // next poll reads the finished one.
                    read()?.let { _palette.value = it }
                }
                delay(POLL_MS)
            }
        }
    }

    private fun read(): MatugenPalette? =
        runCatching {
            if (!colorsFile.isFile) return null
            val root = json.parseToJsonElement(colorsFile.readText()).jsonObject
            MatugenPalette(
                dark = scheme(root.getValue("dark").jsonObject, darkColorScheme()),
                light = scheme(root.getValue("light").jsonObject, lightColorScheme()),
            ).also { Logger.i(TAG, "palette loaded, source ${root["source_color"]?.jsonPrimitive?.content}") }
        }.onFailure { Logger.w(TAG, "${colorsFile.name} unreadable: ${it.message}") }
            .getOrNull()

    /** Every role the file names; any it leaves out keeps Material's baseline. */
    private fun scheme(
        roles: JsonObject,
        base: ColorScheme,
    ): ColorScheme {
        fun c(
            name: String,
            fallback: Color,
        ): Color =
            roles[name]
                ?.jsonPrimitive
                ?.content
                ?.removePrefix("#")
                ?.takeIf { it.length == 6 }
                ?.toLongOrNull(16)
                ?.let { Color(0xFF000000 or it) }
                ?: fallback
        return base.copy(
            primary = c("primary", base.primary),
            onPrimary = c("on_primary", base.onPrimary),
            primaryContainer = c("primary_container", base.primaryContainer),
            onPrimaryContainer = c("on_primary_container", base.onPrimaryContainer),
            inversePrimary = c("inverse_primary", base.inversePrimary),
            secondary = c("secondary", base.secondary),
            onSecondary = c("on_secondary", base.onSecondary),
            secondaryContainer = c("secondary_container", base.secondaryContainer),
            onSecondaryContainer = c("on_secondary_container", base.onSecondaryContainer),
            tertiary = c("tertiary", base.tertiary),
            onTertiary = c("on_tertiary", base.onTertiary),
            tertiaryContainer = c("tertiary_container", base.tertiaryContainer),
            onTertiaryContainer = c("on_tertiary_container", base.onTertiaryContainer),
            background = c("background", base.background),
            onBackground = c("on_background", base.onBackground),
            surface = c("surface", base.surface),
            onSurface = c("on_surface", base.onSurface),
            surfaceVariant = c("surface_variant", base.surfaceVariant),
            onSurfaceVariant = c("on_surface_variant", base.onSurfaceVariant),
            surfaceTint = c("surface_tint", base.surfaceTint),
            inverseSurface = c("inverse_surface", base.inverseSurface),
            inverseOnSurface = c("inverse_on_surface", base.inverseOnSurface),
            error = c("error", base.error),
            onError = c("on_error", base.onError),
            errorContainer = c("error_container", base.errorContainer),
            onErrorContainer = c("on_error_container", base.onErrorContainer),
            outline = c("outline", base.outline),
            outlineVariant = c("outline_variant", base.outlineVariant),
            scrim = c("scrim", base.scrim),
            surfaceBright = c("surface_bright", base.surfaceBright),
            surfaceDim = c("surface_dim", base.surfaceDim),
            surfaceContainerLowest = c("surface_container_lowest", base.surfaceContainerLowest),
            surfaceContainerLow = c("surface_container_low", base.surfaceContainerLow),
            surfaceContainer = c("surface_container", base.surfaceContainer),
            surfaceContainerHigh = c("surface_container_high", base.surfaceContainerHigh),
            surfaceContainerHighest = c("surface_container_highest", base.surfaceContainerHighest),
            primaryFixed = c("primary_fixed", base.primaryFixed),
            primaryFixedDim = c("primary_fixed_dim", base.primaryFixedDim),
            onPrimaryFixed = c("on_primary_fixed", base.onPrimaryFixed),
            onPrimaryFixedVariant = c("on_primary_fixed_variant", base.onPrimaryFixedVariant),
            secondaryFixed = c("secondary_fixed", base.secondaryFixed),
            secondaryFixedDim = c("secondary_fixed_dim", base.secondaryFixedDim),
            onSecondaryFixed = c("on_secondary_fixed", base.onSecondaryFixed),
            onSecondaryFixedVariant = c("on_secondary_fixed_variant", base.onSecondaryFixedVariant),
            tertiaryFixed = c("tertiary_fixed", base.tertiaryFixed),
            tertiaryFixedDim = c("tertiary_fixed_dim", base.tertiaryFixedDim),
            onTertiaryFixed = c("on_tertiary_fixed", base.onTertiaryFixed),
            onTertiaryFixedVariant = c("on_tertiary_fixed_variant", base.onTertiaryFixedVariant),
        )
    }
}

@Composable
actual fun platformDynamicColorScheme(isDark: Boolean): ColorScheme? {
    val palette by remember { MatugenColors.palette }.collectAsState()
    return palette?.let { if (isDark) it.dark else it.light }
}

actual fun isWallpaperDynamicColorSupported(): Boolean = colorsFile.isFile

@Composable
actual fun SystemBarAppearanceEffect(isDark: Boolean) {
    // No system bars to adapt on desktop.
}
