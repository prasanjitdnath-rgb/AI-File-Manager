# AI File Manager

A polished Android file manager and gallery with on-device AI features, smart cleanup, and a private vault.

Built with **Kotlin + Jetpack Compose + Material 3**, Room, Coil, ML Kit, Media3, and BiometricPrompt.

## Highlights

### Files
- Storage overview with usage card and category shortcuts
- Recent files, folder browser with breadcrumbs, sorting, and hidden files
- Multi-select with copy, cut, paste, rename, delete, zip, share, and move to vault
- Built-in audio/video player with playback speed controls

### Gallery
- Date-grouped grid, folder albums, and AI-generated smart albums
- Fullscreen viewer with pinch zoom, double-tap zoom, and photo details
- Multi-select, sharing, deletion, AI labels, OCR text, and smart rename

### AI Chat
- WhatsApp-style chat screen with message bubbles, timestamps, typing state, date pills, and auto-scroll
- Chat history is stored locally (last 100 messages)
- Configure the AI display name, provider (Gemini or OpenAI-compatible), API key, model, base URL, and system prompt in **Settings**
- Access Settings from the AI Chat menu or the gear icon in Files
- Existing on-device index, smart search, and cleanup tools remain available from the AI Chat menu → **AI tools**

### AI tools
- On-device image labeling, Latin OCR, perceptual hashing, and duplicate detection
- Natural-language search such as `screenshots last week`, `bills`, `videos today`
- Cleanup suggestions for similar photos, large files, old screenshots, and APKs
- Optional cloud OCR via your own Gemini API key for handwriting and Bengali text

### Private vault
- Move files into app-private storage, hidden from Gallery and other apps
- Unlock with fingerprint, face, or device PIN
- Auto-locks when the app goes to the background

## Privacy

By default, indexing, OCR, and image analysis run entirely on your device. Cloud OCR is opt-in and only sends an image when you use that feature. Your API key and chat history are stored in app-private preferences. AI Chat only sends messages when you tap send, and only to the provider configured in Settings.

The vault is sandboxed, not encrypted at rest. It is not a substitute for encrypted storage if you need protection against physical device compromise.

## Build

Requirements:
- Android Studio Koala or newer
- JDK 17
- Android SDK 35 installed

Open the project in Android Studio and run the `app` configuration, or build from the command line:

```bash
gradle assembleDebug
```

GitHub Actions builds the debug APK on push and pull request.

minSdk 34, targetSdk 34.

## Permissions

The app requests:
- Media permissions (images, video, audio)
- All files access via `MANAGE_EXTERNAL_STORAGE` for full file management
- Biometric authentication for the private vault
- Internet for AI Chat and the optional cloud OCR feature

Google Play requires a declaration for `MANAGE_EXTERNAL_STORAGE`.

## Notes

- Storage access was rectified using [AmazeFileManager](https://github.com/TeamAmaze/AmazeFileManager) as a reference: all-files access is sufficient on its own, with media permissions as a fallback, and copy/move/rename conflicts use Amaze-style `name (n)` generation.
- Sharing now uses an explicit `ClipData` grant so more target apps can read shared files.
- Indexing runs while the app is open. For production background scanning, move `Indexer` to WorkManager.

## License

MIT
