# ProGuard rules for Rao TTS

# Keep all classes in our package so that reflection-based APIs (e.g. TextToSpeech)
# work correctly.  In a larger project you would narrow these rules.
-keep class com.lao.tts.** { *; }