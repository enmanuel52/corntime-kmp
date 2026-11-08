# 🍿 Corntime KMP

A modern, production-grade **Kotlin Multiplatform (KMP)** movie discovery and management application targeting **Android** and **iOS**. Corntime demonstrates clean architecture, multi-module separation, and shared business/presentation logic combined with native UI implementations (Jetpack Compose on Android and SwiftUI on iOS via SKIE).

---

## 🏗️ Architecture & Modularization

The project follows **Clean Architecture** principles and is heavily modularized for scalability, testability, and fast incremental builds:

```
├── :androidApp          # Android application entry point (Jetpack Compose)
├── :shared              # KMP umbrella framework exported to iOS
├── :core
│   ├── :domain          # Shared domain models, use cases, and repository interfaces
│   ├── :data            # Repositories, Ktor API clients, DTOs, and mappers
│   ├── :datastore       # Preferences and local key-value storage
│   ├── :design-system   # Design tokens, themes, and shared UI components
│   ├── :presentation    # Base ViewModels, UI state management, and common extensions
│   └── :testing         # Shared test fakes, mocks, and test utilities
└── :feature
    └── :movies
        ├── :domain      # Movie feature domain models and business rules
        └── :presentation # Movie feature ViewModels and state flows
```

---

## 🛠️ Tech Stack

- **Kotlin Multiplatform (KMP)** - Cross-platform business logic sharing.
- **UI Frameworks**:
  - **Android**: Jetpack Compose, Material 3, ConstraintLayout Compose.
  - **iOS**: SwiftUI native application (`iosApp`) powered by **SKIE** for seamless Swift-Kotlin interop (async/await coroutines, sealed class to enum mapping).
- **Navigation**: Jetpack Navigation 3 (`androidx.navigation3`).
- **Dependency Injection**: Koin (`io.insert-koin`).
- **Networking**: Ktor Client (`io.ktor`) with Kotlinx Serialization.
- **Local Storage**: AndroidX DataStore (`androidx.datastore`).
- **Concurrency**: Kotlin Coroutines & Flow (`kotlinx.coroutines`).

---

## 📱 Cross-Platform UI Showcase

Because Corntime provides native-feeling user experiences tailored to each platform while sharing core business logic, the UI implementations leverage platform primitives:

| Feature | Android (Jetpack Compose / Material 3) | iOS (SwiftUI / Native Design) |
| :--- | :--- | :--- |
| **Home / Discover** | Material 3 Cards, Shimmer loading, Horizontal/Vertical Carousels | SwiftUI Lists, Grids, and Custom Navigation Transitions |
| **Movie Details** | Immersive Backdrop, Backdrop Blur (Haze), Dynamic Header | SwiftUI ScrollTransition, Floating Action & Rating Views |
| **Search & Navigation** | Real-time query filtering with Navigation 3 state | Native SwiftUI Searchable, TabView with NavigationStack |

*(References design patterns from [corntime-react-native](https://github.com/enmanuel52/corntime-react-native), [corntime-ios](https://github.com/enmanuel52/corntime-ios), and [Pop-Corns](https://github.com/enmanuel52/Pop-Corns))*

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio** (Koala or newer) with Kotlin Multiplatform plugin.
- **Xcode** 16+ (for iOS development).
- **JDK 17**.

### Running the Android App
1. Open the project in Android Studio or terminal.
2. Run the build/run command via Gradle or Android Studio:
   ```bash
   ./gradlew :androidApp:assembleDebug
   ```

### Running the iOS App
1. Ensure the shared Kotlin framework is linked:
   ```bash
   ./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
   ```
2. Open `iosApp/iosApp.xcodeproj` in Xcode:
   ```bash
   open iosApp/iosApp.xcodeproj
   ```
3. Select your target simulator (e.g., iPhone 16) and hit **Run** (`Cmd + R`).

---

## 🧪 Running Tests

- **Android Host Tests**:
  ```bash
  ./gradlew testAndroidHostTest
  ```
- **iOS Simulator Tests**:
  ```bash
  ./gradlew :shared:iosSimulatorArm64Test
  ```
