# WorkoutTracker Codebase Documentation

## 1. What This Project Is

`WorkoutTracker` is an Android fitness application built with:

- Kotlin
- Jetpack Compose
- Room
- Jetpack Navigation
- Health Connect
- Firebase / Firestore / Firebase Auth
- Wear OS data layer integration
- On-device / local-network AI advice utilities

At a high level, the app does five things:

1. Lets the user configure and run workouts.
2. Stores workout history locally and can merge that with cloud data.
3. Imports wellness and workout metadata from Health Connect.
4. Generates adaptive analysis such as muscle readiness, fatigue, and advice.
5. Syncs selected workout state to companion surfaces such as a watch, notifications, and widgets.

The codebase is mostly organized around a central runtime spine:

- `MyApplication` builds long-lived app-level dependencies.
- `MainActivity` owns home/navigation and most read-only exploration screens.
- `WorkoutActivity` owns the live workout session flow.
- `WorkoutRepo` + `WorkoutViewmodels` own persistence and workout state.
- Feature packages such as `muscle`, `health`, `ai`, and `badges` layer specialized behavior on top.

## 2. Current Package Layout

After the first-pass refactor, the important code is grouped like this:

### Root package: `com.forgecompose.workouttracker`

These are still the app entry points, glue screens, and Android components that are safest to keep in the root for now.

- `MyApplication.kt`
- `MainActivity.kt`
- `WorkoutActivity.kt`
- `WorkoutForegroundService.kt`
- `MemoryMonitorForegroundService.kt`
- `NotificationDispatcherActivity.kt`
- `WorkoutWidget.kt`
- `ListenerService.kt`
- `DataLayerListenerService.kt`
- `ProfileScreen.kt`
- `SettingsScreen.kt`
- `Taskbar.kt`
- `GoalSelectionScreen.kt`
- `RestScreen.kt`
- `WorkoutSelectionScreen.kt`
- `WorkoutDetailScreen.kt`
- `AnalyticsActivity.kt`
- `RepPredictorScreen.kt`
- `AppearanceOptionsAppTheme.kt`
- `PerformanceScreen.kt`
- `FirebaseGoogleAuth.kt`
- `FirestoreMapper.kt`
- `HeartbeatCompose.kt`
- `VectorNormaliser.kt`
- `1RPM_ESTIMATOR.kt`

### Feature / support packages

- `ai`
  - AI generation, memory, persona settings, local model settings, rep prediction support.
- `analytics`
  - Charts, progression graphs, summary screens, recent-weight views.
- `badges`
  - Badge definitions, progress storage, unlock logic, badge UI.
- `health`
  - Health Connect manager, health screens, sensor-facing helpers.
- `muscle`
  - Muscle readiness, recovery, heatmap, adaptive fatigue/target logic.
- `profile`
  - User preferences, onboarding, survey collection, profile-side user data capture.
- `ui.components`
  - Shared composables and visual utilities.
- `ui.theme`
  - Compose theme layer.
- `workout`
  - Workout entities, Room database, repositories, sync helpers, presets, history screen.

## 3. Application Startup Flow

### 3.1 `MyApplication.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/MyApplication.kt`

This is the app composition root.

Responsibilities:

- Creates the Room database through `AppDatabase.getDatabase(...)`.
- Creates the main `WorkoutRepo` implementation.
- Initializes AI preference state through `PersonaPrefs`.
- Initializes secure AI storage through `SecureGeminiStore`.
- Initializes the personal data tape engine through `PDE`.
- Initializes blur/performance helpers through `blurAnim` and `PerformanceOptionsManager`.
- Forces night mode.

Why it exists:

- Android components such as activities and services need a stable place to fetch shared dependencies.
- It prevents repeated database/repository setup inside each screen.

Main connection points:

- `MainActivity` reads `workoutRepository` from `application as MyApplication`.
- `WorkoutActivity` does the same.
- `DataLayerListenerService` also uses `MyApplication` to save synced workouts.

### 3.2 `AndroidManifest.xml`

File: `app/src/main/AndroidManifest.xml`

This declares:

