package com.imagesorter.ui

import android.content.Context
import com.imagesorter.domain.MediaKind

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Ajustes del usuario (pantalla Ajustes). Se guardan en el teléfono con [SettingsStore]. */
data class AppSettings(
    /** Cuánto hay que deslizar una foto para decidir, como fracción del ancho. */
    val swipeThreshold: Float = SWIPE_DEFAULT,
    /** Lados invertidos: izquierda = conservar, derecha = borrar. */
    val swapSides: Boolean = false,
    /** Tocar una mitad de la foto decide. Si no, solo deslizar (evita decisiones por un toque sin querer). */
    val tapToDecide: Boolean = true,
    val vibrate: Boolean = true,
    val oldestFirst: Boolean = false,
    val includeVideos: Boolean = true,
    val theme: ThemeMode = ThemeMode.SYSTEM,
) {
    /** Lo que aparece en carpetas y mazos. */
    val kinds: Set<MediaKind> get() = if (includeVideos) MediaKind.entries.toSet() else setOf(MediaKind.IMAGE)

    companion object {
        const val SWIPE_DEFAULT = 0.2f
        const val SWIPE_MIN = 0.1f
        const val SWIPE_MAX = 0.5f
    }
}

object SettingsStore {
    // "swipe_threshold" es la clave de versiones anteriores: la distancia elegida se conserva al actualizar.
    private const val SWIPE = "swipe_threshold"
    private const val SWAP = "swap_sides"
    private const val TAP = "tap_to_decide"
    private const val VIBRATE = "vibrate"
    private const val OLDEST = "oldest_first"
    private const val VIDEOS = "include_videos"
    private const val THEME = "theme"

    fun load(context: Context): AppSettings {
        val p = prefs(context)
        val d = AppSettings()
        return AppSettings(
            swipeThreshold = p.getFloat(SWIPE, d.swipeThreshold).coerceIn(AppSettings.SWIPE_MIN, AppSettings.SWIPE_MAX),
            swapSides = p.getBoolean(SWAP, d.swapSides),
            tapToDecide = p.getBoolean(TAP, d.tapToDecide),
            vibrate = p.getBoolean(VIBRATE, d.vibrate),
            oldestFirst = p.getBoolean(OLDEST, d.oldestFirst),
            includeVideos = p.getBoolean(VIDEOS, d.includeVideos),
            theme = ThemeMode.entries.firstOrNull { it.name == p.getString(THEME, null) } ?: d.theme,
        )
    }

    fun save(context: Context, s: AppSettings) {
        prefs(context).edit()
            .putFloat(SWIPE, s.swipeThreshold.coerceIn(AppSettings.SWIPE_MIN, AppSettings.SWIPE_MAX))
            .putBoolean(SWAP, s.swapSides)
            .putBoolean(TAP, s.tapToDecide)
            .putBoolean(VIBRATE, s.vibrate)
            .putBoolean(OLDEST, s.oldestFirst)
            .putBoolean(VIDEOS, s.includeVideos)
            .putString(THEME, s.theme.name)
            .apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
}
