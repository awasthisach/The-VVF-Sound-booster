package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.audiofx.AudioEffect
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.example.MainActivity

class AudioService : Service() {

    private val CHANNEL_ID = "vivad_sound_enhancer_channel"
    private val NOTIFICATION_ID = 8871
    private var sessionReceiver: AudioSessionReceiver? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("AudioService", "Service onCreate")
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }

        // 1. Register receiver for broadcast audio sessions (legacy/explicit triggers)
        sessionReceiver = AudioSessionReceiver()
        val filter = IntentFilter().apply {
            addAction(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION)
            addAction(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION)
        }
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            sessionReceiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_EXPORTED
        )

        // Ensure legacy mix (Session 0) is running by default
        AudioEffectEngine.getInstance().setLegacyMode(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AudioService", "Service started command")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AudioService", "Service onDestroy")
        try {
            sessionReceiver?.let { unregisterReceiver(it) }
        } catch (e: Exception) {
            Log.e("AudioService", "Unregister receiver fail: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "VVF Sound Equalizer Service"
            val descriptionText = "Equalizer background active status"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("VVF Sound Equalizer")
            .setContentText("System-wide audio optimization is active. (सक्रिय है)")
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