- Main launcher: `MainActivity`
- Live workout activity: `WorkoutActivity`
- Notification dispatcher: `NotificationDispatcherActivity`
- Analytics screen host: `ExerciseAnalyticsActivity`
- Foreground services:
  - `WorkoutForegroundService`
  - `MemoryMonitorForegroundService`
- Wear listener services:
  - `HrDataListenerService`
  - `DataLayerListenerService`
- Widget receiver:
  - `WorkoutWidget`
- Health Connect permissions and metadata

Why it matters:

- This file defines the app's externally visible surface.
- It explains why certain files were intentionally left in the root package during the package split.

## 4. Main App Flow

### 4.1 `MainActivity.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/MainActivity.kt`

This is the main non-session shell of the app.

Responsibilities:

- Chooses the initial route:
  - `onboarding` on first launch
  - `HomeScreen` otherwise
- Creates three major view models:
  - `MainScreenViewModel`
  - `WorkoutListViewModel`
  - `BadgeViewModel`
- Requests notification permission.
- Hosts the main `NavHost`.
- Feeds shared workout data into profile, history, analytics, muscle status, badges, settings, and utility screens.

Important route mapping:

- `HomeScreen`
- `onboarding`
- `Survey`
- `RepPredictor`
- `WorkoutSelector`
- `WorkoutHistory`
- `DetailedWorkout`
- `UserProfile`
- `PersonaSettings`
- `EditUserStats`
- `MuscleGroup`
- `RepMax`
- `WeeklySummary`
- `PerformanceOptions`
- `MemoryMonitor`
- `AppearanceScreen`
- `Settings`
- `HealthConnect`
- `badges`

Why it is designed this way:

- The main app experience is mostly read-oriented and multi-feature.
- Keeping those routes under a single activity makes shared navigation, theming, and animation easier.
- The actual workout session is complex enough that it gets its own separate activity.

### 4.2 `Taskbar.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/Taskbar.kt`

This is the floating bottom taskbar used by the main shell.

Responsibilities:

- Shows navigation affordances.
- Reads theme settings from `AppearanceOptionsManagerAppTheme`.
- Reads motion settings from `PerformanceOptionsManager`.
- Adjusts presentation when a workout is active.

Why it exists:

- The app uses a highly custom navigation surface rather than default Material bottom navigation.
- This gives more control over animation, appearance, and workout-aware behavior.

### 4.3 `ProfileScreen.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/ProfileScreen.kt`

Responsibilities:

- Displays profile data and personal records.
- Reads user stats from `UserPreferencesManager`.
- Reads workout history from `WorkoutListViewModel`.
- Displays badges from `BadgeViewModel`.
- Integrates Firebase auth state.
- Prompts the user for recommendation surveys on a timed cadence.

Why it exists:

- This screen is the user-facing summary hub for identity, progress, and customization.
- It bridges persistent profile preferences with computed workout history.

## 5. Live Workout Session Flow

### 5.1 `WorkoutActivity.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/WorkoutActivity.kt`

This is the second major runtime shell, dedicated to an active workout.

Core role:

- Owns the flow from choosing goals to executing the workout to resting and completing the session.

Key navigation inside this activity:

- `GoalScreen`
- `WorkoutScreen`
- `RestScreen`

Important internal concepts:

- `ConnectedWorkout`
  - Global in-memory workout session state.
  - Stores fields like current reps, sets, time, weight, mode, and goal targets.
- `WorkoutLog`
  - Session-side helper object for accumulated set data.
- `AI_HEART_ADAPT`
  - Workout-time heart/adaptation related helper.

Major responsibilities:

- Starts and stops `WorkoutForegroundService` based on current workout mode.
- Hosts goal selection UI.
- Hosts live workout execution UI.
- Hosts rest-screen logic and transitions.
- Calculates completion and session metrics.
- Saves/restores session snapshots so the notification layer can reopen the right destination.

Why it is separated from `MainActivity`:

- A live workout has different lifecycle needs from the browsing experience.
- It must remain responsive while the app backgrounds/foregrounds.
- It coordinates foreground notifications and companion sync.

### 5.2 `GoalSelectionScreen.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/GoalSelectionScreen.kt`

