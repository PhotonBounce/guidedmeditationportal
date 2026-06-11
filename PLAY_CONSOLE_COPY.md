# Play Console — Copy-Paste Reference (Guided Meditation Portal)

Every field Google asks for, pre-written for the current app. Paste verbatim.
Last updated for **v1.3.0 (versionCode 10)**.

---

## Create app

| Field | Value |
|---|---|
| App name | Guided Meditation Portal |
| Package | com.auroramind.meditation |
| Default language | English – en-US |
| App or game | App |
| Free or paid | Free (with one-time in-app unlock) |
| Declarations | ✓ Developer Program Policies · ✓ US export laws |

---

## Main store listing

### App name (30 char max — this is 24)
```
Guided Meditation Portal
```

### Short description (80 char max — this is 76)
```
Calm your mind: guided meditations, breathing, streaks & a gentle companion.
```

### Full description (paste verbatim — ~2,300 chars, limit 4,000)
```
Guided Meditation Portal — a calmer mind, one breath at a time.

Thirty-eight guided meditations & soundscapes, a breathing coach, an on-device companion called Spirit, and a gentle meditation alarm — wrapped in a living, deep-pink cosmic interface that shifts color with every track. No subscriptions. No clutter. Just calm.

🧘 38 MEDITATIONS & SOUNDSCAPES
Softly narrated sessions for sleep, stress & anxiety, grounding, and self-compassion. Now featuring lo-fi beats in our "Focus Music" genre and electronic waves in our "Energy Music" genre. Start free with three full meditations — unlock the rest forever for one tiny price.

🫧 BREATHING COACH & QUICK CALM
Follow a glowing orb through calming box-breathing (in 4, hold 4, out 4, hold 4), with gentle audio chime beeps at the top and bottom of the cycle so you can practice with eyes closed. Or tap Quick Calm for a 60-second reset whenever the day gets loud.

🔥 STREAKS & PROGRESS
Build a daily habit that sticks. Track your streak, total sessions, and minutes meditated — with a forgiving grace day so one busy day never breaks your momentum.

🗺️ GUIDED JOURNEYS
Multi-day programs — 7 Days to Calmer Sleep, 5 Days Less Anxious, 7 Days of Focus, and 5 Days of Self-Compassion — each day pairs a short practice with the right meditation.

🤍 SPIRIT — YOUR COMPANION
An on-device guide that suggests the right practice for how you feel and walks you through real techniques: breathwork, body scan, loving-kindness, and more.

🎯 BROWSE BY HOW YOU FEEL
Filter the library by goal — Sleep, Stress & Anxiety, Focus, Grounding, Self-Compassion — and star favorites to keep them one tap away.

🌙 A GENTLER WAKE-UP
Set a meditation alarm that wakes you to a favorite narrated track or a soft tone — softer mornings instead of a jarring buzzer.

🔔 REMINDERS THAT TEACH
An optional daily nudge at your chosen time — and every reminder carries a bite-sized technique tuned to your goal, so each ping is a 30-second practice, not a nag.

⏱ SLEEP TIMER & JUKEBOX
Drift off with a fade-out timer up to 8 hours. Shuffle, repeat, and queue the whole library. Plays with the screen off, all night.

📲 HOME-SCREEN WIDGET
Keep your streak and today's quick technique one tap from your home screen.

💎 14-DAY FREE TRIAL & ONE-TIME UNLOCK
Try the entire app — all 38 tracks, Spirit, and the meditation alarm — completely free for 14 days. After the trial, a single $2.00 unlock gives you everything forever. No subscriptions, no recurring charges, and no surprise renewals. More meditations and new languages are on the way.

Made with ♥ for restless sleepers, anxious minds, and anyone seeking a few quiet minutes. 🌙

Spirit offers general wellness guidance and is not a substitute for professional medical or mental-health care.
```

### Category & tags
- App category: **Health & Fitness**
- Tags (up to 5): Meditation · Sleep · Relaxation · Mindfulness · Breathing

