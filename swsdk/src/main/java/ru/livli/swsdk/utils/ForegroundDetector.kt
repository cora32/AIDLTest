package ru.livli.swsdk.utils

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import ru.livli.swsdk.models.Time

internal class ForegroundDetector(val context: Context) : LifecycleObserver {
    private val APP_PAUSE = "APP_PAUSE"
    private val APP_OPENED = "APP_OPENED"
    private val APP_CLOSED = "APP_CLOSED"
    private val APP_EXIT = "APP_EXIT"
    private val FOREGROUND_TIME = "FOREGROUND_TIME"
    private var resumeTime = 0L

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
//        if (App.isByNotification)
//            Firebase.logCustomEvent("toForegroundWithNotification", "utc" to System.currentTimeMillis().toString())
//        else
//            Firebase.logCustomEvent("toForegroundNoNotification", "utc" to System.currentTimeMillis().toString())
//        App.isByNotification = false

        val now = System.currentTimeMillis()
        val pausedTime = now - context.sp().getLong(APP_PAUSE, now)
        if (pausedTime > 0) {
//            Firebase.logCustomEvent(APP_PAUSE, "time" to pausedTime)
            Queueable.queue(APP_PAUSE, Time(pausedTime))
            context.sp().edit().putLong(APP_PAUSE, 0).apply()
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onMoveToForeground() {
        resumeTime = System.currentTimeMillis()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onMoveToBackground() {
        context.sp().edit().putLong(APP_PAUSE, System.currentTimeMillis()).apply()
        val obj = Time(System.currentTimeMillis() - resumeTime)
        Queueable.queue(FOREGROUND_TIME, obj)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
//        Firebase.logCustomEvent(APP_OPENED, "utc" to System.currentTimeMillis().toString())
        Queueable.queue(APP_OPENED, Time(System.currentTimeMillis()))
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        context.sp().edit().putLong(APP_CLOSED, System.currentTimeMillis()).apply()
//        Firebase.logCustomEvent(APP_EXIT, "utc" to System.currentTimeMillis().toString())
        Queueable.queue(APP_EXIT, Time(System.currentTimeMillis()))
    }
}