Responsibilities:

- Configures a workout target:
  - sets
  - reps
  - time
  - distance
  - weight
- Writes directly into `ConnectedWorkout`.
- Launches the live workout route when setup is complete.

Why it exists:

- Goal definition is the boundary between selecting a workout and executing it.
- This file acts as the transition point from passive planning into active tracking.

### 5.3 `RestScreen.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/RestScreen.kt`

Responsibilities:

- Displays rest-time UI and advice.
- Reads `ConnectedWorkout` rest-related state.
- Helps bridge the user from one set to the next.

Why it matters:

- Rest is treated as a first-class mode, not just a timer overlay.
- This allows different UI, advice, and notification behavior during recovery.

### 5.4 `WorkoutForegroundService.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/WorkoutForegroundService.kt`

Responsibilities:

- Runs while the workout is active or resting.
- Maintains a persistent notification.
- Adapts update interval based on screen state and power-save state.
- Provides actions to reopen/stop workout-related surfaces.

Why it exists:

- Active workout tracking should survive app backgrounding.
- Android requires a foreground service for reliable long-running session UX.

### 5.5 `NotificationDispatcherActivity.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/NotificationDispatcherActivity.kt`

Responsibilities:

- Reads the saved workout snapshot.
- Detects whether an active workout is still in progress.
- Redirects the notification tap either to:
  - `WorkoutActivity`
  - or `MainActivity`

Why it exists:

- Notifications should reopen the correct experience without duplicating task stack logic everywhere.

## 6. Workout Data Layer

### 6.1 `workout/WorkoutViewmodels.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/workout/WorkoutViewmodels.kt`

This is one of the most important files in the whole codebase.

It contains:

- Core entities:
  - `Workout`
  - `Exercise`
  - `WorkoutExercise`
  - `ExerciseSet`
- Enums and type converters:
  - `WorkoutStatus`
  - `MuscleGroup`
  - `ExerciseType`
- `RoomDatabase` definition:
  - `AppDatabase`
- DAOs:
  - `WorkoutDao`
  - `ExerciseDao`
  - `WorkoutExerciseDao`
  - `ExerciseSetDao`
- UI state:
  - `WorkoutListUiState`
- Main app workout view model:
  - `WorkoutListViewModel`
- PR helper:
  - `checkPrForExercise(...)`

Why so much is here:

- This file originally grew into the central workout model layer before the package split.
- It still acts as the authoritative definition of workout persistence and view-model behavior.

Important flows inside this file:

#### A. Loading workouts

`WorkoutListViewModel.loadWorkouts()`:

- subscribes to `workoutRepository.getAllWorkouts()`
- maps updates into `WorkoutListUiState.Success`
- drives almost every major read-only screen

This means many screens do not query Room directly. They react to one shared flow.

#### B. Logging workouts

`WorkoutListViewModel.LogWorkout(...)`:

- builds a `Workout`
- writes it through the repository
- includes richer fields such as:
  - HR averages
  - session RPE
  - fatigue
  - systemic drain
  - environment

This matters because later modules, especially `muscle/MuscleStatus.kt`, depend on those richer fields for recovery analysis.

#### C. Health Connect import

`WorkoutListViewModel.syncHealthConnectWorkouts(...)`:

- checks Health Connect availability and permissions
- reads exercise sessions, HR, and calories
- infers:
  - session title
  - RPE
  - fatigue
  - systemic drain
- de-duplicates imported records
- writes imported sessions as local `Workout` records

Why this is important:

- Health Connect data is normalized into the app's local workout format.
- That keeps downstream analysis simple because the app can treat local and imported workouts similarly.

### 6.2 `workout/WorkoutRepo.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/workout/WorkoutRepo.kt`

Responsibilities:

- Defines the repository interfaces for workouts and exercises.
- Implements `WorkoutRepositoryImpl`.
- Implements `ExerciseRepositoryImpl`.

Why it exists:

- It isolates higher-level code from raw DAO calls.
- It centralizes cascade assumptions for nested workout/exercise/set deletion.

### 6.3 `workout/MergedWorkoutRepo.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/workout/MergedWorkoutRepo.kt`