### Contact details
- Email: `support.meditationportal@gmail.com`
- Website: `https://www.photon-bounce.com`
- Phone: leave blank

### External marketing
- Allow Google to promote the app: **Yes** (free reach)

---

## Graphic assets

| Asset | Spec | File in repo |
|---|---|---|
| App icon | 512×512 PNG, no alpha | `app/src/main/appicon/play_store_icon_512.png` |
| Feature graphic | 1024×500 PNG | `marketing/feature_graphic.png` |
| Phone screenshots | 1080×2400 PNG, 2–8 | `qa_full/Q02_home_idle.png`, `Q03_home_playing.png`, `Q06_breathing.png`, `Q07_spirit.png`, `Q11_progress.png` (retake on a clean device for production) |

> The QA screenshots are from a debug device with a status-bar clock/notch.
> For polished store screenshots, capture on your phone with a clean status bar,
> or frame them in a device mockup.

---

## App content → answers

| Task | Answer |
|---|---|
| Privacy policy | `https://www.photon-bounce.com/meditation-portal/privacy.html` |
| App access | All functionality available without restrictions |
| Ads | **Yes**, contains ads (AdMob banner/interstitial/rewarded; removed by unlock) |
| Content rating | Run questionnaire → all "None"; expected **Everyone** |
| Target audience | 18 and over · Appeals to children: **No** |
| News app | No |
| Data safety | See section below |
| Government / Financial / Health | No (Spirit has a "not medical advice" disclaimer — keep Health = No) |

---

## Data safety form

**Collects or shares user data?** → Yes

1. **Advertising ID** (Device or other IDs) — Collected: Yes · Shared: No · Required · Purpose: Advertising (non-personalized) · Encrypted in transit: Yes · Deletion: No (resettable in Android settings)
2. **App interactions / crash logs** (App activity) — Collected: Yes · Shared: No · Required · Purpose: App functionality, Analytics · Encrypted: Yes · Deletion: No (cleared on uninstall)
3. **Purchase history** (Financial info) — Collected: Yes · Shared: No · Required · Purpose: App functionality (unlock) · Encrypted: Yes · Deletion: No (refund via Play)

Security: ✓ Encrypted in transit. (Do not claim independent security review.)

---

## In-app product (Monetize → Products → In-app products)

| Field | Value |
|---|---|
| Product ID | `meditation_portal_unlock`  *(must match exactly)* |
| Name | Portal Unlock — Lifetime |
| Description | Unlock the full guided-meditation library, Spirit, the meditation alarm, and remove ads — forever. |
| Default price | **$2.00 USD** |
| Status | **Active** |

> No subscriptions. This is the only purchase. Set the price to $2.00 in Play
> Console (the in-app text already reads "$2.00").

---

## Release — Production → Create new release

### Release name
```
1.3.0 — Streaks, Journeys, Breathing Coach & Sound Fixes
```

### What's new (≤500 chars)
```
• New: practice streaks, progress stats, and a home-screen widget.
• New: interactive breathing coach + 60-second Quick Calm.
• New: multi-day Journeys and "browse by goal" mood filters.
• New: daily reminders that teach a quick technique.
• Favorites, sharper deep-pink theme, and a livelier splash.
• Fixed audio playback, added back buttons, and improved text contrast throughout.
```

**What's new — German (de-DE)**
```
• Neu: Serien, Fortschritts-Statistiken und ein Startbildschirm-Widget.
• Neu: interaktiver Atemcoach + 60-Sekunden-Quick-Calm.
• Neu: mehrtägige Reisen und Stimmungs-Filter.
• Neu: tägliche Erinnerungen mit einer kurzen Technik.
• Favoriten, kräftigeres Pink-Design und ein lebendigerer Start.
• Audio-Wiedergabe repariert, Zurück-Buttons ergänzt, Textkontrast verbessert.
```

