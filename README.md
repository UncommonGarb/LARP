# LARP (Language AI RolePlay)

An immersive Android application designed for local and remote AI roleplaying. This project focuses on providing a high degree of character depth, consistent personality modeling, and a clean, distraction-free chat interface.

## Core Features

- **Character Depth**: Beyond basic descriptions, the app supports detailed personality modeling including speech patterns, flaws, and specific likes/dislikes.
- **Strict Roleplay Logic**: The system prompt architecture is designed to eliminate AI "assistant" behavior, ensuring characters stay in character.
- **Lorebook Integration**: Manage world information, NPCs, and locations that are dynamically injected into the AI's context as needed.
- **Memory Bank**: Automatically or manually extract facts from conversations to maintain long-term consistency.
- **Backend Flexibility**: Connects to various backends including KoboldCPP and Ollama, supporting both local and remote LLM hosting.

## Getting Started

### Prerequisites

- An active AI backend (KoboldCPP, Ollama, etc.) accessible via IP or hostname.
- **For building from source**: Android Studio Jellyfish or newer.

### Installation

#### Option 1: Direct Install (APK)
Download and install the latest APK from the [Releases](https://github.com/UncommonGarb/LARP/releases) section (if available). Ensure you have "Install from Unknown Sources" enabled on your device.

#### Option 2: Build from Source
1. Clone this repository.
2. Open the `android_app` project in Android Studio.
3. Build and run the application on your device or emulator.

## Configuration

Once the app is running, navigate to **Settings** to:
- Set up your **User Persona**.
- Add a **Backend Connection** (base URL and API type).
- Customize **Global Generation Settings** (Temperature, Top-P, etc.).

## Feedback & Contributions

Have a feature request, found a bug, or simply don't like something about the app? Please open an issue and let us know why! Honest feedback is essential for making LARP better for everyone.

## Status: Active Development

> [!WARNING]
> This application is in continuous development. Users should expect occasional crashes, UI regressions, and significant changes to functionality as the project evolves.

Features and database schemas may change as the application moves toward a stable release.