Responsibilities:

- Wraps a local `WorkoutRepo`.
- Listens to Firestore workouts.
- Maps remote documents to local-like `Workout` objects.
- Merges local and remote streams.
- Prefers local records when signatures match.

Why it exists:

- The UI wants one combined workout stream.
- It avoids forcing every screen to reason about local-vs-remote separately.

### 6.4 `FirestoreMapper.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/FirestoreMapper.kt`

Responsibilities:

- Converts a Firestore `DocumentSnapshot` into the app's `Workout` model.

Why it exists:

- Cloud data does not naturally match Room entities.
- This keeps mapping logic out of `MergedWorkoutRepo`.

### 6.5 `workout/WorkoutHistory.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/workout/WorkoutHistory.kt`

Responsibilities:

- Displays saved workout history.
- Supports export options through `ExportDateRange` and export dialog UI.

Why it exists:

- History is the user-facing read model for the repository layer.

### 6.6 `WorkoutSelectionScreen.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/WorkoutSelectionScreen.kt`

Responsibilities:

- Presents preset workouts and selection UI.
- Uses usage/favorite tracking helpers.
- Is the main entry point into `WorkoutActivity`.

Why it exists:

- It separates workout discovery from workout execution.

### 6.7 `WorkoutDetailScreen.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/WorkoutDetailScreen.kt`

Responsibilities:

- Shows details for a single completed workout.
- Provides utility functions like streak and time formatting.
- Connects to analytics views and deeper examination flows.

Why it exists:

- Users need a drill-down view from history and home summaries into one specific workout.

## 7. Health and Wellness Layer

### 7.1 `health/HealthConnect.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/health/HealthConnect.kt`

Responsibilities:

- Wraps `HealthConnectClient`.
- Declares required permissions.
- Exposes read helpers for:
  - heart rate
  - sleep
  - resting HR
  - body fat
  - nutrition
  - calories
  - oxygen saturation
  - exercise sessions
- Exposes write support for workout sessions.

Why it exists:

- This is the app's boundary adapter to Health Connect.
- Higher-level code can stay focused on business logic rather than raw record requests.

### 7.2 `HealthConnectScreen.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/health/HealthConnectScreen.kt`

Responsibilities:

- Gives the user a UI for availability, permissions, and Health Connect setup.

Why it exists:

- Health integrations require user trust and explicit permission flow.

### 7.3 How Health Connect feeds the rest of the app

There are two major paths:

1. Import path
   - `WorkoutListViewModel.syncHealthConnectWorkouts(...)`
   - imports sessions into the local workout history
2. Analysis path
   - `MuscleStatusSection(...)` reads sleep, calories, SpO2, body fat, nutrition, and HR directly through `HealthConnectManager`
   - uses those to compute recovery and muscle readiness

Why there are two paths:

- One path preserves workouts as durable history.
- The other path computes current readiness from wellness metrics.

## 8. Muscle Analysis and Recovery Layer

### 8.1 `muscle/MuscleStatus.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/muscle/MuscleStatus.kt`

This is the main physiological analysis engine in the app.

Major responsibilities:

- Converts profile preferences into a `UserProfile`.
- Parses survey answers into `SurveyInsights`.
- Reads recent `WorkoutSummary` history.
- Calculates:
  - recovery efficacy
  - weekly target per muscle
  - acute and chronic load
  - fatigue
  - injury risk
  - readiness bands
  - detail insights
- Renders the muscle status UI and heatmap.

Important models:

- `SurveyInsights`
- `SprintGoal`
- `UserProfile`
- `RecoveryFactors`
- `WorkoutSummary`
- `MuscleLoad`
- `Mission`
- `MuscleInsight`

Important compute stages:

1. Build user profile from `UserPreferencesManager`.
2. Parse survey tape from `SurveyTape`.
3. Read wellness data from `HealthConnectManager`.
4. Convert workout history into per-muscle stimulus/load/frequency.
5. Compute recovery efficacy.
6. Compute weekly targets.
7. Compute fatigue and injury risk.
8. Classify each muscle into a `LoadBand`.
9. Render charts, tiles, and body heatmap.

