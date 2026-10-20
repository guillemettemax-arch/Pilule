package com.mapilule.app.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.mapilule.app.MainActivity
import com.mapilule.app.R
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        ReminderScheduler.scheduleDailyReminder(context)
        if (context.getSharedPreferences("pill_records", Context.MODE_PRIVATE).getBoolean(LocalDate.now().toString(), false)) return
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel("pill_reminders", "Rappels de prise", NotificationManager.IMPORTANCE_HIGH))
        val pending = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(2001, NotificationCompat.Builder(context, "pill_reminders")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Ma Pilule")
            .setContentText("Avez-vous confirmé votre prise aujourd'hui ?")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending).setAutoCancel(true).build())
    }
}
