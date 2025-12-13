package com.example.garapro.data.model.emergencies

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.util.Log

object EmergencySoundPlayer {

    private var ringtone: Ringtone? = null
    private var isPlaying = false

    fun start(context: Context) {
        try {
            if (isPlaying) return


            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            ringtone = RingtoneManager.getRingtone(context, uri).apply {
                audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }

            ringtone?.play()
            isPlaying = true

            Log.d("EmergencySound", "Ringtone emergency started")
        } catch (e: Exception) {
            Log.e("EmergencySound", "Error starting ringtone: ${e.message}")
            stop()
        }
    }

    fun stop() {
        try {
            ringtone?.stop()
        } catch (e: Exception) {
            Log.e("EmergencySound", "Error stopping ringtone: ${e.message}")
        } finally {
            ringtone = null
            isPlaying = false
        }
    }
}

