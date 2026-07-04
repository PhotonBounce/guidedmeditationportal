# Guided Meditation Portal — Release Notes

## v1.4.1 (versionCode 12) — July 3, 2026

### 🏪 Play Console "What's new" (paste into the release — max 500 chars)

**English:**
```
✨ What's new in 1.4.1
• Full Android 15 edge-to-edge display — beautiful on every screen
• Breathing exercise is fully immersive again
• Chat: the keyboard no longer covers your message box
• Updated billing, ads & privacy compliance (Play Billing 8)
• Bug fixes and polish throughout
```

**Deutsch:**
```
✨ Neu in 1.4.1
• Volle Android-15-Edge-to-Edge-Anzeige auf jedem Bildschirm
• Atemübung wieder komplett im Vollbild
• Chat: Die Tastatur verdeckt das Eingabefeld nicht mehr
• Aktualisierte Abrechnungs-, Werbe- & Datenschutz-Compliance
• Fehlerbehebungen und Feinschliff
```

**Français:**
```
✨ Nouveautés de la 1.4.1
• Affichage bord à bord Android 15 sur tous les écrans
• Exercice de respiration à nouveau en plein écran immersif
• Chat : le clavier ne masque plus la zone de saisie
• Conformité facturation, publicités et confidentialité à jour
• Corrections de bugs et améliorations
```

**Español:**
```
✨ Novedades de la 1.4.1
• Pantalla completa de borde a borde en Android 15
• El ejercicio de respiración vuelve a ser totalmente inmersivo
• Chat: el teclado ya no tapa el cuadro de mensaje
• Cumplimiento actualizado de facturación, anuncios y privacidad
• Corrección de errores y mejoras
```

---

### 🔧 Technical changelog (v11 → v12)

**Play Store compliance (fixes the Policy status issues):**
- **Privacy Policy** — comprehensive GDPR/CCPA policy now covers AdMob, Play Billing, the fully on-device Spirit chat, data retention & deletion. Canonical URL (enter in Play Console → App content → Privacy policy):
  `https://www.photon-bounce.com/guidedmeditation/privacy.html`
- **Google Play Billing 6.2.1 → 8.2.0** — clears the "must use 7.0.0+" violation AND the upcoming "must use 8.0.0+ from Aug 31, 2026" deadline in one move
- **Edge-to-edge (Android 15 / SDK 35)** — removed every deprecated window API (`statusBarColor`, `navigationBarColor`, `enforceStatusBarContrast`, `windowFullscreen`); all 8 screens migrated to `enableEdgeToEdge()` with proper window-insets handling

**Library upgrades:**
- Google Mobile Ads SDK 23.1.0 → **25.4.0** (latest)
- User Messaging Platform (consent) 2.2.0 → **4.0.0** (latest)
- Kotlin 1.9.22 → **2.2.0** (required by the new Google libraries)
- androidx.activity 1.9.3 added (`enableEdgeToEdge`)

**Fixes found by QA:**
- System-bar icons stay white on devices set to light theme (app is always dark)
- No more white navigation-bar strip on Android 8–9 devices in light theme
- Breathing exercise restored to true fullscreen (status bar hidden, swipe to reveal)
- Spirit chat input no longer hidden behind the keyboard
- Splash keeps its immersive fullscreen via the modern insets API

**Requirements change:**
- Minimum Android version is now **6.0 (API 23)**, required by Google Mobile Ads SDK 24+. Android 5.x (~0.1% of devices) can no longer install updates.

---

### 📋 Release checklist

1. ☐ Merge PR #3 into `main`
2. ☐ Run **Release AAB (signed)** workflow → download `app-release.aab` (v12 / 1.4.1)
3. ☐ Upload the AAB to Play Console → Closed testing (or Production)
4. ☐ Paste the "What's new" text above into the release notes (all 4 languages)
5. ☐ Play Console → App content → Privacy policy → `https://www.photon-bounce.com/guidedmeditation/privacy.html`
6. ☐ Run **Deploy Website** workflow (FTP password) — publishes new privacy/terms + v1.4.1 site
7. ☐ Run **Release APK** then **APK to Website** workflows — puts the v1.4.1 test APK on the microsite
8. ☐ Review "third-party app stores" preference in Play Console (deadline Jul 22)
