package com.mapilule.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.mapilule.app.data.security.PinSecurityManager
import com.mapilule.app.reminder.ReminderScheduler
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        ReminderScheduler.scheduleDailyReminder(this)
        val security = PinSecurityManager(this)
        val prefs = getSharedPreferences("pill_records", MODE_PRIVATE)
        setContent {
            var configured by remember { mutableStateOf(security.isPinConfigured()) }
            var unlocked by remember { mutableStateOf(false) }
            var pin by remember { mutableStateOf("") }
            var confirmPin by remember { mutableStateOf("") }
            var error by remember { mutableStateOf("") }
            var page by remember { mutableStateOf("today") }
            var refresh by remember { mutableIntStateOf(0) }
            val today = LocalDate.now().toString()
            val taken = remember(refresh, today) { prefs.getBoolean(today, false) }
            val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
            DisposableEffect(Unit) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) { unlocked = false; pin = ""; page = "today" }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }
            MaterialTheme(colorScheme = lightColorScheme(primary = androidx.compose.ui.graphics.Color(0xFF345A84))) {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(24.dp))
                        Text("Ma Pilule", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(24.dp))
                        if (!configured || !unlocked) {
                            Text(if (!configured) "Créer un NIP de 4 à 6 chiffres" else "Entrer votre NIP", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(value = pin, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) { pin = it; error = "" } }, label = { Text("NIP") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true)
                            if (!configured) {
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(value = confirmPin, onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmPin = it }, label = { Text("Confirmer le NIP") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), singleLine = true)
                            }
                            if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = {
                                if (!configured) {
                                    if (pin.length !in 4..6 || pin != confirmPin) error = "Les NIP doivent correspondre (4 à 6 chiffres)."
                                    else if (security.setupPin(pin)) { configured = true; unlocked = true; pin = ""; confirmPin = "" }
                                } else if (security.verifyPin(pin)) { unlocked = true; pin = "" }
                                else { error = "NIP incorrect"; pin = "" }
                            }) { Text(if (configured) "Déverrouiller" else "Créer mon NIP") }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("today" to "Aujourd'hui", "history" to "Historique", "settings" to "Réglages").forEach { (id, label) ->
                                    TextButton(onClick = { page = id }) { Text(label) }
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                            when (page) {
                                "today" -> {
                                    Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.CANADA_FRENCH)), style = MaterialTheme.typography.titleLarge)
                                    Spacer(Modifier.height(30.dp))
                                    Text(if (taken) "Prise confirmée aujourd'hui" else "Prise non confirmée", style = MaterialTheme.typography.headlineSmall)
                                    Spacer(Modifier.height(20.dp))
                                    Button(onClick = {
                                        prefs.edit().putBoolean(today, !taken).apply(); refresh++
                                        ReminderScheduler.scheduleDailyReminder(this@MainActivity)
                                    }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(if (taken) "Annuler la confirmation" else "J'ai pris ma pilule") }
                                }
                                "history" -> {
                                    Text("30 derniers jours", style = MaterialTheme.typography.titleLarge)
                                    Spacer(Modifier.height(12.dp))
                                    LazyColumn { items((0..29).map { LocalDate.now().minusDays(it.toLong()) }) { date ->
                                        val done = prefs.getBoolean(date.toString(), false)
                                        Text("${date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.CANADA_FRENCH))}   ${if (done) "Confirmé" else "Non confirmé"}", modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp))
                                        HorizontalDivider()
                                    } }
                                }
                                "settings" -> {
                                    val settings = getSharedPreferences("reminder_settings", MODE_PRIVATE)
                                    var enabled by remember { mutableStateOf(settings.getBoolean("enabled", true)) }
                                    var hour by remember { mutableStateOf(settings.getInt("hour", 10).toString()) }
                                    var minute by remember { mutableStateOf(settings.getInt("minute", 0).toString().padStart(2, '0')) }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Rappels activés", modifier = Modifier.weight(1f))
                                        Switch(checked = enabled, onCheckedChange = { enabled = it; settings.edit().putBoolean("enabled", it).apply(); ReminderScheduler.scheduleDailyReminder(this@MainActivity) })
                                    }
                                    Text("Heure du rappel")
                                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        OutlinedTextField(hour, { if (it.length <= 2 && it.all(Char::isDigit)) hour = it }, label = { Text("Heure") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                        OutlinedTextField(minute, { if (it.length <= 2 && it.all(Char::isDigit)) minute = it }, label = { Text("Minute") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = {
                                        val h = hour.toIntOrNull(); val m = minute.toIntOrNull()
                                        if (h != null && m != null && h in 0..23 && m in 0..59) {
                                            settings.edit().putInt("hour", h).putInt("minute", m).apply()
                                            ReminderScheduler.scheduleDailyReminder(this@MainActivity)
                                        }
                                    }) { Text("Enregistrer l'heure") }
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedButton(onClick = { unlocked = false }) { Text("Verrouiller") }
                                    if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        Button(onClick = { notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Autoriser les notifications") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