### 8.2 Recent adaptation addition

The new adaptation layer inside `MuscleStatus.kt` now:

- builds a `MuscleHistoryProfile`
- derives a `MuscleAdaptation`
- raises weekly targets modestly for well-adapted muscles
- reduces fatigue accumulation for adapted muscles
- speeds fatigue decay for adapted muscles

Why this matters:

- The app no longer treats every muscle as equally recoverable forever.
- It now reflects repeated exposure, growth, and adaptation from real usage history.

### 8.3 `muscle/MuscleStatusHost.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/muscle/MuscleStatusHost.kt`

Responsibilities:

- Hosts the muscle status route inside the main navigation graph.
- Adapts workout history from `Workout` entities into `WorkoutSummary`.
- Bridges home/profile-level view model data into the muscle engine.

Why it exists:

- `MuscleStatus.kt` is intentionally computation-heavy.
- This host isolates the route wiring and model conversion from the engine itself.

### 8.4 `muscle/MuscleDiagram.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/muscle/MuscleDiagram.kt`

Responsibilities:

- Defines body drawing geometry and muscle diagram support visuals.

Why it exists:

- The analysis engine needs a visual target to present readiness and strain intuitively.

## 9. AI and Adaptive Guidance Layer

### 9.1 `ai/GeminiUtlitity.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/ai/GeminiUtlitity.kt`

This file is the largest AI integration point.

Responsibilities:

- Defines AI-facing state snapshots such as:
  - `GeminiSignalSnapshot`
  - `WorkoutTrendSnapshot`
- Stores prompt/advice memory in `GeminiAdaptiveMemoryStore`.
- Hosts `GeminiUtilityViewModel`.
- Exposes `useGeminiAdviceGenerator(...)` for Compose screens.
- Supports model readiness for:
  - edge/on-device model
  - local-network model

Why it exists:

- The app wants reusable AI advice from many screens without reimplementing prompt, caching, and provider selection each time.

How it connects:

- `WorkoutActivity` uses it for workout-time advice.
- `MuscleStatus.kt` records muscle signal snapshots into `GeminiAdaptiveMemoryStore`.
- Persona settings and secure storage support its provider choices.

### 9.2 `ai/PersonaSetting.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/ai/PersonaSetting.kt`

Responsibilities:

- Stores AI provider choice.
- Stores local LLM connection settings.
- Manages edge-model preferences.

Why it exists:

- The project supports multiple AI execution modes, so provider configuration needs a dedicated home.

### 9.3 `ai/SecureGeminiStore.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/ai/SecureGeminiStore.kt`

Responsibilities:

- Persists AI-related secrets/config in encrypted shared preferences.

### 9.4 `ai/Personal_Data_Engine.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/ai/Personal_Data_Engine.kt`

Responsibilities:

- Maintains a tape/stream of user-personalized data for summary and AI-oriented readback.

### 9.5 `ai/LexAdviceFastr.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/ai/LexAdviceFastr.kt`

Responsibilities:

- Provides a fast rule/template-based advice path.

Why it exists:

- The app can still produce lightweight guidance even when heavier model paths are unavailable.

### 9.6 `RepPredictor.kt` and `RepPredictorScreen.kt`

Files:

- `app/src/main/java/com/forgecompose/workouttracker/ai/RepPredictor.kt`
- `app/src/main/java/com/forgecompose/workouttracker/RepPredictorScreen.kt`

Responsibilities:

- Estimate rep potential.
- Present a UI tool for rep prediction.

Why it exists:

- This is a narrower analysis feature separated from the full advice system.

## 10. User Profile, Onboarding, and Survey Layer

### 10.1 `profile/UserPreferenceManager.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/profile/UserPreferenceManager.kt`

Responsibilities:

- Stores user profile stats securely:
  - name
  - age
  - height
  - weight
  - experience
  - preferred style
  - important muscles
- Stores Health Connect mapping preferences.

Why it exists:

- Many systems depend on stable user baseline data:
  - profile UI
  - goal calculation
  - muscle adaptation
  - Health Connect import normalization

### 10.2 `profile/OnboardingScreen.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/profile/OnboardingScreen.kt`

