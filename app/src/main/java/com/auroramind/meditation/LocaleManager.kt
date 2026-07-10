package com.auroramind.meditation

import android.app.Activity
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Per-app language switching for Power of Mind.
 *
 * Uses AppCompatDelegate.setApplicationLocales (backported to API 21). Selecting
 * a language recreates the running activities immediately; with the
 * AppLocalesMetadataHolderService declared in the manifest it also persists the
 * choice across launches on API < 33 (API 33+ stores it in the system).
 *
 * The chosen BCP47 tag is mirrored into prefs so the picker can highlight the
 * current selection without the legacy he→iw / yi→ji language-code normalisation
 * getting in the way.
 */
object LocaleManager {

    data class Lang(val tag: String, val nativeName: String, val rtl: Boolean = false)

    /** Offered languages, in picker order. */
    val SUPPORTED = listOf(
        Lang("en", "English"),
        Lang("uk", "Українська"),
        Lang("es", "Español"),
        Lang("fr", "Français"),
        Lang("de", "Deutsch"),
        Lang("ru", "Русский"),
        Lang("he", "עברית", rtl = true),
        Lang("yi", "ייִדיש", rtl = true),
    )

    /** The active language tag ("en" if the user hasn't chosen one). */
    fun currentTag(context: Context): String {
        val saved = PrefsManager(context).getLanguageTag()
        if (saved.isNotBlank()) return saved
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return "en"
        val lang = locales[0]?.language ?: return "en"
        // Map the JVM legacy codes back to the BCP47 tags we key on.
        return when (lang) {
            "iw" -> "he"
            "ji" -> "yi"
            else -> lang
        }
    }

    fun apply(context: Context, tag: String) {
        PrefsManager(context).setLanguageTag(tag)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    /** Shows the language chooser; applying a choice recreates the activity. */
    fun showPicker(activity: Activity) {
        val names = SUPPORTED.map { it.nativeName }.toTypedArray()
        val checked = SUPPORTED.indexOfFirst { it.tag == currentTag(activity) }.coerceAtLeast(0)
        MaterialAlertDialogBuilder(activity, R.style.AlertDialogDark)
            .setTitle(R.string.language_title)
            .setSingleChoiceItems(names, checked) { dialog, which ->
                dialog.dismiss()
                if (SUPPORTED[which].tag != currentTag(activity)) apply(activity, SUPPORTED[which].tag)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
