package com.timemanagementapp

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.OpenableColumns
import android.content.pm.PackageManager
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

private val PageColor = Color(0xFFF7F5F0)
private val InkColor = Color(0xFF25342F)
private val MutedColor = Color(0xFF7B8580)
private val AccentColor = Color(0xFFE8895B)
private val CardColor = Color.White
private val SoftGreen = Color(0xFFE8EEE8)
private const val BUILT_IN_MUSIC_URI = "builtin:soft_focus"
private const val SOMAFM_FOCUS_STREAM = "https://ice2.somafm.com/dronezone-128-mp3"
private const val SOMAFM_TASK_STREAM = "https://ice2.somafm.com/groovesalad-128-mp3"
internal const val ALARM_REQUEST_CODE = 42
internal const val ALARM_PREFERENCES_KEY = "next_alarm_at"
internal const val ALARM_CHANNEL_ID = "time_management_loud_alarm"
internal const val STOP_ALARM_ACTION = "com.timemanagementapp.STOP_ALARM"
private const val QUICK_GAME_SECONDS = 5 * 60

private fun alarmPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, ReminderReceiver::class.java)
    return PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST_CODE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

@Composable
private fun MusicCardAdditionalControls(
    musicName: String?,
    isPlaying: Boolean,
    isLoading: Boolean,
    errorMessage: String,
    onChooseFocusRadio: () -> Unit,
    onChooseTaskRadio: () -> Unit,
    onOpenSpotify: () -> Unit,
    onOpenYouTube: () -> Unit,
    onToggleMusic: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
OutlinedButton(
    onClick = onChooseFocusRadio,
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.weight(1f)
) {
    Text("Study radio", color = InkColor)
}
OutlinedButton(
    onClick = onChooseTaskRadio,
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.weight(1f)
) {
    Text("Chores radio", color = InkColor)
}
        }
        Button(
onClick = onToggleMusic,
enabled = musicName != null && !isLoading,
colors = ButtonDefaults.buttonColors(containerColor = InkColor),
shape = RoundedCornerShape(12.dp),
modifier = Modifier.fillMaxWidth()
        ) {
Text(if (isLoading) "Loading…" else if (isPlaying) "Pause music" else "Play music")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
OutlinedButton(
    onClick = onOpenSpotify,
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.weight(1f)
) {
    Text("Spotify", color = InkColor)
}
OutlinedButton(
    onClick = onOpenYouTube,
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier.weight(1f)
) {
    Text("YouTube Music", color = InkColor)
}
        }
        Text(
"SomaFM radio uses internet data; stream music remains copyright of its artists and labels. Spotify and YouTube searches open their app or website.",
color = MutedColor,
fontSize = 11.sp
        )
        if (errorMessage.isNotEmpty()) {
Text(errorMessage, color = AccentColor, fontSize = 12.sp)
        }
    }
}

private data class ActivityType(
    val name: String,
    val emoji: String,
    val defaultMinutes: Int
)

private val StarterActivities = listOf(
    // Half an hour is a solid starting point for studying, and it can be changed any time.
    ActivityType("Study", "📚", 30),
    ActivityType("Work", "💻", 45),
    ActivityType("Job search", "📋", 30),
    ActivityType("Walk", "🚶", 20),
    ActivityType("Run", "🏃", 30),
    ActivityType("Exercise", "💪", 30),
    ActivityType("Cooking", "🍳", 20),
    ActivityType("Housework", "🧹", 25),
    ActivityType("Reading", "📖", 25),
    ActivityType("Hobby", "🎨", 30),
    ActivityType("Gaming", "🎮", 60),
    ActivityType("Break", "☕", 5)
)

private data class PlanTask(
    val name: String,
    val isDone: Boolean = false,
    val timeGoalMinutes: Int = 30,
    val createdAt: Long = System.currentTimeMillis(),
    val rewardClaimed: Boolean = false
)

private fun musicSearchFor(activity: String): String {
    return when (activity.lowercase(Locale.getDefault())) {
        "study", "reading" -> "lofi study music"
        "work", "job search" -> "focus work music"
        "walk", "run", "exercise" -> "upbeat workout music"
        "cooking", "housework" -> "upbeat cleaning and cooking music"
        "gaming" -> "video game music playlist"
        "break" -> "relaxing chill music"
        else -> "$activity music playlist"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "Loud alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Rings for alarms you set in Time Management App."
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 700, 300, 700)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        setContent {
            MaterialTheme {
                TimeManagementApp()
            }
        }
    }
}

