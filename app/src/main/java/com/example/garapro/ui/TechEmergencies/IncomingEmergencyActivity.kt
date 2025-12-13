package com.example.garapro.ui.TechEmergencies

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.garapro.MainActivity
import com.example.garapro.R
import com.example.garapro.data.model.emergencies.EmergencySoundPlayer

class IncomingEmergencyActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "IncomingEmergencyActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setupFullScreenFlags()

        setContentView(R.layout.activity_incoming_emergency)

        val title = intent.getStringExtra("title") ?: "Emergency"
        val body = intent.getStringExtra("body") ?: "You has an emergency"
        val screen = intent.getStringExtra("screen") ?: "ReportsFragment"

        Log.d(TAG, "onCreate: title=$title, body=$body, screen=$screen")

        val tvTitle = findViewById<TextView>(R.id.tvEmergencyTitle)
        val tvBody = findViewById<TextView>(R.id.tvEmergencyBody)
        val btnOk = findViewById<Button>(R.id.btnEmergencyOk)

        tvTitle.text = title
        tvBody.text = body

        btnOk.setOnClickListener {

            EmergencySoundPlayer.stop()


            val mainIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP

                putExtra("from_notification", true)
                putExtra("screen", screen) // ReportsFragment
                putExtra("notificationType", "Emergency")
            }

            startActivity(mainIntent)
            finish()
        }
    }

    private fun setupFullScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
    }

    @Suppress("MissingSuperCall")
    override fun onBackPressed() {

    }
}
