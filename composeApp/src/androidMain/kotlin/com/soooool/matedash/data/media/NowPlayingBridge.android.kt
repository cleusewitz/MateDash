package com.soooool.matedash.data.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.provider.Settings
import com.soooool.matedash.MateDashNotificationListener

private var appContext: Context? = null

fun initNowPlaying(context: Context) {
    appContext = context.applicationContext
}

fun isNotificationListenerEnabled(): Boolean {
    val ctx = appContext ?: return false
    val listeners = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
    val expected = ComponentName(ctx, MateDashNotificationListener::class.java).flattenToString()
    return listeners.split(":").any { it == expected }
}

fun openNotificationListenerSettings() {
    val ctx = appContext ?: return
    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ctx.startActivity(intent)
}

actual fun getNowPlaying(): NowPlayingInfo? {
    val ctx = appContext ?: return null
    if (!isNotificationListenerEnabled()) return null
    val msm = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return null
    val component = ComponentName(ctx, MateDashNotificationListener::class.java)
    val controllers = try {
        msm.getActiveSessions(component)
    } catch (e: SecurityException) {
        return null
    }
    if (controllers.isEmpty()) return null

    val controller = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
        ?: controllers.first()

    val md = controller.metadata ?: return null
    val title = md.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() } ?: return null
    val artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
        ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        ?: ""
    val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
    return NowPlayingInfo(title = title, artist = artist, isPlaying = isPlaying)
}