**What's new — French (fr-FR)**
```
• Nouveau : séries, statistiques de progrès et widget d'écran d'accueil.
• Nouveau : coach respiratoire interactif + Quick Calm de 60 secondes.
• Nouveau : parcours sur plusieurs jours et filtres par humeur.
• Nouveau : rappels quotidiens avec une technique rapide.
• Favoris, thème rose plus net et démarrage plus vivant.
• Lecture audio corrigée, boutons retour ajoutés, contraste du texte amélioré.
```

**What's new — Spanish (es-419)**
```
• Nuevo: rachas, estadísticas de progreso y widget de pantalla de inicio.
• Nuevo: coach de respiración interactivo + Quick Calm de 60 segundos.
• Nuevo: viajes de varios días y filtros por estado de ánimo.
• Nuevo: recordatorios diarios con una técnica rápida.
• Favoritos, tema rosa más nítido y un inicio más vivo.
• Audio reparado, botones de regreso añadidos, contraste de texto mejorado.
```

---

## Pricing & distribution
- All countries available (Play handles tax & currency).
- Allow Play to promote outside Google Search & Discover: **Yes**

## App signing
- Use **Play App Signing** (default). Local keystore = upload key; Google holds the app signing key.

---

## Localized store listings (Store presence → Main store listing → Add translation)

The app already ships German, French and Spanish strings — localized listings
convert significantly better in those markets. Paste these.

### 🇩🇪 German (de-DE)

**App name**
```
Guided Meditation Portal
```

**Short description (80 max — this is 78)**
```
Beruhige deinen Geist: geführte Meditationen, Atemcoach, Serien & Begleiter.
```

**Full description**
```
Guided Meditation Portal — ein ruhigerer Geist, Atemzug für Atemzug.

23 sanft gesprochene geführte Meditationen, ein Atemcoach, ein Begleiter namens Spirit und ein sanfter Meditationswecker — in einer lebendigen, kosmischen Oberfläche, die mit jedem Track die Farbe wechselt. Keine Abos. Nur Ruhe.

🧘 23 GEFÜHRTE MEDITATIONEN
Sitzungen für Schlaf, Stress & Angst, Fokus, Erdung und Selbstmitgefühl. Starte kostenlos mit drei vollständigen Meditationen — schalte den Rest für immer frei.

🫧 ATEMCOACH & QUICK CALM
Folge einer leuchtenden Kugel durch beruhigendes Box-Breathing. Oder tippe auf Quick Calm für einen 60-Sekunden-Reset.

🔥 SERIEN & FORTSCHRITT
Baue eine tägliche Gewohnheit auf: Serie, Sitzungen und Minuten — mit einem verzeihenden Karenztag.

🗺️ GEFÜHRTE REISEN
Mehrtägige Programme: 7 Tage zu ruhigerem Schlaf, 5 Tage weniger Angst, 7 Tage Fokus, 5 Tage Selbstmitgefühl.

🤍 SPIRIT — DEIN BEGLEITER
Ein Guide auf dem Gerät, der die richtige Übung für deine Stimmung vorschlägt: Atemarbeit, Body-Scan, Loving-Kindness und mehr.

🌙 SANFTER AUFWACHEN
Ein Meditationswecker, der dich mit einem Lieblingstrack oder einem weichen Ton weckt.

⏱ EINSCHLAF-TIMER & JUKEBOX
Timer bis 8 Stunden mit Ausblenden. Läuft bei ausgeschaltetem Bildschirm.

✨ EINMALIG FREISCHALTEN — KEINE ABOS
Alles für immer für 2,00 $. Keine monatlichen Gebühren. Mehr Meditationen und neue Sprachen sind unterwegs.

Spirit bietet allgemeine Wellness-Begleitung und ersetzt keine professionelle medizinische oder psychologische Hilfe.
```

### 🇫🇷 French (fr-FR)

**App name**
```
Guided Meditation Portal
```

**Short description (80 max — this is 79)**
```
Apaisez votre esprit : méditations guidées, respiration, séries & compagnon.
```

