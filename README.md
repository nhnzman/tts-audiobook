# Android TTS Audio Book App

This project contains the complete source code for an Android application that converts long pieces of text into audio using whatever text‑to‑speech (TTS) engines are installed on the user's device.  It automatically splits large texts into manageable chunks (up to 20 000 characters each), based on the last hyphen (`-`) within that range, generates audio files for each chunk and names them sequentially (for example `민법01-1.mp3`, `민법01-2.mp3`, etc.).  It also organises audio and text files by subject, provides a built‑in audio player with variable playback speeds, and allows the user to select from any TTS voices available on their phone.

## Features

* **Text input and file import** – users can paste text directly into the app or select one or more `.txt` files from storage.
* **Subject folders** – imported files and generated audio are stored under subject folders (e.g. `민법`, `상법`), which can be chosen or created on the fly.
* **Smart splitting** – texts are split into chunks up to 20 000 characters.  When the limit is reached the app looks backwards for the most recent hyphen (`-`) and splits there; if no hyphen is found in a reasonable distance, it splits at the limit.
* **Voice selection** – the app queries the system for all installed TTS engines and voices.  Users can pick any of these voices for conversion and playback.
* **Audio generation and playback** – each chunk is synthesised to a temporary WAV file using `android.speech.tts.TextToSpeech` and then converted to MP3 on device.  A built‑in player (based on ExoPlayer) supports seeking, play/pause and discrete speed changes (1.0×, 1.1×, …, 2.0×).
* **Resume support** – if the app is closed during long conversions, it keeps track of progress and resumes when reopened.

## Repository structure

```
app_src/
├── README.md                 – this file
└── app/                      – Android application module
    ├── build.gradle          – module build configuration
    ├── proguard-rules.pro    – ProGuard/R8 rules (default empty)
    └── src/main
        ├── AndroidManifest.xml
        ├── java/com/example/ttsaudiobook
        │   ├── MainActivity.kt
        │   ├── FileUtils.kt
        │   ├── Splitter.kt
        │   ├── AudioGenerator.kt
        │   └── AudioPlayerActivity.kt
        └── res
            ├── layout
            │   ├── activity_main.xml
            │   └── activity_audio_player.xml
            └── values
                ├── strings.xml
                └── colors.xml
```

## Building the APK

1. Install **[Android Studio](https://developer.android.com/studio)** on your development machine.
2. In Android Studio choose **“Open”** and select the `app_src` directory as the project root.
3. Let Gradle download any dependencies (internet connection required on first run).
4. Connect an Android device with developer mode enabled or start an emulator.
5. Press **Run (▶)** to build and install the APK.

If you prefer the command line, you can build a release APK with:

```bash
./gradlew assembleRelease
```

The resulting file will be located at `app/build/outputs/apk/release/app-release.apk`.

## Customisation

* **Changing the maximum chunk size** – in `Splitter.kt` the `MAX_LEN` constant defines the maximum characters per chunk.  Adjust this if 20 000 characters is too small or too large.
* **Adjusting the hyphen splitting rule** – the `findSplitPoint` method in `Splitter.kt` controls how the last hyphen within the 20 000 character window is located.  You can modify this logic to handle other custom delimiters.
* **Adding playback speeds** – see `AudioPlayerActivity.kt` where an array of speeds is defined (`playbackSpeeds`).  You can change or extend these values.
* **UI layout** – the XML files under `res/layout` control how screens look.  Feel free to adjust paddings, colours or add additional elements.  Colours are defined in `res/values/colors.xml` and strings in `res/values/strings.xml`.

## Usage

1. **Launch the app** and pick either **Paste Text** or **Import Text File**.
2. **Choose or create a subject**; this will create a folder on your device under `/AudioBooks/<subject>` for all related files.
3. When pasting, enter your text and add hyphens (`-`) to indicate natural break points.  The app will honour these when creating chunks as long as it doesn’t exceed 20 000 characters.
4. Select a voice from the available TTS engines when prompted.
5. Wait while the text is split and synthesised into audio.  Progress is displayed for each chunk.
6. After conversion, go to **My Audio Books** to see your saved titles.  Tap a title to view its chunks and begin playback.

## Disclaimer

This project demonstrates how to use Android’s built‑in TTS and ExoPlayer for educational purposes.  It is provided as a starting point; further refinement (error handling, background tasks, UI polishing) may be required for production use.  There are no external network requests in this app; all TTS synthesis happens locally on the device using installed engines.
