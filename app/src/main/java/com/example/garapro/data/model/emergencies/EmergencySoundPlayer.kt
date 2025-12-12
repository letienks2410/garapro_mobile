package com.example.garapro.data.model.emergencies

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.util.Log

object EmergencySoundPlayer {

    private var mediaPlayer: MediaPlayer? = null

    fun start(context: Context) {

        try {

            if (mediaPlayer?.isPlaying == true) return   // đã phát rồi

            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }

            Log.d("EmergencySound", "Emergency sound started")
        } catch (e: Exception) {
            Log.e("EmergencySound", "Error starting emergency sound: ${e.message}")
            stop()
        }
    }

    fun stop() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e("EmergencySound", "Error stopping emergency sound: ${e.message}")
        } finally {
            mediaPlayer = null
        }
    }
}