Responsibilities:

- First-run intro flow.
- Collects the first profile values.
- Hands them back to `MainActivity`, which persists them through `UserPreferenceManager`.

### 10.3 `profile/SurveryType.kt` and `profile/SurveyScreen.kt`

Files:

- `app/src/main/java/com/forgecompose/workouttracker/profile/SurveryType.kt`
- `app/src/main/java/com/forgecompose/workouttracker/profile/SurveyScreen.kt`

Responsibilities:

- Survey tape storage and recommendation survey UI.

Why it matters:

- The muscle engine reads this survey data to personalize recovery and target calculations.

### 10.4 `EditUserStats.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/profile/EditUserStats.kt`

Responsibilities:

- Lets the user update profile values after onboarding.

## 11. Badge / Progress Reward Layer

### 11.1 `badges/BadgeManager.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/badges/BadgeManager.kt`

Responsibilities:

- Applies badge progression when workouts are logged.
- Stores badge progress through a `BadgeStorage` abstraction.
- Supports persistent and in-memory storage modes.

Why it exists:

- Reward logic should not be embedded directly into workout UI code.

### 11.2 Other badge files

- `BadgeDataclass.kt`
  - badge domain models
- `BadgeCatelog.kt`
  - badge definitions
- `BadgeViewmodel.kt`
  - screen-facing state and events
- `BadgeScreen.kt`
  - badge browser UI
- `BadgeSection.kt`
  - badge summaries/embeds
- `BadgeUnlockAnimation.kt`
  - unlock presentation

How badges connect to the app:

- `MainActivity` instantiates `BadgeViewModel`.
- Home/profile screens read badge state.
- Workout completion can advance badge progress.

## 12. Analytics and Visualization Layer

### 12.1 `analytics` package

Important files:

- `WeeklySummary.kt`
- `ProgressionGraphs.kt`
- `RecentWeightsCard.kt`
- `GraphBar.kt`
- `GenericLineChart.kt`
- `LineChartComposables.kt`

Responsibilities:

- Transform workout history into chart-friendly and summary-friendly views.
- Present progression over time.
- Provide reusable chart building blocks.

Why it exists:

- The app emphasizes trend visibility, not just raw logs.

### 12.2 `AnalyticsActivity.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/AnalyticsActivity.kt`

Responsibilities:

- Hosts deeper exercise analytics in a dedicated activity.

Why it exists:

- Some analytics workflows deserve a focused screen outside the main navigation stack.

### 12.3 `1RPM_ESTIMATOR.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/1RPM_ESTIMATOR.kt`

Responsibilities:

- One-rep max estimation utility and screen.

Why it exists:

- It is a standalone calculation feature based on PR and workout data.

## 13. Appearance and Runtime Performance

### 13.1 `AppearanceOptionsAppTheme.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/AppearanceOptionsAppTheme.kt`

Responsibilities:

- Defines app color themes.
- Stores the selected appearance profile.
- Exposes appearance flows consumed by many screens.

Why it exists:

- The codebase uses a custom visual identity rather than plain Material defaults.

### 13.2 `PerformanceScreen.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/PerformanceScreen.kt`

Responsibilities:

- Stores runtime visual-performance preferences such as:
  - blur
  - taskbar animation
  - moving gradients/particles
  - navigation effects
  - body heatmap visibility
  - max suggestions
- Computes effective runtime settings based on foreground/screen state.

Why it exists:

- The UI is visually rich, so the project needs explicit runtime tuning.

### 13.3 `ui.components`

Important files:

- `blurAnim.kt`
- `BackgroundAnimations.kt`
- `CircularTimerBar.kt`
- `ReusableComponents.kt`
- `QuickStartButton.kt`
- `RunningStickFigure.kt`

Responsibilities:

- Shared visual and interaction primitives reused across multiple screens.

## 14. Wear OS and Companion Sync

### 14.1 `workout/WorkoutConfigSync.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/workout/WorkoutConfigSync.kt`

Responsibilities:

- Normalizes workout config.
- Writes latest config to Firestore when possible.
- Caches the latest payload locally.
- Sends the payload to connected watch nodes.

Why it exists:

