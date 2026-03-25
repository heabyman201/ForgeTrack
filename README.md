# ForgeTrack
An Android application for logging workouts, tracking progress, and receiving AI-generated coaching based on training history.

---

## What It Does
ForgeTrack records workouts ,exercises, sets, reps, weight — and stores them locally. It surfaces that data back through a history view and progress charts, and passes it to an AI coaching system that generates feedback and recommendations based on what you have actually been doing. There is also a rest timer. That is roughly the scope of it.

---

## Features
- Logs workouts with exercises, sets, reps, and weight
- Stores all data locally via Room
- Rest timer with configurable duration
- Training history view with per-exercise progress tracking
- AI coaching system that reads your logged data and generates session feedback
- Progress charts for visualizing volume and load over time

---

## Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Architecture:** MVVM with ViewModels, Repository pattern, Room, SettingsManager

---

## AI Coaching
ForgeTrack includes a coaching layer that takes your recent training history and generates written feedback observations about volume, recovery, progression, or whatever the model decides is relevant given the data. It is not a rigid rule-based system. The model gets your logs and produces a response. Sometimes this is useful. Sometimes it says things you already know. It is more interesting than a static dashboard and less reliable than an actual coach.

The coaching system currently runs against a local LLM on the user's machine. No external API is called. You point it at your local endpoint and it works from there.
---

## Code Quality
Functional but uneven. Some of the earlier files are longer than they should be and naming conventions drift across the codebase. It was built iteratively, which means certain parts were written before the architecture was fully decided and were never properly revisited. A `CODEBASE_DOCUMENTATION.md` is included to make navigation less painful for anyone reading through it.

---

## Setup
1. Clone the repository
2. Open in Android Studio
3. Add your API key where the app expects it
4. Build and run on a physical device or emulator

---

## Status
Active development. The core logging and history features are stable. The coaching system works but is the part most likely to change.

---

## License
To be determined.
<img width="1080" height="2400" alt="Screenshot_20260325_093052" src="https://github.com/user-attachments/assets/13e27506-bf41-42f6-bb68-f263d020c4ef" />
reenshot_20260325_093007" src="https://github.com/user-attachments/assets/3ae7afa5-f496-4a78-a7d4-d871bab6fd33" />
<img width="1080" height="2400" alt="Screenshot_20260325_093028" src="https://github.com/user-attachments/assets/a6a0f5e7-42cf-4521-b1c4-afef3a0a1e1a" />
<img width="1080" height="2400" alt="Screenshot_20260325_093117" src="https://github.com/user-attachments/assets/6b9cf676-36a4-439b-9840-3affcd186903" />
<img width="1080" height="2400" alt="Screenshot_20260325_093140" src="https://github.com/user-attachments/assets/d1be2024-0b1c-4ed7-a6e3-792940d26c76" />
<img width="1080" height="2400" alt="Screenshot_20260325_093154" src="https://github.com/user-attachments/assets/2c21bdc2-c70f-4df7-88da-50fc6de5ab2c" />