@Composable
private fun TimeManagementApp() {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences("time_management_app", android.content.Context.MODE_PRIVATE)
    }
    val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val taskList = remember {
        mutableStateListOf<PlanTask>().apply {
            val savedTasks = preferences.getString("tasks", null)
            if (savedTasks == null) {
                add(PlanTask("Plan today's priorities"))
            } else {
                try {
                    val savedArray = JSONArray(savedTasks)
                    for (index in 0 until savedArray.length()) {
                        val task = savedArray.getJSONObject(index)
                        add(
                            PlanTask(
                                name = task.getString("name"),
                                isDone = task.optBoolean("done"),
                                timeGoalMinutes = task.optInt("goalMinutes", 30),
                                createdAt = task.optLong("createdAt", System.currentTimeMillis()),
                                rewardClaimed = task.optBoolean("rewardClaimed", task.optBoolean("done"))
                            )
                        )
                    }
                } catch (error: Exception) {
                    throw IllegalStateException("Could not load saved tasks.", error)
                }
            }
        }
    }

    val activities = remember {
        mutableStateListOf<ActivityType>().apply {
            addAll(StarterActivities)
            val savedActivities = preferences.getString("custom_activities", null)
            if (savedActivities != null) {
                try {
                    val savedArray = JSONArray(savedActivities)
                    for (index in 0 until savedArray.length()) {
                        val name = savedArray.getString(index)
                        if (none { it.name.equals(name, ignoreCase = true) }) {
                            add(ActivityType(name, "✨", 25))
                        }
                    }
                } catch (error: Exception) {
                    throw IllegalStateException("Could not load your custom activities.", error)
                }
            }
        }
    }
    val activityDurations = remember {
        mutableStateMapOf<String, Int>().apply {
            activities.forEach { activity ->
                put(activity.name, preferences.getInt("duration_${activity.name}", activity.defaultMinutes))
            }
        }
    }
    var selectedActivityName by remember {
        mutableStateOf(preferences.getString("selected_activity", "Study") ?: "Study")
    }
    val selectedActivity = activities.firstOrNull { it.name == selectedActivityName } ?: activities.first()
    val selectedLength = activityDurations[selectedActivity.name] ?: selectedActivity.defaultMinutes
    var activeSection by remember { mutableStateOf("Focus") }
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var stopwatchStartedAt by remember { mutableStateOf<Long?>(null) }
    var stopwatchElapsedMillis by remember { mutableStateOf(0L) }
    var alarmAtMillis by remember {
        mutableStateOf(preferences.getLong(ALARM_PREFERENCES_KEY, 0L).takeIf { it > 0L })
    }
    var pendingAlarmTime by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showConfetti by remember { mutableStateOf(false) }
    var showSessionReward by remember { mutableStateOf(false) }
    var showQuickGame by remember { mutableStateOf(false) }
    var trackedMinutes by remember {
        mutableIntStateOf(preferences.getInt("tracked_minutes_$todayKey", 0))
    }
    var rewardPoints by remember {
        mutableIntStateOf(preferences.getInt("reward_points_$todayKey", 0))
    }
    var remainingSeconds by remember { mutableIntStateOf(selectedLength * 60) }
    var isRunning by remember { mutableStateOf(false) }
    var showDurationDialog by remember { mutableStateOf(false) }
    var showTaskDialog by remember { mutableStateOf(false) }
    var showActivityDialog by remember { mutableStateOf(false) }
    var rewardMessage by remember { mutableStateOf("") }
    var musicUri by remember {
        mutableStateOf(preferences.getString("music_uri", BUILT_IN_MUSIC_URI))
    }
    var musicName by remember {
        mutableStateOf(preferences.getString("music_name", "Soft focus (built in)"))
    }
    var isMusicPlaying by remember { mutableStateOf(false) }
    var isMusicLoading by remember { mutableStateOf(false) }
    var musicError by remember { mutableStateOf("") }
    val player = remember { mutableStateOf<MediaPlayer?>(null) }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                val displayName = context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                    } else {
                        null
                    }
                } ?: "Selected music"
                musicUri = uri.toString()
                musicName = displayName
                preferences.edit()
                    .putString("music_uri", musicUri)
                    .putString("music_name", musicName)
                    .apply()
                player.value = null
                isMusicPlaying = false
                isMusicLoading = false
                musicError = ""
            } catch (error: SecurityException) {
                musicError = "This music file could not be opened. Please choose it again."
            }
        }
    }

    val activePlayer = player.value
    DisposableEffect(activePlayer) {
        onDispose { activePlayer?.release() }
    }

    fun awardPoints(points: Int, message: String) {
        rewardPoints += points
        preferences.edit().putInt("reward_points_$todayKey", rewardPoints).apply()
        rewardMessage = "$message  +$points points"
    }

    fun setReminder(hour: Int, minute: Int) {
        val alarmClock = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val alarmIntent = alarmPendingIntent(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            alarmManager.canScheduleExactAlarms()
        ) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmClock.timeInMillis,
                alarmIntent
            )
        } else {
            // This still works without special exact-alarm access; Android may delay it a little.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmClock.timeInMillis,
                alarmIntent
            )
        }
        alarmAtMillis = alarmClock.timeInMillis
        preferences.edit().putLong(ALARM_PREFERENCES_KEY, alarmClock.timeInMillis).apply()
    }

    val notificationPermissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        pendingAlarmTime?.let { (hour, minute) ->
            if (granted) setReminder(hour, minute)
            pendingAlarmTime = null
        }
    }

    fun selectMusic(name: String, uri: String) {
        player.value = null
        musicUri = uri
        musicName = name
        isMusicPlaying = false
        isMusicLoading = false
        musicError = ""
        preferences.edit()
            .putString("music_uri", uri)
            .putString("music_name", name)
            .apply()
    }

    fun openMusicSearch(serviceUrl: String, searchText: String) {
        val searchUrl = "$serviceUrl${Uri.encode(searchText)}"
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)))
        } catch (error: ActivityNotFoundException) {
            musicError = "No browser or music app is available to open that search."
        } catch (error: SecurityException) {
            musicError = "Your device could not open that music search."
        }
    }

    LaunchedEffect(isRunning, remainingSeconds) {
        // Keeping the countdown in Compose state means pausing it also pauses this delay.
        if (isRunning && remainingSeconds > 0) {
            delay(1000)
            remainingSeconds--
        } else if (isRunning) {
            isRunning = false
            if (selectedActivity.name != "Break") {
                trackedMinutes += selectedLength
                preferences.edit().putInt("tracked_minutes_$todayKey", trackedMinutes).apply()
            }
            val points = if (selectedActivity.name == "Break") 3 else 5
            awardPoints(
                points,
                if (selectedActivity.name == "Break") "Break finished!" else "${selectedActivity.name} session complete!"
            )
            showConfetti = true
            if (selectedActivity.name != "Break") showSessionReward = true
            remainingSeconds = selectedLength * 60
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000)
        }
    }

    LaunchedEffect(stopwatchStartedAt) {
        while (stopwatchStartedAt != null) {
            stopwatchElapsedMillis = stopwatchElapsedMillis.coerceAtLeast(
                SystemClock.elapsedRealtime() - stopwatchStartedAt!!
            )
            delay(100)
        }
    }

    LaunchedEffect(showConfetti) {
        if (showConfetti) {
            delay(2800)
            showConfetti = false
        }
    }

    LaunchedEffect(rewardMessage) {
        if (rewardMessage.isNotEmpty()) {
            delay(4000)
            rewardMessage = ""
        }
    }

    fun saveTasks() {
        val savedArray = JSONArray()
        taskList.forEach { task ->
            savedArray.put(
                JSONObject()
                    .put("name", task.name)
                    .put("done", task.isDone)
                    .put("goalMinutes", task.timeGoalMinutes)
                    .put("createdAt", task.createdAt)
                    .put("rewardClaimed", task.rewardClaimed)
            )
        }
        preferences.edit().putString("tasks", savedArray.toString()).apply()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(PageColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 22.dp, top = 14.dp, end = 22.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionTab(
                label = "Focus",
                selected = activeSection == "Focus",
                onClick = { activeSection = "Focus" },
                modifier = Modifier.weight(1f)
            )
            SectionTab(
                label = "Tools",
                selected = activeSection == "Tools",
                onClick = { activeSection = "Tools" },
                modifier = Modifier.weight(1f)
            )
        }

        if (activeSection == "Focus") {
    LazyColumn(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .background(PageColor),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 22.dp,
            top = 28.dp,
            end = 22.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Text(
                    text = "TIME MANAGEMENT APP",
                    color = AccentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Make time for life.",
                    color = InkColor,
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = "Build better routines, one thing at a time.",
                    color = MutedColor,
                    fontSize = 15.sp
                )
            }
        }

        item {
            ProgressCard(trackedMinutes = trackedMinutes, rewardPoints = rewardPoints)
        }

        item {
            AnimatedVisibility(
                visible = rewardMessage.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Surface(color = SoftGreen, shape = RoundedCornerShape(16.dp)) {
                    Text(
                        text = "★  $rewardMessage",
                        color = InkColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Choose an activity",
                        color = InkColor,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "EDIT TIME",
                        color = AccentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showDurationDialog = true }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activities.forEach { activity ->
                        ActivityChip(
                            activity = activity,
                            selected = activity.name == selectedActivity.name,
                            onClick = {
                                isRunning = false
                                selectedActivityName = activity.name
                                preferences.edit().putString("selected_activity", activity.name).apply()
                                remainingSeconds = (activityDurations[activity.name]
                                    ?: activity.defaultMinutes) * 60
                            }
                        )
                    }
                    Text(
                        text = "+ Custom",
                        color = AccentColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardColor)
                            .clickable { showActivityDialog = true }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }

                Surface(
                    color = CardColor,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TimerRing(
                            remainingSeconds = remainingSeconds,
                            totalSeconds = selectedLength * 60
                        )
                        Spacer(Modifier.height(18.dp))
                        Text(
                            text = "${selectedActivity.emoji}  ${selectedActivity.name} time. Make it count.",
                            color = MutedColor,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    isRunning = !isRunning
                                    rewardMessage = ""
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = InkColor,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f).height(50.dp)
                            ) {
                                Text(
                                    text = if (isRunning) "Pause" else "Start ${selectedActivity.name.lowercase()}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    isRunning = false
                                    remainingSeconds = selectedLength * 60
                                    rewardMessage = ""
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Text("Reset", color = InkColor)
                            }
                        }
                    }
                }
            }
        }

        item {
            MusicCard(
                musicName = musicName,
                isPlaying = isMusicPlaying,
                isLoading = isMusicLoading,
                errorMessage = musicError,
                onChooseBuiltInMusic = {
                    selectMusic("Soft focus (built in)", BUILT_IN_MUSIC_URI)
                },
                onChooseMusic = { audioPicker.launch(arrayOf("audio/*")) },
                onChooseFocusRadio = {
                    selectMusic("SomaFM · Drone Zone", SOMAFM_FOCUS_STREAM)
                },
                onChooseTaskRadio = {
                    selectMusic("SomaFM · Groove Salad", SOMAFM_TASK_STREAM)
                },
                onOpenSpotify = {
                    // These buttons search the chosen service; they don't need account access.
                    openMusicSearch(
                        "https://open.spotify.com/search/",
                        musicSearchFor(selectedActivity.name)
                    )
                },
                onOpenYouTube = {
                    openMusicSearch(
                        "https://music.youtube.com/search?q=",
                        musicSearchFor(selectedActivity.name)
                    )
                },
                onToggleMusic = {
                    val savedUri = musicUri
                    if (savedUri == null) {
                        musicError = "Choose a music file first."
                    } else if (isMusicPlaying) {
                        player.value?.pause()
                        isMusicPlaying = false
                        isMusicLoading = false
                    } else if (player.value?.isPlaying == false) {
                        player.value?.start()
                        isMusicPlaying = true
                        musicError = ""
                    } else {
                        try {
                            if (savedUri == BUILT_IN_MUSIC_URI) {
                                val newPlayer = MediaPlayer.create(context, R.raw.soft_focus)
                                if (newPlayer == null) {
                                    musicError = "The built-in music could not be opened."
                                } else {
                                    newPlayer.isLooping = true
                                    newPlayer.start()
                                    player.value = newPlayer
                                    isMusicPlaying = true
                                    musicError = ""
                                }
                            } else {
                                val newPlayer = MediaPlayer()
                                newPlayer.setDataSource(context, Uri.parse(savedUri))
                                newPlayer.isLooping = true
                                isMusicLoading = true
                                newPlayer.setOnPreparedListener { preparedPlayer ->
                                    preparedPlayer.start()
                                    isMusicPlaying = true
                                    isMusicLoading = false
                                    musicError = ""
                                }
                                newPlayer.setOnErrorListener { _, _, _ ->
                                    isMusicPlaying = false
                                    isMusicLoading = false
                                    player.value = null
                                    musicError = "This audio file could not be played. Try another file."
                                    true
                                }
                                player.value = newPlayer
                                newPlayer.prepareAsync()
                            }
                        } catch (error: IOException) {
                            isMusicLoading = false
                            musicError = "This audio file could not be opened. Try another file."
                        } catch (error: IllegalArgumentException) {
                            isMusicLoading = false
                            musicError = "This audio file could not be played. Try another file."
                        } catch (error: SecurityException) {
                            isMusicLoading = false
                            musicError = "This music file could not be opened. Choose it again."
                        } catch (error: IllegalStateException) {
                            isMusicLoading = false
                            musicError = "Music could not start. Please try again."
                        }
                    }
                }
            )
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's plan",
                        color = InkColor,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "${taskList.count { it.isDone }} of ${taskList.size} tasks done",
                        color = MutedColor,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = "+ Add task",
                    color = AccentColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showTaskDialog = true }
                        .padding(8.dp)
                )
            }
        }

        if (taskList.isEmpty()) {
            item {
                Surface(color = CardColor, shape = RoundedCornerShape(16.dp)) {
                    Text(
                        text = "Nothing on your list yet. Add one thing you'd like to get done.",
                        color = MutedColor,
                        fontSize = 14.sp,
                        modifier = Modifier.fillMaxWidth().padding(18.dp)
                    )
                }
            }
        } else {
            itemsIndexed(taskList) { index, task ->
                TaskRow(
                    task = task,
                    onToggle = {
                        if (!task.isDone && !task.rewardClaimed) {
                            val wasOnTime = System.currentTimeMillis() - task.createdAt <=
                                task.timeGoalMinutes * 60_000L
                            val points = if (wasOnTime) 15 else 5
                            taskList[index] = task.copy(isDone = true, rewardClaimed = true)
                            awardPoints(
                                points,
                                if (wasOnTime) "Task done within your time goal!" else "Task done! Keep going!"
                            )
                        } else {
                            taskList[index] = task.copy(isDone = !task.isDone)
                        }
                        saveTasks()
                    },
                    onDelete = {
                        taskList.removeAt(index)
                        saveTasks()
                    }
                )
            }
        }

        item {
            Text(
                text = "© 2026 Jaineil Rana · Original ambient loop · SomaFM stream music © its artists",
                color = MutedColor,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp)
            )
        }
    }
        } else {
            ToolsScreen(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                nowMillis = currentTimeMillis,
                alarmAtMillis = alarmAtMillis,
                notificationsEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED,
                stopwatchElapsedMillis = stopwatchElapsedMillis,
                stopwatchRunning = stopwatchStartedAt != null,
                onSetAlarm = { hour, minute ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                        PackageManager.PERMISSION_GRANTED
                    ) {
                        pendingAlarmTime = hour to minute
                        notificationPermissionRequest.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        setReminder(hour, minute)
                    }
                },
                onCancelAlarm = {
                    context.getSystemService(AlarmManager::class.java)
                        .cancel(alarmPendingIntent(context))
                    alarmAtMillis = null
                    preferences.edit().remove(ALARM_PREFERENCES_KEY).apply()
                },
                onStartStopwatch = {
                    stopwatchStartedAt = SystemClock.elapsedRealtime() - stopwatchElapsedMillis
                },
                onPauseStopwatch = { stopwatchStartedAt = null },
                onResetStopwatch = {
                    stopwatchStartedAt = null
                    stopwatchElapsedMillis = 0L
                }
            )
        }
    }

    if (showConfetti) ConfettiBurst(Modifier.fillMaxSize())

    if (showSessionReward) {
        AlertDialog(
            onDismissRequest = { showSessionReward = false },
            title = { Text("You did it!", color = InkColor, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Your ${selectedActivity.name.lowercase()} session is complete. Enjoy a little reward, or take a five-minute breather.",
                    color = MutedColor
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSessionReward = false
                        showQuickGame = true
                    }
                ) { Text("Play for 5 minutes", color = AccentColor) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val breakActivity = activities.first { it.name == "Break" }
                        selectedActivityName = breakActivity.name
                        preferences.edit().putString("selected_activity", breakActivity.name).apply()
                        isRunning = true
                        remainingSeconds = 5 * 60
                        activityDurations[breakActivity.name] = 5
                        preferences.edit().putInt("duration_${breakActivity.name}", 5).apply()
                        showSessionReward = false
                    }
                ) { Text("Take a 5-minute break", color = InkColor) }
            },
            containerColor = CardColor
        )
    }

    if (showQuickGame) {
        QuickBreakGameDialog(onDismiss = { showQuickGame = false })
    }

    if (showDurationDialog) {
        DurationDialog(
            activityName = selectedActivity.name,
            minutes = selectedLength,
            onDismiss = { showDurationDialog = false },
            onSave = { newMinutes ->
                activityDurations[selectedActivity.name] = newMinutes
                preferences.edit().putInt("duration_${selectedActivity.name}", newMinutes).apply()
                if (!isRunning) remainingSeconds = newMinutes * 60
                showDurationDialog = false
            }
        )
    }

    if (showTaskDialog) {
        AddTaskDialog(
            onDismiss = { showTaskDialog = false },
            onAdd = { taskName, timeGoal ->
                taskList.add(PlanTask(taskName, timeGoalMinutes = timeGoal))
                saveTasks()
                showTaskDialog = false
            }
        )
    }

    if (showActivityDialog) {
        AddActivityDialog(
            existingNames = activities.map { it.name },
            onDismiss = { showActivityDialog = false },
            onAdd = { name, minutes ->
                val newActivity = ActivityType(name, "✨", minutes)
                activities.add(newActivity)
                activityDurations[name] = minutes
                isRunning = false
                selectedActivityName = name
                remainingSeconds = minutes * 60
                preferences.edit()
                    .putString("selected_activity", name)
                    .putInt("duration_$name", minutes)
                    .putString(
                        "custom_activities",
                        JSONArray(activities.filter { it !in StarterActivities }.map { it.name }).toString()
                    )
                    .apply()
                showActivityDialog = false
            }
        )
    }
}