- Workout setup must be transferable to the watch.
- Local cache allows replay if the watch reconnects later.

### 14.2 `ListenerService.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/ListenerService.kt`

Responsibilities:

- Listens for heart-rate data changes from Wear OS.
- Emits values into `HrUpdateBus`.

Why it exists:

- Live HR is one of the most useful workout-time companion signals.

### 14.3 `DataLayerListenerService.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/DataLayerListenerService.kt`

Responsibilities:

- Handles incoming watch messages.
- Responds to `/workout_config_request`.
- Re-sends cached or in-memory workout config.
- Persists completed workouts received from the watch.

Why it exists:

- The phone is the durable source of truth, but the watch can drive or complete sessions remotely.

## 15. Notifications, Widgets, and System Surfaces

### 15.1 `WorkoutWidget.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/WorkoutWidget.kt`

Responsibilities:

- Launches `MainActivity` with a route hint that opens workout selection.

Why it exists:

- It provides fast entry into the workout flow from the home screen.

### 15.2 `MemoryMonitorForegroundService.kt`

File: `app/src/main/java/com/forgecompose/workouttracker/MemoryMonitorForegroundService.kt`

Responsibilities:

- Captures app memory snapshots.
- Stores monitor history.
- Exposes monitoring state for the performance/debug screen.

Why it exists:

- This project is visually heavy and session-heavy, so memory diagnostics are a first-class concern.

## 16. End-to-End Data Flows

### 16.1 First launch flow

1. `MainActivity` checks `OnboardingManager.isFirstTimeLaunch()`.
2. User lands on `OnboardingScreen`.
3. On completion, `MainActivity` saves data through `UserPreferenceManager`.
4. App navigates to `HomeScreen`.

### 16.2 Manual workout logging flow

1. User enters `WorkoutSelectionScreen`.
2. User starts `WorkoutActivity`.
3. `GoalSelectionScreen` writes goals into `ConnectedWorkout`.
4. `WorkoutScreen` tracks live progress.
5. `RestScreen` handles between-set recovery.
6. Session completion calls into workout logging logic.
7. `WorkoutListViewModel` writes the `Workout` through `WorkoutRepo`.
8. All workout-history consumers update automatically from the shared flow.

### 16.3 Health Connect import flow

1. User grants permissions through the health screen.
2. `WorkoutListViewModel.syncHealthConnectWorkouts(...)` reads sessions.
3. Imported sessions are normalized to the app's `Workout` model.
4. Imported records enter the same local history stream.
5. Analytics, profile, and muscle features can use them with no special branch logic.

### 16.4 Muscle readiness flow

1. `ProfileMuscleStatusRoute` converts `Workout` history into `WorkoutSummary`.
2. `MuscleStatusSection` reads:
   - profile preferences
   - survey tape
   - wellness signals from Health Connect
   - recent workout history
3. `deriveMuscleLoads(...)` computes load, fatigue, targets, adaptation, and risk.
4. The UI renders readiness cards and heatmaps.
5. Signal snapshots are also recorded for AI memory reuse.

### 16.5 Badge unlock flow

1. A workout is logged.
2. Badge logic evaluates total workouts and trained muscles.
3. `BadgeStorage` persists progress and pending unlocks.
4. `BadgeViewModel` surfaces unlock events.
5. Main/profile screens can render unlock animations and badge summaries.

### 16.6 Watch sync flow

1. Workout config is sent via `WorkoutConfigSync`.
2. Watch can request config using `/workout_config_request`.
3. Phone replays cached or in-memory config.
4. Watch can send `/workout_state`.
5. If state is completed, `DataLayerListenerService` persists a `Workout`.

## 17. Why The Code Connects The Way It Does

The structure is driven by the app's runtime realities:

