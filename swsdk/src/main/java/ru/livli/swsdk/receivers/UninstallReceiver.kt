package ru.livli.swsdk.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ru.livli.swsdk.models.RemovedEvent
import ru.livli.swsdk.utils.Queueable


internal class UninstallReceiver : BroadcastReceiver() {
    private val REMOVED_EVENT = "REMOVED_EVENT"

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            if (it.action == Intent.ACTION_PACKAGE_FULLY_REMOVED)
                Queueable.queue(REMOVED_EVENT, RemovedEvent("${intent.dataString}"))
        }
    }
}