@Composable
private fun ProgressCard(trackedMinutes: Int, rewardPoints: Int) {
    val dailyGoal = 240
    val progress = (trackedMinutes.toFloat() / dailyGoal).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(progress, label = "daily time progress")

    Surface(
        color = SoftGreen,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("TIME YOU MADE FOR YOURSELF", color = MutedColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "${trackedMinutes / 60}h ${trackedMinutes % 60}m",
                        color = InkColor,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("★ $rewardPoints points", color = InkColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text("Goal: 4 hours", color = MutedColor, fontSize = 12.sp)
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color.White)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(AccentColor)
                )
            }
            Text(
                text = if (trackedMinutes >= dailyGoal) "You reached your daily time goal. Great job!"
                else "${dailyGoal - trackedMinutes} minutes to reach today's goal",
                color = MutedColor,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun TimerRing(
    remainingSeconds: Int,
    totalSeconds: Int
) {
    val progress = if (totalSeconds > 0) {
        (remainingSeconds.toFloat() / totalSeconds).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(194.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val ringStyle = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = PageColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = ringStyle
            )
            if (progress == 1f) {
                // A full arc can leave a tiny gap at the join, so use a circle at 100%.
                drawCircle(color = AccentColor, style = ringStyle)
            } else {
                drawArc(
                    color = AccentColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = ringStyle
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedContent(targetState = remainingSeconds, label = "timer countdown") { secondsRemaining ->
                Text(
                    text = "%02d:%02d".format(secondsRemaining / 60, secondsRemaining % 60),
                    color = InkColor,
                    fontSize = 39.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text("MINUTES LEFT", color = MutedColor, fontSize = 10.sp, letterSpacing = 1.2.sp)
        }
    }
}

@Composable
private fun ActivityChip(activity: ActivityType, selected: Boolean, onClick: () -> Unit) {
    val chipColor by animateColorAsState(
        targetValue = if (selected) InkColor else CardColor,
        label = "activity selection color"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else InkColor,
        label = "activity label color"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(chipColor)
            .clickable(onClick = onClick)
            .animateContentSize()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "${activity.emoji} ${activity.name}",
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun TaskRow(task: PlanTask, onToggle: () -> Unit, onDelete: () -> Unit) {
    Surface(color = CardColor, shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, top = 13.dp, bottom = 13.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(23.dp)
                    .clip(CircleShape)
                    .background(if (task.isDone) AccentColor else PageColor)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                if (task.isDone) Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    color = if (task.isDone) MutedColor else InkColor,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Time goal: ${task.timeGoalMinutes} min",
                    color = MutedColor,
                    fontSize = 11.sp
                )
            }
            Text(
                text = "×",
                color = MutedColor,
                fontSize = 21.sp,
                modifier = Modifier.clickable(onClick = onDelete).padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
private fun DurationDialog(
    activityName: String,
    minutes: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var minutesText by remember { mutableStateOf(minutes.toString()) }
    val isValid = (minutesText.toIntOrNull() ?: 0) in 1..360

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set ${activityName.lowercase()} time", color = InkColor, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                NumberInput(label = "Timer length (minutes)", value = minutesText, onValueChange = { minutesText = it })
                Text("Choose any time from 1 to 360 minutes.", color = MutedColor, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(minutesText.toInt()) },
                enabled = isValid
            ) {
                Text("Save", color = if (isValid) AccentColor else MutedColor)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MutedColor) } },
        containerColor = CardColor
    )
}

@Composable
private fun NumberInput(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = InkColor, fontSize = 13.sp)
        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                if (newValue.length <= 3 && newValue.all { it.isDigit() }) onValueChange(newValue)
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(PageColor)
                .padding(12.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = InkColor)
        )
    }
}

@Composable
private fun AddTaskDialog(onDismiss: () -> Unit, onAdd: (String, Int) -> Unit) {
    var taskName by remember { mutableStateOf("") }
    var timeGoal by remember { mutableStateOf("30") }
    val trimmedName = taskName.trim()
    val goalMinutes = timeGoal.toIntOrNull() ?: 0
    val isValid = trimmedName.isNotEmpty() && goalMinutes in 1..360

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to your plan", color = InkColor, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("What's one thing you want to get done?", color = MutedColor, fontSize = 13.sp)
                BasicTextField(
                    value = taskName,
                    onValueChange = { taskName = it.take(80) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PageColor)
                        .padding(12.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = InkColor)
                )
                NumberInput(
                    label = "Time goal (minutes)",
                    value = timeGoal,
                    onValueChange = { timeGoal = it }
                )
                Text(
                    "Finish before this time to earn the bigger reward. The clock starts when you add the task.",
                    color = MutedColor,
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(trimmedName, goalMinutes) }, enabled = isValid) {
                Text("Add task", color = if (isValid) AccentColor else MutedColor)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MutedColor) } },
        containerColor = CardColor
    )
}

