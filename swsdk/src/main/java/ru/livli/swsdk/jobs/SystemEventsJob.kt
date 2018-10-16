package ru.livli.swsdk.jobs

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.provider.CalendarContract
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import org.jetbrains.anko.coroutines.experimental.bg
import ru.livli.swsdk.models.SystemState
import ru.livli.swsdk.utils.Queueable

private val systemStateReceiver = SystemStateReceiver()
const val SYSTEM_STATE = "system_state"

internal class SystemStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Queueable.queue(SYSTEM_STATE, SystemState(intent.action ?: "", intent.dataString))
    }
}

internal class SystemEventsJob : JobService() {
    private var inProgress = false

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        if (inProgress) {
            jobFinished(jobParameters, true)
            return false
        }

        bg {
            inProgress = true
            applicationContext.registerSystemEvents()
        }.invokeOnCompletion {
            jobFinished(jobParameters, true)
            inProgress = false
        }
        return false
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        inProgress = false
        return false
    }
}


internal fun Context.registerSystemEvents() {
    try {
        unregisterReceiver(systemStateReceiver)
    } catch (ex: IllegalArgumentException) {
    }

    registerReceiver(systemStateReceiver, IntentFilter().apply {
        addAction(Intent.ACTION_SCREEN_ON)
        addAction(Intent.ACTION_USER_PRESENT) // Sent when the keyguard is gone/device is unlocked
        addAction(Intent.ACTION_SCREEN_OFF)
        addAction(Intent.ACTION_POWER_CONNECTED)
        addAction(Intent.ACTION_POWER_DISCONNECTED)
        addAction(Intent.ACTION_BATTERY_LOW)
        addAction(Intent.ACTION_BATTERY_OKAY)

        addAction(Intent.ACTION_REBOOT)
        addAction(Intent.ACTION_BOOT_COMPLETED)

        addAction(Intent.ACTION_LOCALE_CHANGED)
        addAction(Intent.ACTION_DATE_CHANGED)
        addAction(Intent.ACTION_TIME_CHANGED)
        addAction(Intent.ACTION_TIMEZONE_CHANGED)

        addAction(Intent.ACTION_HEADSET_PLUG)
        addAction(CalendarContract.ACTION_EVENT_REMINDER)
        addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)
        addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
        addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED)
    })
}