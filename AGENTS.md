# HostelFixApp — Project & Testing Guide

Welcome to the **HostelFixApp** repository documentation.

## Project Structure
- `:app` — Main Android Application module containing Jetpack Compose UI, ViewModels, SQLite Local DB, Firebase Cloud Firestore integration, and Navigation.

## Documentation Links
- **[Testing Strategy & Execution Guide](docs/testing.md)** — Complete guide covering Unit Tests (35 tests), Integration Tests (SQLite DB), Compose UI Tests, Test Fakes, and Gradle execution commands.

## Quick Commands
- **Run Unit Tests**: `.\gradlew testDebugUnitTest`
- **Run Device Integration & UI Tests**: `.\gradlew connectedCheck`
- **Build App**: `.\gradlew assembleDebug`
