package com.application.cadence.data.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.application.cadence.CadenceApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as CadenceApplication
                val lessons = app.lessonRepository.observeAll().first()
                NotificationScheduler.sync(context, lessons)
            } finally {
                pending.finish()
            }
        }
    }
}
