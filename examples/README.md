# Mail

| Module | Package | Description |
|--------|---------|-------------|
| `mail` | `com.thelightphone.mail` | minimal email client (IMAP/SMTP, plain LOGIN auth - no OAuth) |

## How to run on device

```bash
./gradlew :examples:mail:installDebug
adb shell am start -n com.thelightphone.mail/com.thelightphone.sdk.LightActivity
```
