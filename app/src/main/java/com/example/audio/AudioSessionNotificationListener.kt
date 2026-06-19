package com.example.audio

import android.content.ComponentName
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class AudioSessionNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("NotificationListener", "Vivad Sound Notification service connected successfully")
        detectActivePlaybacks()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        detectActivePlaybacks()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        detectActivePlaybacks()
    }

    private fun detectActivePlaybacks() {
        try {
            val manager = getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
            val component = ComponentName(this, AudioSessionNotificationListener::class.java)
            
            // Get active players
            val controllers = manager.getActiveSessions(component)
            if (controllers.isNotEmpty()) {
                for (controller in controllers) {
                    val state = controller.playbackState
                    if (state != null && state.state == PlaybackState.STATE_PLAYING) {
                        val pkg = controller.packageName
                        Log.d("NotificationListener", "Audio active payload playing on: $pkg")
                        // Trigger registration or legacy activation on detection
                        AudioEffectEngine.getInstance().registerSession(0, "Active Notify: $pkg")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationListener", "Failed checking active playbacks: ${e.message}")
        }
    }
}
