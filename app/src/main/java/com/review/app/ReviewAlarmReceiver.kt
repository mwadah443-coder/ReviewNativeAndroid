package com.review.app
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
class ReviewAlarmReceiver: BroadcastReceiver(){ override fun onReceive(c:Context,i:Intent){ ReviewNotification.show(c,i.getStringExtra("id")?:"review",i.getStringExtra("title")?:"Review",i.getStringExtra("body")?:"حان وقت المراجعة") } }
object ReviewNotification { fun show(c:Context,id:String,title:String,body:String){ val n=NotificationCompat.Builder(c,"review_reminders").setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(title).setContentText(body).setAutoCancel(true).build(); try{NotificationManagerCompat.from(c).notify(id.hashCode(),n)}catch(_:SecurityException){} } }
