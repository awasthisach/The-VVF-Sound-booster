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
                var foundPlaying = false
                for (controller in controllers) {
                    val state = controller.playbackState
                    if (state != null && state.state == PlaybackState.STATE_PLAYING) {
                        val pkg = controller.packageName
                        Log.d("NotificationListener", "Audio active payload playing on: $pkg")
                        foundPlaying = true
                        
                        // Try to get user friendly app name
                        val appLabel = try {
                            val pm = packageManager
                            val info = pm.getApplicationInfo(pkg, 0)
                            pm.getApplicationLabel(info).toString()
                        } catch (e: Exception) {
                            pkg
                        }
                        
                        AudioEffectEngine.getInstance().setActivePlaybackApp(appLabel)
                        break
                    }
                }
                if (!foundPlaying) {
                    AudioEffectEngine.getInstance().setActivePlaybackApp(null)
                }
            } else {
                AudioEffectEngine.getInstance().setActivePlaybackApp(null)
            }
        } catch (e: Exception) {
            Log.e("NotificationListener", "Failed checking active playbacks: ${e.message}")
            AudioEffectEngine.getInstance().setActivePlaybackApp(null)
        }
    }
}