@Composable
private fun AddActivityDialog(
    existingNames: List<String>,
    onDismiss: () -> Unit,
    onAdd: (String, Int) -> Unit
) {
    var activityName by remember { mutableStateOf("") }
    var timeText by remember { mutableStateOf("25") }
    val name = activityName.trim()
    val minutes = timeText.toIntOrNull() ?: 0
    val alreadyExists = existingNames.any { it.equals(name, ignoreCase = true) }
    val canAdd = name.isNotEmpty() && !alreadyExists && minutes in 1..360

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Make your own timer", color = InkColor, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Name any activity you want to make time for.", color = MutedColor, fontSize = 13.sp)
                BasicTextField(
                    value = activityName,
                    onValueChange = { activityName = it.take(30) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PageColor)
                        .padding(12.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = InkColor)
                )
                NumberInput(
                    label = "Timer length (minutes)",
                    value = timeText,
                    onValueChange = { timeText = it }
                )
                if (alreadyExists) {
                    Text("You already have a timer with that name.", color = AccentColor, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(name, minutes) }, enabled = canAdd) {
                Text("Create timer", color = if (canAdd) AccentColor else MutedColor)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MutedColor) } },
        containerColor = CardColor
    )
}

@Composable
private fun MusicCard(
    musicName: String?,
    isPlaying: Boolean,
    isLoading: Boolean,
    errorMessage: String,
    onChooseBuiltInMusic: () -> Unit,
    onChooseMusic: () -> Unit,
    onChooseFocusRadio: () -> Unit,
    onChooseTaskRadio: () -> Unit,
    onOpenSpotify: () -> Unit,
    onOpenYouTube: () -> Unit,
    onToggleMusic: () -> Unit
) {
    Surface(color = CardColor, shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Focus music", color = InkColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                text = musicName ?: "Original ambient sound or music from your device.",
                color = MutedColor,
                fontSize = 13.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onChooseBuiltInMusic,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Original ambient", color = InkColor)
                }
                OutlinedButton(
                    onClick = onChooseMusic,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("My music", color = InkColor)
                }
            }
            MusicCardAdditionalControls(
                musicName = musicName,
                isPlaying = isPlaying,
                isLoading = isLoading,
                errorMessage = errorMessage,
                onChooseFocusRadio = onChooseFocusRadio,
                onChooseTaskRadio = onChooseTaskRadio,
                onOpenSpotify = onOpenSpotify,
                onOpenYouTube = onOpenYouTube,
                onToggleMusic = onToggleMusic
            )
        }
    }
}

