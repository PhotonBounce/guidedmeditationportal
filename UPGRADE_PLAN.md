
# SleepApp Modernization & Play Store Prep Plan

## 1. Architecture & Codebase Upgrades
- [ ] Refactor MainActivity to use ViewModel and LiveData/StateFlow
- [ ] Introduce Hilt for Dependency Injection
- [ ] Decouple business logic from UI (move logic to ViewModel/use-cases)
- [ ] Add error handling for service, billing, and audio focus
- [ ] Add basic unit tests for AudioEngine, PrefsManager, BillingManager

## 2. Monetization & Free Features
- [ ] Add rewarded ads to unlock a premium sound for a session
- [ ] Consider daily rotating free premium sound
- [ ] Ensure upsell dialogs are clear and compliant

## 3. User Experience & Compliance
- [ ] Add privacy policy and terms (in-app and Play Store)
- [ ] Add About/Legal/Support screen
- [ ] Add onboarding/walkthrough for first launch
- [ ] Add feedback form or contact support option
- [ ] Add settings screen for user preferences
- [ ] Add analytics (Firebase/Google Analytics)
- [ ] Add age/family policy handling if needed

## 4. Play Store Publishing Prep
- [ ] Ensure manifest and permissions are Play Store compliant
- [ ] Add/verify signing config for release builds
- [ ] Update versionCode and versionName
- [ ] Ensure ProGuard rules cover Billing, AdMob, Hilt, Analytics
- [ ] Replace AdMob test IDs with production IDs

## 5. QA & Deployment
- [ ] Add basic UI tests and emulator-based QA scripts
- [ ] Build release APK/AAB
- [ ] Run full QA in Android emulator
- [ ] Push APK to connected Galaxy 14 device (only after QA passes)
- [ ] Launch app on device and verify all flows

---

Progress will be checkpointed here after each step. See this file for current modernization status.
