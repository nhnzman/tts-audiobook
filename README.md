# Rao TTS

Rao TTS는 긴 텍스트를 사용자의 기기에 설치된 TTS 엔진으로 음성 파일로 변환해주는 오프라인 안드로이드 앱입니다. 이 저장소에는 완전한 안드로이드 스튜디오 프로젝트가 포함되어 있으며, 앱의 모든 핵심 기능과 빌드 자동화 스크립트를 제공합니다.

## 주요 특징

* **텍스트 입력 및 불러오기** – 사용자가 직접 텍스트를 입력하거나 하나 이상의 `.txt` 파일을 선택해서 불러올 수 있습니다. 파일은 과목별 폴더로 정리됩니다.
* **스마트 분할** – 2만자를 넘는 긴 텍스트는 마지막 하이픈(`-`) 위치를 기준으로 자동 분할됩니다. 하이픈이 없는 경우에는 적당한 지점에서 분할합니다.
* **TTS 변환** – Android `TextToSpeech` 엔진을 사용하여 각 분할된 텍스트를 오디오 파일로 변환합니다. 기기에 설치된 모든 음색을 자동으로 검색하여 선택할 수 있습니다.
* **배속 조절** – 재생 속도를 1.0배부터 3.0배까지 0.1배 단위로 조절할 수 있습니다. 상하 버튼을 눌러 쉽게 변경할 수 있습니다.
* **오디오 저장** – 변환된 파일은 MP3 형식으로 저장되고, 사용자 선택 저장소(예: `/storage/emulated/0/RaoTTS`) 및 과목별 폴더에 정리됩니다.
* **미니 플레이어 및 전체 화면 플레이어** – 하단에 고정된 미니 플레이어와 전체 화면 재생 화면을 제공합니다. 백그라운드 재생 및 Foreground Service 알림을 지원합니다.
* **GitHub Actions 자동 빌드** – 저장소에는 APK를 자동으로 빌드하는 워크플로가 포함되어 있어, 푸시나 PR마다 서명된 릴리스 APK를 생성하고 아티팩트로 업로드합니다.

## 프로젝트 구조

```
RaoTTS/
├── app/                     # Android 애플리케이션 모듈
│   ├── build.gradle         # 모듈 레벨 Gradle 설정
│   ├── proguard-rules.pro    
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/com/lao/tts/
│           │   ├── MainActivity.kt
│           │   ├── FileUtils.kt
│           │   ├── Splitter.kt
│           │   ├── AudioGenerator.kt
│           │   ├── AudioService.kt
│           │   └── AudioPlayerActivity.kt
│           └── res/
│               ├── layout/
│               │   ├── activity_main.xml
│               │   ├── activity_audio_player.xml
│               │   └── include_mini_player.xml
│               ├── mipmap-anydpi-v26/
│               │   └── ic_launcher.xml
│               ├── mipmap-hdpi/
│               │   └── ic_launcher.png
│               ├── mipmap-mdpi/
│               │   └── ic_launcher.png
│               ├── mipmap-xhdpi/
│               │   └── ic_launcher.png
│               ├── mipmap-xxhdpi/
│               │   └── ic_launcher.png
│               ├── mipmap-xxxhdpi/
│               │   └── ic_launcher.png
│               └── values/
│                   ├── colors.xml
│                   ├── strings.xml
│                   └── themes.xml
├── build.gradle             # 프로젝트 레벨 Gradle 설정
├── gradle.properties        
├── settings.gradle
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.properties
│       └── gradle-wrapper.jar
├── gradlew                  # 유닉스용 Gradle 실행 스크립트
├── gradlew.bat              # Windows용 Gradle 실행 스크립트
└── .github/workflows/
    └── android_build.yml   # GitHub Actions 워크플로
```

## 빌드 방법

1. JDK 17 이상과 Android SDK 프로젝트 레벨 dependencies가 설치된 환경에서 아래 명령을 실행하여 릴리스 APK를 빌드할 수 있습니다.

   ```bash
   ./gradlew assembleRelease
   ```

2. GitHub Actions를 사용하는 경우, 본 프로젝트를 push 하면 `app/build/outputs/apk/release/app-release.apk` 파일이 자동으로 생성되어 아티팩트로 업로드됩니다.

## TTS 엔진 및 MP3 변환

안드로이드의 `TextToSpeech` API는 안드로이드 OS에 내장된 음성 합성 엔진을 사용합니다. MP3 변환을 위해 내부적으로 PCM WAV를 생성한 뒤 MediaCodec을 통해 MP3로 변환합니다. 필요에 따라 외부 라이브러리를 사용하여 변환 로직을 개선할 수 있습니다.

## 라이선스

이 프로젝트는 교육 목적으로 제공되며 별도의 라이선스가 명시되지 않은 한 MIT License 하에 배포됩니다.