- `MainActivity` is optimized for broad navigation and read-heavy features.
- `WorkoutActivity` is optimized for focused live session tracking.
- `WorkoutViewmodels.kt` centralizes the workout model because everything depends on workout history.
- `WorkoutRepo` and `MergedWorkoutRepo` hide local/cloud differences from screens.
- `HealthConnectManager` acts as an integration boundary so analysis code does not deal with raw record APIs.
- `MuscleStatus.kt` is isolated because it is the most algorithmically dense feature in the app.
- `GeminiUtlitity.kt` is isolated because AI provider logic, memory, and prompt orchestration are cross-cutting concerns.
- `UserPreferenceManager` and survey files feed personalization into multiple systems without duplicating storage logic.
- Services and listeners are kept close to Android component boundaries because they are lifecycle-sensitive.

In short:

- the `workout` package is the truth source,
- the `profile` package personalizes that truth,
- the `health` package enriches it,
- the `muscle` and `ai` packages interpret it,
- the `analytics` and root UI screens present it,
- and Android services/widgets/watch sync make it available outside the main screen.

## 18. Best Starting Points For Future Work

If you are new to the codebase, start in this order:

1. `MyApplication.kt`
2. `MainActivity.kt`
3. `workout/WorkoutViewmodels.kt`
4. `workout/WorkoutRepo.kt`
5. `WorkoutActivity.kt`
6. `health/HealthConnect.kt`
7. `muscle/MuscleStatus.kt`
8. `ai/GeminiUtlitity.kt`

That sequence gives the clearest understanding of:

- app startup
- navigation
- persistence
- live session behavior
- wellness integration
- adaptive analysis
- AI support

## 19. Notes For Future Refactoring

The current package split is a good first pass, but there is still room to improve:

- The root package still contains many feature screens that could later move into subpackages.
- `WorkoutViewmodels.kt` is still doing the work of several files.
- `WorkoutActivity.kt` is large enough to benefit from feature extraction.
- `MainActivity.kt` still acts as a broad navigation orchestrator and could eventually be decomposed into route modules.

Those are not bugs. They are the natural next steps after the current stabilization pass.

## 20. LLM Coaching System

Package: `com.forgecompose.workouttracker.coaching`

This is a true coaching layer that turns the existing muscle-status signals into a
custom weekly training plan using an LLM. The user prompts it in natural language
and the request is sent to whichever model the user selected: the on-device
MediaPipe model, a local-network OpenAI-compatible server, or the Google AI Studio
(Gemini) API. If no model is configured/reachable, a deterministic signal-driven
planner produces a usable week so the feature never fails.

### Files

- `CoachingModels.kt`
  - Serializable domain models: `WeeklyPlan`, `PlanDay`, `PlanExercise`,
    `CoachingGoal`, `MuscleSignal`, `RecoverySnapshot`, `PlanRequest`, and the
    `PlanGenState` UI state.
- `CoachingStore.kt`
  - JSON persistence (SharedPreferences) for the current plan, a rolling plan
    history, and the goal list.
- `CoachingEngine.kt`
  - The brain. Builds the system + user prompt from the muscle signals and the
    user's request, routes to the selected provider (reusing
    `PersonaPrefs.readModelProvider()`, `SecureGeminiStore`/`BuildConfig` key,
    `LocalLlmConfig`, and `EdgeModelManager`), parses the JSON response, and
    provides `buildHeuristicPlan(...)` — a deterministic fallback that prescribes
    custom exercises, sets/reps/RPE/rest and rest-day placement for every
    training-day combination based on each muscle's readiness band, injury risk,
    and weekly volume target.
- `CoachingViewModel.kt`
  - Activity-scoped `AndroidViewModel` holding provider selection, computed
    signals, the current plan, generation lifecycle, and goal CRUD.
- `CoachingScreen.kt`
  - The `Coaching` route. Computes live muscle signals (same data sources as the
    Muscle Status screen via `deriveMuscleLoadsStepwise`), shows the engine
    selector, a prompt box with quick prompts, the live muscle-status strip, and
    the generated week as expandable day cards.
- `CoachingGoalsScreen.kt`
  - The `CoachingGoals` route. Weekly-volume progress vs targets, plus goal
    creation, progress tracking, and status management.

### Wiring

- Routes `Coaching` and `CoachingGoals` are registered in `MainActivity`'s NavHost.
- The Muscle Status screen (`MuscleStatusHost.kt`) has an "AI Coach" entry card
  that opens the coaching route, since coaching consumes the same signals.
