# AGENTS.md - MedAI Android Project

## Project Overview
Multi-module Android application using Gradle (Kotlin DSL), version catalog, and Jetpack libraries.

## Modules
- **mobile**: Main Android application (minSdk 28, targetSdk 36)
- **automotive**: Android Automotive OS module
- **shared**: Shared library module for common code

---

## Build Commands

### Gradle Wrapper
```bash
./gradlew          # Linux/macOS
gradlew.bat        # Windows
```

### Build & Run
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease       # Build release APK
./gradlew installDebug          # Build and install to connected device
```

### Testing
```bash
# All unit tests
./gradlew test

# Unit tests for specific module
./gradlew :mobile:test
./gradlew :automotive:test
./gradlew :shared:test

# Single test class
./gradlew :mobile:test --tests "com.example.medai.ExampleUnitTest"

# Single test method
./gradlew :mobile:test --tests "com.example.medai.ExampleUnitTest.addition_isCorrect"

# Instrumented tests (requires device/emulator)
./gradlew :mobile:connectedAndroidTest
./gradlew :mobile:connectedAndroidTest --tests "com.example.medai.ExampleInstrumentedTest"
```

### Linting
```bash
./gradlew lint                    # Run lint on all modules
./gradlew :mobile:lint           # Lint specific module
./gradlew :mobile:lintRelease    # Lint release build
```

### Clean & Rebuild
```bash
./gradlew clean
./gradlew clean assembleDebug
```

### Dependency Updates
```bash
./gradlew dependencyUpdates       # Check for outdated dependencies
```

---

## Code Style Guidelines

### Kotlin Style
- **Kotlin code style**: Official (`kotlin.code.style=official` in gradle.properties)
- **Line length**: Follow Android Studio default (100 characters recommended)
- **Indentation**: 4 spaces (no tabs)
- **File encoding**: UTF-8

### Naming Conventions
- **Classes/Interfaces**: PascalCase (e.g., `MainActivity`, `MusicService`)
- **Functions**: camelCase (e.g., `onCreate`, `getUserData`)
- **Properties/Variables**: camelCase (e.g., `userName`, `isEnabled`)
- **Constants**: SCREAMING_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`)
- **Package names**: lowercase with dots (e.g., `com.example.medai`)
- **Resource files**: lowercase with underscores (e.g., `activity_main.xml`, `ic_launcher.xml`)

### Imports
- Use explicit imports (no wildcard imports except for kotlin.* )
- Group imports in this order: Android, Kotlin, Java, project imports
- Sort alphabetically within each group
- Use AndroidX packages (e.g., `androidx.core.ktx`, not `android.support`)

### Types
- **Java compatibility**: JavaVersion.VERSION_11
- Prefer Kotlin types over Java equivalents (e.g., `List<T>` not `ArrayList<T>`)
- Use nullable types (`?`) when appropriate
- Avoid raw types; always provide generic type parameters

### Error Handling
- Use Kotlin's try-catch with functional style when appropriate
- Return `Result<T>` or use sealed classes for operation outcomes
- Never swallow exceptions silently; at minimum log them
- Use custom exceptions for recoverable errors

### Coroutines (if used)
- Use `viewModelScope` for ViewModel coroutines
- Use `lifecycleScope` for Activity/Fragment coroutines
- Prefer `Dispatchers.Main` for UI-related code
- Handle exceptions with `CoroutineExceptionHandler` for top-level coroutines

### Architecture
- Follow Android Architecture Components pattern (ViewModel, LiveData/StateFlow, Repository)
- Single Activity pattern preferred
- Use dependency injection (Hilt recommended for new code)
- Separate UI, business logic, and data layers

### XML Resources
- Use ConstraintLayout as default for complex layouts
- Use Material Design components
- Define colors, strings, and dimensions in resource files (no hardcoded values)
- Use `tools:` attributes for preview data

### Testing
- Unit tests: JUnit 4 with kotlin.test
- Instrumented tests: AndroidJUnit4
- Follow AAA pattern (Arrange, Act, Assert)
- Name test methods descriptively: `should_return_user_when_valid_id_provided`

### Git Conventions
- Feature branches: `feature/<ticket-number>-description`
- Bug fixes: `fix/<ticket-number>-description`
- Commit messages: Imperative mood, first line under 50 chars

---

## Configuration Files

### Version Catalog
Dependencies are managed in `gradle/libs.versions.toml`. Add new dependencies there.

### Module Structure
```
module/
├── src/
│   ├── main/
│   │   ├── java/com/example/medai/
│   │   └── res/
│   ├── test/java/          # Unit tests
│   └── androidTest/java/   # Instrumented tests
└── build.gradle.kts
```

### Key Files
- `build.gradle.kts` - Root project configuration
- `settings.gradle.kts` - Module inclusion and plugin management
- `gradle.properties` - Gradle and Kotlin settings
- `gradle/libs.versions.toml` - Dependency version catalog

---

## IDE Integration
- This project is designed for Android Studio (latest stable)
- Import as Gradle project
- Enable "Configure on demand" for faster sync
