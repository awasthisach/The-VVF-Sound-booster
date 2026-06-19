package com.example.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.util.Log

class AudioSessionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        val sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
        val packageName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME) ?: "Unknown Player"

        if (sessionId == -1) return

        when (action) {
            AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                Log.d("AudioSessionReceiver", "Intercepted session opened: $sessionId for package: $packageName")
                AudioEffectEngine.getInstance().registerSession(sessionId, packageName)
            }
            AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                Log.d("AudioSessionReceiver", "Intercepted session closed: $sessionId for package: $packageName")
                AudioEffectEngine.getInstance().unregisterSession(sessionId)
            }
        }
    }
}
