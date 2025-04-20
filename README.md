# 📽️ NovaBox

**NovaBox** is a feature-rich Android streaming application designed to deliver movies, live TV, and downloadable content with a sleek, user-friendly interface. Built for entertainment enthusiasts, it offers seamless playback, personalized favorites, and robust offline capabilities—all wrapped in a modern design.

---

## ✨ Features

- **Movies & Live TV**: Stream movies and live channels with high-quality playback.
- **Offline Downloads**: Save content for offline viewing with a dedicated download manager.
- **Favorites & Search**: Bookmark your go-to titles and search effortlessly.
- **Account Management**: Secure login, registration, and profile settings.
- **Custom Playback**: Enhanced video player with VLC-based streaming (via `PlayerActivity`).
- **Rewards & Redeem**: Engage users with rewards and redemption options.
- **Live Updates**: Keep the app fresh with seamless update checks.
- **Responsive UI**: Dark/light themes, custom fonts, and smooth navigation.

---

## 📸 Screenshots

Get a peek at NovaBox in action:

| Splash Screen | Movies Browser | Video Player | Login |
|---------------|----------------|--------------|-------|
| ![Splash Screen](screenshots/splash.png) | ![Movies Browser](screenshots/movies.png) | ![Video Player](screenshots/player.png) | ![Login](screenshots/login.png) |

---

## 🛠️ Tech Stack

- **Language**: Java/Kotlin (assumed from typical Android setup)
- **UI Framework**: Android Jetpack (Fragments, Navigation)
- **Media Playback**: VLC-based `PlayerActivity` for robust streaming
- **Networking**: Custom `NetworkMonitor` for connectivity handling
- **Data Parsing**: `M3UParser` for live channel playlists
- **Build System**: Gradle with version catalogs (`libs.versions.toml`)
- **Dependencies**:
  - Likely ExoPlayer (based on `exo_playback_control_view.xml`)
  - AndroidX libraries
  - Custom utilities (`DialogUtils`, `UpdateManager`)

---

## 📂 Project Structure

app/
├── AndroidManifest.xml
├── src/main/
│   ├── java/com/example/novaflix/
│   │   ├── AccountFragment.java        # User profile management
│   │   ├── AuthActivity.java           # Login/register entry point
│   │   ├── Channel.java               # Live channel model
│   │   ├── ChannelAdapter.java        # RecyclerView adapter for channels
│   │   ├── DialogUtils.java           # UI dialog helpers
│   │   ├── DownloadFragment.java      # Offline content manager
│   │   ├── DownloadItem.java          # Download item model
│   │   ├── DownloadListAdapter.java   # Download list UI
│   │   ├── FavouriteFragment.java     # Bookmarked content
│   │   ├── LiveFragment.java          # Live TV streaming
│   │   ├── LoadingDialogFragment.java # Progress dialogs
│   │   ├── LoginFragment.java         # Login UI
│   │   ├── M3UParser.java             # M3U playlist parser
│   │   ├── MainActivity.java          # Core app hub
│   │   ├── Movies.java                # Movie model
│   │   ├── MoviesAdapter.java         # Movie list UI
│   │   ├── MoviesFragment.java        # Movie browsing
│   │   ├── MoviesPlayerActivity.java  # Movie playback
│   │   ├── NetworkMonitor.java        # Connectivity checks
│   │   ├── NovaFlixApplication.java   # App lifecycle
│   │   ├── PlayerActivity.java        # VLC-based streaming
│   │   ├── RedeemFragment.java        # Reward redemption
│   │   ├── RegisterFragment.java      # Sign-up UI
│   │   ├── RestartFragment.java       # App restart logic
│   │   ├── RewardFragment.java        # User rewards
│   │   ├── SearchFragment.java        # Content search
│   │   ├── SettingsFragment.java      # App preferences
│   │   ├── SplashActivity.java        # Launch screen
│   │   ├── UpdateFire.java            # Update triggers
│   │   ├── UpdateManager.java         # OTA updates
│   │   ├── VideoPlayerActivity.java   # Alternate player
│   ├── res/
│   │   ├── drawable/                  # App icons, graphics
│   │   ├── font/                      # Custom typography
│   │   ├── layout/                    # UI layouts (e.g., activity_main.xml, fragment_movies.xml)
│   │   ├── menu/                      # Navigation menus
│   │   ├── mipmap-*/                  # Launcher icons
│   │   ├── navigation/                # Jetpack Navigation graphs
│   │   ├── values/                    # Strings, colors, themes
│   │   ├── values-night/              # Dark mode styles
│   │   ├── xml/                       # Config files
├── build.gradle                       # App-level build config
├── proguard-rules.pro                 # Code shrinking rules
├── gradle.properties                  # Gradle settings
├── gradle-wrapper.properties          # Gradle wrapper config
├── libs.versions.toml                # Dependency versioning
├── local.properties                   # Local SDK paths
├── settings.gradle                   # Project setup
text

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Latest stable version (e.g., Koala | 2024.1.1)
- **JDK**: 17 or higher
- **Gradle**: 8.x (configured in `gradle-wrapper.properties`)
- **PayPal SDK**: For payment features (optional, see `PlayerActivity`)
- **VLC SDK**: For streaming (bundled or external)

### Installation
1. **Clone the Repo**:
   ```bash
   git clone https://github.com/Jun-Tsu/NovaBox.git
   cd NovaBox

    Open in Android Studio:
        Import the project via File > Open.
        Let Gradle sync dependencies (check build.gradle and libs.versions.toml).
    Configure PayPal (Optional):
        If using PlayerActivity for payment streams:
            Grab your Client ID from developer.paypal.com.
            Update PlayerActivity.java or relevant config.
    Run the App:
        Connect an Android device or emulator (API 21+).
        Hit Run > Run 'app' in Android Studio.

🖥️ Usage

    Splash Screen: SplashActivity kicks things off with a branded intro.
    Auth Flow: AuthActivity handles login (LoginFragment) and registration (RegisterFragment).
    Main Hub: MainActivity serves movies (MoviesFragment), live TV (LiveFragment), downloads (DownloadFragment), and more.
    Streaming: PlayerActivity (VLC-powered) or MoviesPlayerActivity for playback.
    Offline: Save content via DownloadFragment, view in DownloadListAdapter.
    Extras: Rewards (RewardFragment), redemption (RedeemFragment), and settings (SettingsFragment).

🔧 Configuration

    Themes: Customize via res/values/themes.xml and values-night/.
    Icons: Update launcher icons in mipmap-* folders.
    API Keys: Add PayPal or streaming service keys in local.properties or a secure config.
    M3U Playlists: Feed live channels to M3UParser for LiveFragment.

🤝 Contributing

Want to make NovaBox even doper? Here’s how:

    Fork the repo.
    Create a feature branch (git checkout -b feature/awesome-thing).
    Commit your changes (git commit -m "Add awesome thing").
    Push to your fork (git push origin feature/awesome-thing).
    Open a Pull Request—explain what’s dope about it.

🐛 Issues

Got bugs or ideas? Drop them in the Issues tab. Be clear—screenshots, logs, and vibes help.
📜 License

This project is licensed under the MIT License—see  for details.
🎉 Acknowledgements

    VLC Team: For the streaming backbone in PlayerActivity.
    Android Jetpack: Making navigation and fragments a breeze.
    You: For vibing with NovaBox and making entertainment epic.

Built with 💪 by Jun-Tsu—streaming the future, one flick at a time.