@Composable
private fun SectionTab(
                label: String,
                selected: Boolean,
                onClick: () -> Unit,
                modifier: Modifier = Modifier
            ) {
                Surface(
                    modifier = modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
                    color = if (selected) InkColor else CardColor,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = label,
                        color = if (selected) Color.White else InkColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 12.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            @Composable
            private fun ToolsScreen(
                modifier: Modifier,
                nowMillis: Long,
                alarmAtMillis: Long?,
                notificationsEnabled: Boolean,
                stopwatchElapsedMillis: Long,
                stopwatchRunning: Boolean,
                onSetAlarm: (Int, Int) -> Unit,
                onCancelAlarm: () -> Unit,
                onStartStopwatch: () -> Unit,
                onPauseStopwatch: () -> Unit,
                onResetStopwatch: () -> Unit
            ) {
                val context = LocalContext.current
                val localZone = TimeZone.getDefault()
                val currentDate = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date(nowMillis))
                val currentTime = SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(nowMillis))
                val zoneName = localZone.getDisplayName(localZone.inDaylightTime(Date(nowMillis)), TimeZone.LONG, Locale.getDefault())
                val activeAlarmAtMillis = alarmAtMillis?.takeIf { it > nowMillis }
                val alarmText = activeAlarmAtMillis?.let {
                    SimpleDateFormat("EEE, h:mm a", Locale.getDefault()).format(Date(it))
                }
                val minutes = TimeUnit.MILLISECONDS.toMinutes(stopwatchElapsedMillis)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(stopwatchElapsedMillis) % 60
                val hundredths = (stopwatchElapsedMillis % 1000) / 10

                Column(
                    modifier = modifier
                        .background(PageColor)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column {
                        Text("A few handy extras", color = InkColor, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text("Little tools for making the day easier.", color = MutedColor, fontSize = 14.sp)
                    }

                    Surface(color = CardColor, shape = RoundedCornerShape(20.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Your local time", color = InkColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(currentTime, color = InkColor, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                            Text(currentDate, color = MutedColor, fontSize = 14.sp)
                            Text("$zoneName · ${localZone.id}", color = MutedColor, fontSize = 12.sp)
                            Text("Uses the time zone set on this device.", color = MutedColor, fontSize = 11.sp)
                        }
                    }

                    Surface(color = CardColor, shape = RoundedCornerShape(20.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Alarm", color = InkColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = alarmText?.let { "Next reminder: $it" } ?: "Set a reminder for a time that works for you.",
                                color = MutedColor,
                                fontSize = 13.sp
                            )
                            if (!notificationsEnabled) {
                                Text(
                                    "Allow notifications when Android asks so the alarm can show up.",
                                    color = AccentColor,
                                    fontSize = 12.sp
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        val now = Calendar.getInstance()
                                        TimePickerDialog(
                                            context,
                                            { _, hour, minute -> onSetAlarm(hour, minute) },
                                            now.get(Calendar.HOUR_OF_DAY),
                                            now.get(Calendar.MINUTE),
                                            android.text.format.DateFormat.is24HourFormat(context)
                                        ).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = InkColor),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (activeAlarmAtMillis == null) "Set alarm" else "Change alarm")
                                }
                                if (activeAlarmAtMillis != null) {
                                    OutlinedButton(onClick = onCancelAlarm, shape = RoundedCornerShape(12.dp)) {
                                        Text("Cancel", color = InkColor)
                                    }
                                }
                            }
                            Text(
                                "Android may deliver reminders a little later when battery saving is on.",
                                color = MutedColor,
                                fontSize = 11.sp
                            )
                            Text("Alarm loudness follows your phone's alarm-volume setting.", color = MutedColor, fontSize = 11.sp)
                        }
                    }

                    Surface(color = SoftGreen, shape = RoundedCornerShape(20.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Stopwatch", color = InkColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "%02d:%02d.%02d".format(minutes, seconds, hundredths),
                                color = InkColor,
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = if (stopwatchRunning) onPauseStopwatch else onStartStopwatch,
                                    colors = ButtonDefaults.buttonColors(containerColor = InkColor),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (stopwatchRunning) "Pause" else "Start")
                                }
                                OutlinedButton(onClick = onResetStopwatch, shape = RoundedCornerShape(12.dp)) {
                                    Text("Reset", color = InkColor)
                                }
                            }
                        }
                    }
                }
            }

            @Composable
            private fun ConfettiBurst(modifier: Modifier = Modifier) {
                var progress by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
                val confettiColors = listOf(AccentColor, Color(0xFF5A9D79), Color(0xFFF2C14E), Color(0xFF7299CC))

                LaunchedEffect(Unit) {
                    for (frame in 0..28) {
                        progress = frame / 28f
                        delay(100)
                    }
                }

                Canvas(modifier = modifier) {
                    for (piece in 0 until 42) {
                        val x = size.width * ((piece * 37 % 100) / 100f)
                        val startY = -size.height * ((piece * 19 % 40) / 100f)
                        val y = (startY + progress * (size.height + 80f)) % (size.height + 80f)
                        drawCircle(
                            color = confettiColors[piece % confettiColors.size],
                            radius = 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }

            @Composable
            private fun QuickBreakGameDialog(onDismiss: () -> Unit) {
                var secondsLeft by remember { mutableIntStateOf(QUICK_GAME_SECONDS) }
                var score by remember { mutableIntStateOf(0) }

                LaunchedEffect(secondsLeft) {
                    if (secondsLeft > 0) {
                        delay(1000)
                        secondsLeft--
                    }
                }

                Dialog(onDismissRequest = onDismiss) {
                    Surface(color = CardColor, shape = RoundedCornerShape(24.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Quick break", color = InkColor, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                            Text(
                                if (secondsLeft > 0) "A tiny five-minute game. Tap the star when it pops up."
                                else "Break's over. Nice work — time to get back to your day.",
                                color = MutedColor,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Text(
                                "%02d:%02d left".format(secondsLeft / 60, secondsLeft % 60),
                                color = InkColor,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (secondsLeft > 0) {
                                    Button(
                                        onClick = { score++ },
                                        modifier = Modifier.offset(x = ((score * 47 % 121) - 60).dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                                        shape = CircleShape
                                    ) {
                                        Text("★  Tap!", fontSize = 17.sp)
                                    }
                                }
                            }
                            Text("Stars tapped: $score", color = MutedColor, fontSize = 14.sp)
                            TextButton(onClick = onDismiss) {
                                Text(if (secondsLeft == 0) "Done" else "End break", color = InkColor)
                            }
                        }
                    }
                }
            }
