# Contributing to Mimikyu

First off, thank you for considering contributing to Mimikyu! Open-source community involvement is what makes this project thriving and accessible for everyone.

Please take a moment to review this document in order to make the contribution process easy and effective for everyone involved.

---

## Code of Conduct

By participating in this project, you agree to abide by our standards of respectful, collaborative, and inclusive communication.

---

## Getting Started & Development Setup

### Prerequisites
- **Android Studio Ladybug (2024.2+)** or newer.
- **JDK 17 or JDK 21** configured as your Gradle JDK.
- **Android SDK Platform 36** (compileSdk 36, targetSdk 36, minSdk 26).

### Setting Up Your Environment
1. Fork and clone the repository:
   ```bash
   git clone https://github.com/szzsuke/Mimikyu.git
   cd Mimikyu
   ```
2. Copy the sample environment files:
   ```bash
   cp .env.example .env
   cp local.properties.example local.properties
   cp app/google-services.example.json app/google-services.json
   ```
3. Open the project in Android Studio and let Gradle sync.
4. Run the debug build:
   ```bash
   ./gradlew :app:assembleDebug
   ```

---

## Coding Guidelines

- **Kotlin & Compose Standards**: Write idiomatic Kotlin. Follow official Jetpack Compose best practices (stable parameters, remembering lambdas, hoisted state).
- **Zero-Regression**: Only touch code directly relevant to your pull request. Preserve existing helper methods, architecture boundaries, and UI polish.
- **No Secrets in Code**: Never commit API keys, tokens, or personal configuration files (`google-services.json`, `local.properties`, keystores).
- **Testing**: Run `./gradlew test` and `./gradlew :app:assembleDebug` to verify that all modules compile and tests pass before opening a PR.

---

## Pull Request Process

1. Create a descriptive feature branch:
   ```bash
   git checkout -b feature/amazing-feature
   ```
2. Commit your changes with clear, semantic commit messages:
   ```bash
   git commit -m "feat(player): add smooth waveform seek animation"
   ```
3. Push to your fork:
   ```bash
   git push origin feature/amazing-feature
   ```
4. Open a Pull Request using the provided PR template.
5. Provide screenshots or screen recordings for any UI/UX modifications.

---

## Reporting Issues

If you find a bug, please check existing issues first. If it has not been reported, open a new issue using our **Bug Report** template with detailed reproduction steps, device model, and Android OS version.
