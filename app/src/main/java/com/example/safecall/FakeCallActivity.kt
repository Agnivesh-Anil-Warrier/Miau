package com.example.safecall

import android.Manifest
import android.app.Activity
import android.app.KeyguardManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.CallLog
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
import com.example.safecall.ui.theme.SafeCallTheme
import kotlinx.coroutines.delay

class FakeCallActivity : ComponentActivity(), SensorEventListener {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var callStartTime: Long = 0
    private var isCallAcceptedState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val callerName = intent.getStringExtra("CALLER_NAME") ?: "Unknown"
        val callerNumber = intent.getStringExtra("CALLER_NUMBER") ?: ""

        // Configure window flags to display full-screen over lock screen and turn screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        }

        // Start Ringtone
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        ringtone = RingtoneManager.getRingtone(this, ringtoneUri)
        ringtone?.apply {
            audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                isLooping = true
            }
            play()
        }

        // Start Vibration
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 1000, 1000)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }

        // Proximity Sensor Setup
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            @Suppress("DEPRECATION")
            wakeLock = powerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "SafeCall:ProximityLock")
        }

        setContent {
            val view = LocalView.current
            if (!view.isInEditMode) {
                SideEffect {
                    val window = (view.context as Activity).window
                    window.navigationBarColor = Color.Black.toArgb()
                    window.statusBarColor = Color.Transparent.toArgb()
                    WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
                }
            }

            SafeCallTheme {
                var isCallAccepted by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1C1C1C)
                ) {
                    if (!isCallAccepted) {
                        IncomingCallScreen(
                            name = callerName,
                            number = callerNumber,
                            onDecline = {
                                addCallToLog(callerName, callerNumber, CallLog.Calls.MISSED_TYPE, 0)
                                stopAndFinish()
                            },
                            onAccept = {
                                isCallAccepted = true
                                isCallAcceptedState = true
                                callStartTime = System.currentTimeMillis()
                                stopRinging()
                                startProximitySensor()
                            }
                        )
                    } else {
                        ActiveCallScreen(
                            name = callerName,
                            number = callerNumber,
                            onEndCall = {
                                val duration = (System.currentTimeMillis() - callStartTime) / 1000
                                addCallToLog(callerName, callerNumber, CallLog.Calls.INCOMING_TYPE, duration)
                                stopAndFinish()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun stopRinging() {
        ringtone?.stop()
        vibrator?.cancel()
    }

    private fun startProximitySensor() {
        sensorManager?.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private fun addCallToLog(
        name: String,
        number: String,
        type: Int,
        duration: Long
    ) {
        if (
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val values = ContentValues().apply {
            put(CallLog.Calls.NUMBER, number)
            put(CallLog.Calls.TYPE, type)
            put(CallLog.Calls.DATE, System.currentTimeMillis())
            put(CallLog.Calls.DURATION, duration)
            put(CallLog.Calls.NEW, 1)
        }

        try {
            contentResolver.insert(
                CallLog.Calls.CONTENT_URI,
                values
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAndFinish() {
        stopRinging()
        sensorManager?.unregisterListener(this)
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        finish()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
            val distance = event.values[0]
            if (distance < (proximitySensor?.maximumRange ?: 0f)) {
                // Near: Turn screen off
                if (wakeLock?.isHeld == false) {
                    wakeLock?.acquire(10 * 60 * 1000L /*10 minutes*/)
                }
            } else {
                // Far: Turn screen on
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        stopAndFinish()
        super.onDestroy()
    }
}

@Composable
fun IncomingCallScreen(
    name: String,
    number: String,
    location: String = "New Delhi, ND",
    onDecline: () -> Unit,
    onAccept: () -> Unit,
    onSmsReply: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(
        label = "chevron_bounce"
    )

    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0f at 0 with FastOutSlowInEasing
                -16f at 400 with FastOutSlowInEasing
                -16f at 600
                0f at 1000 with FastOutSlowInEasing
                0f at 1200
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "offsetY"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(
                horizontal = 24.dp,
                vertical = 32.dp
            ),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Normal
            )

            if (number.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = number,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (location.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = location,
                    color = Color.Gray,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                text = "Incoming call",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardDoubleArrowUp,
                        contentDescription = null,
                        tint = Color(0xFFFF5252).copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(32.dp)
                            .offset(y = offsetY.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    IconButton(
                        onClick = onDecline,
                        modifier = Modifier
                            .size(76.dp)
                            .background(
                                color = Color(0xFFFF3B30),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Decline",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardDoubleArrowUp,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50).copy(alpha = 0.8f),
                        modifier = Modifier
                            .size(32.dp)
                            .offset(y = offsetY.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier
                            .size(76.dp)
                            .background(
                                color = Color(0xFF34C759),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Accept",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onSmsReply,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2C2C2E)
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(
                    horizontal = 24.dp,
                    vertical = 10.dp
                )
            ) {
                Text(
                    text = "SMS reply",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ActiveCallScreen(
    name: String,
    number: String,
    country: String = "India",
    onEndCall: () -> Unit
) {
    var seconds by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            seconds++
        }
    }

    val timeText = String.format("%02d:%02d", seconds / 60, seconds % 60)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(
                text = name,
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.Normal
            )
            if (number.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = number,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 22.sp
                )
            }
            if (country.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = country,
                    color = Color.Gray,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "HD",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .background(Color.Transparent)
                )
                Text(
                    text = timeText,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CallActionButton(
                    icon = Icons.Default.Mic,
                    label = "Record",
                    onClick = { }
                )
                CallActionButton(
                    icon = Icons.Default.Pause,
                    label = "Hold",
                    onClick = { }
                )
                CallActionButton(
                    icon = Icons.Default.Add,
                    label = "Add call",
                    onClick = { }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CallActionButton(
                    icon = Icons.Default.MicOff,
                    label = "Mute",
                    onClick = { }
                )
                CallActionButton(
                    icon = Icons.Default.Videocam,
                    label = "Video call",
                    onClick = { }
                )
                Spacer(modifier = Modifier.width(72.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CallActionButton(
                    icon = Icons.Default.VolumeUp,
                    label = null,
                    onClick = { }
                )
                CallActionButton(
                    icon = Icons.Default.CallEnd,
                    label = null,
                    backgroundColor = Color(0xFFFF3B30),
                    iconTint = Color.White,
                    onClick = onEndCall
                )
                CallActionButton(
                    icon = Icons.Default.Dialpad,
                    label = null,
                    onClick = { }
                )
            }
        }
    }
}

@Composable
fun CallActionButton(
    icon: ImageVector,
    label: String?,
    backgroundColor: Color = Color(0xFF333333),
    iconTint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(72.dp)
                .background(color = backgroundColor, shape = CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
        }
        if (label != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun CallButton(onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(80.dp).background(color, CircleShape)
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(40.dp))
        }
        Text(label, color = Color.White, modifier = Modifier.padding(top = 8.dp))
    }
}