**Full description**
```
Guided Meditation Portal — un esprit plus calme, une respiration à la fois.

23 méditations guidées doucement narrées, un coach respiratoire, un compagnon nommé Spirit et un réveil méditation — dans une interface cosmique vivante qui change de couleur avec chaque piste. Pas d'abonnement. Juste du calme.

🧘 23 MÉDITATIONS GUIDÉES
Des séances pour le sommeil, le stress et l'anxiété, la concentration, l'ancrage et l'auto-compassion. Commencez gratuitement avec trois méditations complètes — débloquez le reste pour toujours.

🫧 COACH RESPIRATOIRE & QUICK CALM
Suivez un orbe lumineux dans une respiration carrée apaisante. Ou touchez Quick Calm pour une pause de 60 secondes.

🔥 SÉRIES & PROGRÈS
Construisez une habitude quotidienne : série, séances et minutes — avec un jour de grâce indulgent.

🗺️ PARCOURS GUIDÉS
Programmes sur plusieurs jours : 7 jours vers un sommeil plus calme, 5 jours moins anxieux, 7 jours de concentration, 5 jours d'auto-compassion.

🤍 SPIRIT — VOTRE COMPAGNON
Un guide sur l'appareil qui suggère la bonne pratique selon votre humeur : respiration, body scan, bienveillance et plus.

🌙 UN RÉVEIL PLUS DOUX
Un réveil méditation qui vous réveille avec une piste préférée ou un son doux.

⏱ MINUTEUR DE SOMMEIL & JUKEBOX
Minuteur jusqu'à 8 heures avec fondu. Fonctionne écran éteint.

✨ DÉBLOCAGE UNIQUE — SANS ABONNEMENT
Tout débloquer pour toujours pour 2,00 $. Aucun frais mensuel. Plus de méditations et de nouvelles langues arrivent.

Spirit offre un accompagnement bien-être général et ne remplace pas un avis médical ou psychologique professionnel.
```

### 🇪🇸 Spanish (es-419 / es-ES)

**App name**
```
Guided Meditation Portal
```

**Short description (80 max — this is 76)**
```
Calma tu mente: meditaciones guiadas, respiración, rachas y un compañero.
```

**Full description**
```
Guided Meditation Portal — una mente más tranquila, respiración a respiración.

23 meditaciones guiadas narradas con suavidad, un coach de respiración, un compañero llamado Spirit y una alarma de meditación — en una interfaz cósmica viva que cambia de color con cada pista. Sin suscripciones. Solo calma.

🧘 23 MEDITACIONES GUIADAS
Sesiones para dormir, estrés y ansiedad, concentración, conexión a tierra y autocompasión. Empieza gratis con tres meditaciones completas — desbloquea el resto para siempre.

🫧 COACH DE RESPIRACIÓN & QUICK CALM
Sigue un orbe brillante en una respiración cuadrada calmante. O toca Quick Calm para un reinicio de 60 segundos.

🔥 RACHAS Y PROGRESO
Crea un hábito diario: racha, sesiones y minutos — con un día de gracia indulgente.

🗺️ VIAJES GUIADOS
Programas de varios días: 7 días hacia un sueño más tranquilo, 5 días menos ansioso, 7 días de concentración, 5 días de autocompasión.

🤍 SPIRIT — TU COMPAÑERO
Un guía en el dispositivo que sugiere la práctica adecuada según cómo te sientes: respiración, escaneo corporal, bondad amorosa y más.

🌙 UN DESPERTAR MÁS SUAVE
Una alarma de meditación que te despierta con tu pista favorita o un tono suave.

⏱ TEMPORIZADOR DE SUEÑO & JUKEBOX
Temporizador de hasta 8 horas con desvanecimiento. Funciona con la pantalla apagada.

✨ DESBLOQUEO ÚNICO — SIN SUSCRIPCIONES
Desbloquea todo para siempre por $2.00. Sin cuotas mensuales. Vienen más meditaciones y nuevos idiomas.

Spirit ofrece orientación general de bienestar y no sustituye la atención médica o psicológica profesional.
```
