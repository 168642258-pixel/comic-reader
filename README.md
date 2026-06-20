# 漫画阅读 (Comic Reader)

Android 本地漫画阅读器，支持 .zip/.cbz 导入，四种翻页模式。

## 功能

- 导入 .zip / .cbz 漫画文件（自动解压提取图片）
- 四种阅读模式：左右、上下、左右连续、上下连续
- 阅读进度自动保存
- 长按删除漫画
- 深色主题，沉浸阅读

## 快速构建 (GitHub Actions)

1. 推送到 GitHub 仓库
2. 进入 Actions > Build Android APK > Run workflow
3. 在 Artifacts 中下载 APK

## 本地构建 (Windows)

前置条件：JDK 17, Android SDK 34

powershell -ExecutionPolicy Bypass -File app\build-apk.ps1

## 手动构建 (任何平台)

./gradlew assembleDebug

APK: app/build/outputs/apk/debug/app-debug.apk

## 技术栈

语言: Kotlin
UI: Jetpack Compose + Material 3
图片: Coil
最低 API: 26 (Android 8.0)
