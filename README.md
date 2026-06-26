# Android-Kitty-AI

Kitty-AI 的 Android 客户端，用于请求服务器生成睡前故事，并播放服务器返回的 mp3 音频。

## 后端

当前后端地址：`http://47.95.111.58:8000/`

所有故事生成和 mp3 生成都在服务器端完成，APK 不包含 API Key，不在本地调用 LLM 或生成 TTS。

## 功能

- 输入故事要求
- 请求后端 `/api/bedtime`
- 显示故事标题和正文
- 播放服务器返回的 audio_url
- 暂停 / 继续 / 停止 / 重新播放
- 友好错误提示（网络、服务器、超时等）

## 构建

```bash
./gradlew assembleDebug
```

APK 输出路径：

```
app/build/outputs/apk/debug/app-debug.apk
```

## GitHub Actions 下载 APK

仓库页面 → **Actions** → **Android Debug APK Build** → 最新运行 → **Artifacts** → `KittyAI-debug-apk`

## 服务器地址修改

编辑 `app/src/main/java/com/kittyai/pet/bedtime/ApiConfig.kt`：

```kotlin
const val BASE_URL = "http://47.95.111.58:8000/"
```

## 注意事项

- APK 不包含 DeepSeek API Key
- APK 不在本地调用 LLM
- APK 不在本地生成 TTS
- 所有故事生成和 mp3 生成都在服务器完成
- 当前使用 HTTP，AndroidManifest 需要 `usesCleartextTraffic=true`（已配置）
