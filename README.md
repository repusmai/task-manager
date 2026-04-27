# 📋 Personal Task Manager

A personal daily task manager for Android with calendar, reminders, recurring tasks, and auto-update from GitHub.

## Features
- ✅ Task creation with title, description, priority, due date/time
- 📅 Monthly calendar view with task indicators
- 🔔 Reminders via AlarmManager notifications
- 🔁 Recurring tasks (daily / weekly / monthly)
- 🏷️ Categories with color coding
- 🔄 Auto-update: checks GitHub on launch and installs new APKs directly

## Setup

### 1. Configure your GitHub repo in the update checker

Open `app/src/main/java/com/personal/taskmanager/update/UpdateChecker.kt` and replace:
```kotlin
private const val GITHUB_REPO = "YOUR_USERNAME/YOUR_REPO"
```
with your actual GitHub username and repository name, e.g.:
```kotlin
private const val GITHUB_REPO = "johndoe/my-task-manager"
```

### 2. Open in Android Studio
- Open Android Studio → File → Open → select the `TaskManager` folder
- Wait for Gradle sync to complete

### 3. Run on your device
- Enable USB Debugging on your phone (Settings → About → tap Build Number 7x → Developer Options → USB Debugging)
- Connect via USB
- Press the ▶ Run button in Android Studio

### 4. Push to GitHub for auto-updates
```bash
git init
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO.git
git add .
git commit -m "Initial commit"
git push origin main
```

GitHub Actions will automatically build the APK and publish it as the `latest` release.
Every time you push to `main`, a new APK is built and released.
The app checks for updates on launch and prompts you to install if a newer version is available.

## Project Structure
```
app/src/main/java/com/personal/taskmanager/
├── data/
│   ├── db/          # Room database, DAOs, type converters
│   ├── model/       # Task, Category data classes
│   └── repository/  # Single source of truth
├── di/              # Hilt dependency injection
├── notifications/   # Alarm scheduling, notification receivers
├── ui/
│   ├── tasks/       # Task list screen + ViewModel
│   ├── calendar/    # Calendar screen
│   └── settings/    # Settings + update checker UI
├── update/          # GitHub update checker + APK installer
└── workers/         # WorkManager background jobs
```

## Updating the App
1. Make your code changes
2. Bump `versionCode` and `versionName` in `app/build.gradle.kts`
3. Commit and push to `main`
4. GitHub Actions builds and releases the APK automatically
5. Open the app → it detects the new version → tap "Install Update"
