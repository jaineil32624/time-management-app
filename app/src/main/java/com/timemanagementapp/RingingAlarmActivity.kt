package com.timemanagementapp

import android.app.NotificationManager
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.IOException

class RingingAlarmActivity : ComponentActivity() {
    private var alarmPlayer: MediaPlayer? = null
    private var soundError by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action == STOP_ALARM_ACTION) {
            stopAlarm()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        startAlarmSound()
        setContent {
            MaterialTheme {
                RingingAlarmScreen(soundError = soundError, onStop = ::stopAlarm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == STOP_ALARM_ACTION) stopAlarm()
    }

    override fun onDestroy() {
        releaseAlarmPlayer()
        super.onDestroy()
    }

    private fun startAlarmSound() {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        if (soundUri == null) {
            soundError = "No alarm sound is set on this device."
            return
        }

        try {
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.setDataSource(this, soundUri)
            player.isLooping = true
            player.setOnPreparedListener { it.start() }
            player.setOnErrorListener { failedPlayer, _, _ ->
                failedPlayer.release()
                alarmPlayer = null
                soundError = "The alarm sound could not be played. Check your alarm volume and sound settings."
                true
            }
            alarmPlayer = player
            player.prepareAsync()
        } catch (error: IOException) {
            releaseAlarmPlayer()
            soundError = "The alarm sound could not be opened."
        } catch (error: IllegalArgumentException) {
            releaseAlarmPlayer()
            soundError = "The alarm sound could not be played."
        } catch (error: SecurityException) {
            releaseAlarmPlayer()
            soundError = "Android blocked access to the alarm sound."
        } catch (error: IllegalStateException) {
            releaseAlarmPlayer()
            soundError = "The alarm sound could not start."
        }
    }

    private fun stopAlarm() {
        releaseAlarmPlayer()
        getSystemService(NotificationManager::class.java).cancel(ALARM_REQUEST_CODE)
        getSharedPreferences("time_management_app", MODE_PRIVATE)
            .edit()
            .remove(ALARM_PREFERENCES_KEY)
            .apply()
        finish()
    }

    private fun releaseAlarmPlayer() {
        alarmPlayer?.release()
        alarmPlayer = null
    }
}

@Composable
private fun RingingAlarmScreen(soundError: String, onStop: () -> Unit) {
    val ink = Color(0xFF25342F)
    val accent = Color(0xFFE8895B)

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF7F5F0)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("⏰", fontSize = 64.sp)
            Spacer(Modifier.height(18.dp))
            Text("Time to get moving", color = ink, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Your alarm will keep ringing until you stop it.",
                color = ink,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            if (soundError.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(soundError, color = accent, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = ink),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Stop alarm", modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp))
            }
        }
    }
}
