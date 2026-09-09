package com.thelightphone.sdk

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Posts a simple local notification - e.g. from a background [LightJob] that just found
 * something worth telling the person about. There's no deep link back into the tool; tapping it
 * just brings the app's existing task to the foreground (whatever screen was already showing),
 * same as tapping its launcher icon - this is the minimal "something happened" primitive, not a
 * full notification framework.
 *
 * Creates [channelId] (if missing) with [channelName] as its user-visible label. Requires
 * `android.permission.POST_NOTIFICATIONS` (Android 13+) to actually be granted - if it isn't,
 * this silently does nothing rather than throwing, since a job shouldn't fail just because
 * the person hasn't granted that permission yet.
 */
fun SealedLightContext.postNotification(
    channelId: String,
    channelName: String,
    notificationId: Int,
    title: String,
    text: String,
) {
    val context = androidContext.applicationContext
    val manager = NotificationManagerCompat.from(context)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(
            NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
    if (!hasPermission) return

    // setAutoCancel only clears the notification on tap if there's a registered content
    // intent to tap - without one, some launchers still open the app (via their own
    // fallback), but the notification itself is left behind in the shade.
    val contentIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ?.let { PendingIntent.getActivity(context, notificationId, it, PendingIntent.FLAG_IMMUTABLE) }

    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle(title)
        .setContentText(text)
        .setSmallIcon(android.R.drawable.ic_dialog_email)
        .setAutoCancel(true)
        .setContentIntent(contentIntent)
        .build()

    manager.notify(notificationId, notification)
}
