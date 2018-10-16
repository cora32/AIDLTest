package ru.livli.swsdk.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import ru.livli.swsdk.models.Time
import ru.livli.swsdk.utils.Queueable
import ru.livli.swsdk.utils.sp

internal class StopperService : Service() {
    private val ACTIVE_TIME = "ACTIVE_TIME"
    private val APP_CLOSED = "APP_CLOSED"
    private var firstStartTime = 0L
    override fun onBind(p0: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        firstStartTime = System.currentTimeMillis()
        return Service.START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val now = System.currentTimeMillis()
        applicationContext?.sp()?.edit()?.putLong(APP_CLOSED, now)?.apply()
        Queueable.queue(ACTIVE_TIME, Time(now - firstStartTime))
        super.onTaskRemoved(rootIntent)
    }
}