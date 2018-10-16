package ru.livli.swsdk.managers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import ru.livli.swsdk.receivers.InstallReceiver
import ru.livli.swsdk.receivers.UninstallReceiver

internal object ReceiverManager {
    private var installReceiver: InstallReceiver? = null
    private var uninstallReceiver: UninstallReceiver? = null

    //todo: test for < 8.0
    fun registerInstallReceiver(context: Context) {
        try {
            if (installReceiver != null)
                context.unregisterReceiver(installReceiver)
        } catch (ex: Exception) {
        }

        installReceiver = InstallReceiver()
        context.registerReceiver(installReceiver, IntentFilter().apply {
            addAction("com.android.vending.INSTALL_REFERRER")
            addAction(Intent.ACTION_PACKAGE_INSTALL)
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
        })
    }

    fun registerUninstallReceiver(context: Context) {
        try {
            if (uninstallReceiver != null)
                context.unregisterReceiver(uninstallReceiver)
        } catch (ex: Exception) {
        }

        uninstallReceiver = UninstallReceiver()
        context.registerReceiver(uninstallReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
        })
    }

}