package com.example.safecall

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContactPage
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.safecall.ui.theme.SafeCallTheme
import com.example.safecall.ui.theme.SafeCallRed

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CallNotification.createChannel(this)
        enableEdgeToEdge()
        setContent {
            SafeCallTheme {
                val context = LocalContext.current
                var hasCallLogPermission by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.WRITE_CALL_LOG
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                var selectedContactName by remember { mutableStateOf("") }
                var selectedContactNumber by remember { mutableStateOf("") }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasCallLogPermission = isGranted
                }

                val contactPicker = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    if (result.resultCode == RESULT_OK) {
                        val contactUri = result.data?.data ?: return@rememberLauncherForActivityResult
                        val projection = arrayOf(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                        )

                        contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                selectedContactName = cursor.getString(nameIndex)
                                selectedContactNumber = cursor.getString(numberIndex)
                            }
                        }
                    }
                }

                val pickContactIntent = Intent(
                    Intent.ACTION_PICK,
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background
                ) { innerPadding ->
                    FakeCallSetupScreen(
                        modifier = Modifier.padding(innerPadding),
                        selectedContactName = selectedContactName,
                        selectedContactNumber = selectedContactNumber,
                        onPickContact = { contactPicker.launch(pickContactIntent) },
                        onScheduleCall = { name, number, seconds ->
                            if (!hasCallLogPermission) {
                                permissionLauncher.launch(Manifest.permission.WRITE_CALL_LOG)
                            }
                            scheduleFakeCall(name, number, seconds)
                        },
                        onCancelCall = { cancelScheduledCall() }
                    )
                }
            }
        }
    }

    private fun scheduleFakeCall(name: String, number: String, seconds: Int) {
        if (seconds <= 0) {
            Toast.makeText(this, "Please set a valid delay", Toast.LENGTH_SHORT).show()
            return
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(
                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:${this.packageName}")
                )
                startActivity(intent)
                Toast.makeText(this, "Please grant permission for exact alarms", Toast.LENGTH_LONG).show()
                return
            }
        }

        val effectiveName = name.ifBlank { "Unknown" }
        val intent = Intent(this, CallReceiver::class.java).apply {
            putExtra("CALLER_NAME", effectiveName)
            putExtra("CALLER_NUMBER", number)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = System.currentTimeMillis() + (seconds * 1000L)
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent)

        try {
            alarmManager.setAlarmClock(
                alarmClockInfo,
                pendingIntent
            )

            val minutes = seconds / 60
            val remainingSeconds = seconds % 60
            val timeText = if (minutes > 0) "${minutes}m ${remainingSeconds}s" else "${seconds}s"

            Toast.makeText(this, "Fake call scheduled in $timeText", Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, "Permission denied for exact alarm", Toast.LENGTH_LONG).show()
        }
    }

    private fun cancelScheduledCall() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, CallReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Toast.makeText(this, "Scheduled call cancelled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No call scheduled", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FakeCallSetupScreen(
    modifier: Modifier = Modifier,
    selectedContactName: String,
    selectedContactNumber: String,
    onPickContact: () -> Unit,
    onScheduleCall: (String, String, Int) -> Unit,
    onCancelCall: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf("") }

    var minutesInput by remember { mutableStateOf("0") }
    var secondsInput by remember { mutableStateOf("10") }

    fun updateInputsFromSeconds(seconds: Int) {
        minutesInput = (seconds / 60).toString()
        secondsInput = (seconds % 60).toString()
    }

    val currentTotalSeconds = remember(minutesInput, secondsInput) {
        val mins = minutesInput.toIntOrNull() ?: 0
        val secs = secondsInput.toIntOrNull() ?: 0
        (mins * 60) + secs
    }

    LaunchedEffect(selectedContactName, selectedContactNumber) {
        if (selectedContactName.isNotEmpty()) {
            name = selectedContactName
            number = selectedContactNumber
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Screen Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.Black, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "App Icon",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Miau",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(120.dp))
                Column {
                    val context = LocalContext.current

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/Agnivesh-Anil-Warrier")
                            )
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_github),
                            contentDescription = "GitHub",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Caller Info Section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Caller Information",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Caller Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        trailingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                VerticalDivider(
                                    modifier = Modifier
                                        .height(24.dp)
                                        .padding(horizontal = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                IconButton(onClick = onPickContact) {
                                    Icon(
                                        imageVector = Icons.Default.ContactPage,
                                        contentDescription = "Pick Contact",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = number,
                        onValueChange = { number = it },
                        label = { Text("Caller Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                }
            }

            // Timer Delay Section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Timer Delay",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Preset Quick Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presetTimes = listOf(5 to "5s", 15 to "15s", 30 to "30s", 60 to "1m", 300 to "5m")
                        presetTimes.forEach { (seconds, label) ->
                            FilterChip(
                                selected = currentTotalSeconds == seconds,
                                onClick = { updateInputsFromSeconds(seconds) },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Editable Minutes & Seconds Text Fields
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = minutesInput,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.all { it.isDigit() }) {
                                    minutesInput = input
                                }
                            },
                            label = { Text("Minutes") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = ":",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = secondsInput,
                            onValueChange = { input ->
                                if (input.isEmpty() || input.all { it.isDigit() }) {
                                    secondsInput = input
                                }
                            },
                            label = { Text("Seconds") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Precision Slider (Syncs automatically with typed inputs)
                    Slider(
                        value = currentTotalSeconds.coerceIn(0, 300).toFloat(),
                        onValueChange = { updateInputsFromSeconds(it.toInt()) },
                        valueRange = 0f..300f,
                        steps = 59
                    )
                }
            }
        }

        // Action Buttons
        Column(
            modifier = Modifier.padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { onScheduleCall(name, number, currentTotalSeconds) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Alarm, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Schedule Fake Call", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onCancelCall,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Cancel, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Cancel Scheduled Call")
            }
        }
    }
}
