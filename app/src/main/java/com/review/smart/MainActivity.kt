package com.review.smart

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel

class MainActivity : ComponentActivity() {
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(5,8,6)
        window.navigationBarColor = android.graphics.Color.rgb(5,8,6)
        NotificationScheduler.ensureChannel(this)
        if (BuildConfig.ONESIGNAL_APP_ID.isNotBlank()) { OneSignal.Debug.logLevel = LogLevel.NONE; OneSignal.initWithContext(this, BuildConfig.ONESIGNAL_APP_ID) }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { ReviewApp() }
    }
}

object NotificationScheduler {
    private const val CHANNEL = "review_reminders"
    fun ensureChannel(c: Context) { if (Build.VERSION.SDK_INT >= 26) c.getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"Review reminders",NotificationManager.IMPORTANCE_DEFAULT)) }
    fun schedule(c: Context, id: String, title: String, body: String, at: Long) {
        val am=c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent=Intent(c, ReviewAlarmReceiver::class.java).apply { putExtra("id",id); putExtra("title",title); putExtra("body",body) }
        val pi=android.app.PendingIntent.getBroadcast(c,id.hashCode(),intent,android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        if (Build.VERSION.SDK_INT>=23) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi) else am.set(AlarmManager.RTC_WAKEUP,at,pi)
    }
}
