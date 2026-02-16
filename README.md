# Java Swing Habit Tracker

This is a desktop habit tracker built with Java Swing.

## What was fixed for desktop usage
- Added persistent storage, so your data is saved to a local file and restored when you reopen the app.
- Added explicit **Save Data** and **Load Data** buttons.
- Added auto-save on checkbox changes and when closing the window.
- Kept the monthly tracker design with date rows and habit checkbox columns.

## Features
- Track any number of habits for each day of a selected month.
- Navigate month-to-month.
- End-of-month analysis:
  - Overall completion percentage
  - Perfect day count
  - Best full-completion streak
  - Per-habit completion percentages

## Run as desktop app
```bash
javac HabitCalendarApp.java
java HabitCalendarApp
```

## Optional: build a runnable JAR
```bash
javac HabitCalendarApp.java
jar cfe HabitTracker.jar HabitCalendarApp *.class
java -jar HabitTracker.jar
```

## Data file location
The app stores data in:
- `~/.habit-tracker-data.ser` (Linux/macOS)
- `%USERPROFILE%\\.habit-tracker-data.ser` (Windows)
