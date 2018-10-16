package ru.livli.swsdk

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.view.accessibility.AccessibilityEvent
import ru.livli.swsdk.api.impl.SWImpl
import ru.livli.swsdk.api.impl.SWNetApi
import ru.livli.swsdk.models.SiloData
import ru.livli.swsdk.utils.Queueable
import ru.livli.swsdk.utils.error


class SWSdk {
    companion object {
        const val NONE = -1
        const val LIFECYCLE = 0
        const val INSTALLS = 1
        const val UNINSTALLS = 2
        const val SYSTEM_EVENTS = 3
        const val FILE_DATA = 4
        const val APP_USAGE = 5
        const val ACTIVITY = 6
        const val LOCATION = 7
        const val PHONE_PERMISSION_CODE = 8
        const val LOCATION_CODE = 9

        @Volatile
        private var instance: SWImpl? = null

        var isDebug = false

        fun getInstance(context: Context, apiKey: String, tasks: Int = NONE): SWImpl =
            instance ?: synchronized(this) {
                instance ?: buildInstance(context, apiKey).also {
                    instance = it
                    Queueable.getInstance(context)

                    when (tasks) {
                        LIFECYCLE -> instance?.registerForegroundDetector(context)
                        INSTALLS -> instance?.registerInstallReceiver(context)
                        UNINSTALLS -> instance?.registerUninstallReceiver(context)
                        SYSTEM_EVENTS -> instance?.scheduleSystemEvents(context)
                        FILE_DATA -> instance?.scheduleFileData(context)
                        APP_USAGE -> instance?.scheduleAppTimeline(context)
                        ACTIVITY -> instance?.scheduleActivity(context)
                        LOCATION -> instance?.scheduleLocation(context)
                    }

                    instance?.scheduleSilo(context)

//                        "---- ICom bane: ${ICom::class.java.name}".error
                    val implicit = Intent(ICom::class.java.name)
//                        val matches = context.packageManager
//                                .queryIntentServices(implicit, 0)
//                        "---- SIZE: ${matches.size} $matches".error
//
                    val explicit = Intent(implicit)
                    var binding: ICom? = null
                    val sConn = object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                            "--- SWSDK onServiceConnected $name".error
//                val b = binder as IMyBinder
//                b.test()
                            binding = ICom.Stub.asInterface(binder)
                            binding?.test()
                            val uuid = binding?.getUuid()
                            "--- recvd uuid: $uuid".error
                        }

                        override fun onServiceDisconnected(name: ComponentName) {
                            "--- SWSDK onServiceDisconnected $name".error
                        }
                    }
//                        if(matches.isNotEmpty()) {
//                            val svcInfo = matches[0].serviceInfo
//                            val cn = ComponentName(svcInfo.applicationInfo.packageName, svcInfo.name)
//                            explicit.component = cn
//
//                            context.bindService(explicit, sConn, Context.BIND_AUTO_CREATE)
//                        } else {
//                            "--- starting: ${context.packageName} -- ${AnalyticsService::class.java.name}".error
//                            explicit.component = ComponentName(context.packageName, AnalyticsService::class.java.name)
//                            context.startService(explicit)
//                            context.bindService(explicit, sConn, Context.BIND_AUTO_CREATE)
//                        }


                    val implicit2 = Intent(ICom::class.java.name)
                    val matches2 = context.packageManager
                        .queryIntentServices(implicit2, 0)
                    val matches3 = context.packageManager
                        .resolveService(Intent("ru.livli.swsdk.AnalyticsService"), 0)
                    "---- SEC SIZE: ${matches2.size} $matches2 $matches3".error

                    val cn = ComponentName(matches3.serviceInfo.packageName, matches3.serviceInfo.name)
                    explicit.component = cn

                    context.bindService(explicit, sConn, Context.BIND_AUTO_CREATE)


                    val sp = context.getSharedPreferences("SWSDK_SP", Context.MODE_PRIVATE)
//                        uuid = UUID.randomUUID().toString()
                    val uuid = sp.getString("UUID", "")
                    "--- uuid test: $uuid".error
                }
            }

        private fun buildInstance(context: Context, apiKey: String) = SWImpl(context).also { it.register(apiKey) }

        fun sendToken(token: String, type: SWNetApi.ServiceType, permissions: List<String>) =
            instance?.sendToken(token, type, permissions)

        fun logEvent(name: String, vararg params: Pair<String, Any?>) = instance?.logEvent(name, *params)
        fun logEvent(name: String, params: Map<String, Any?>) = instance?.logEvent(name, params)
        fun scheduleAppTimeline(context: Context) = instance?.scheduleAppTimeline(context)
        fun scheduleLocation(context: Context) = instance?.scheduleLocation(context)
        fun scheduleActivity(context: Context) = instance?.scheduleActivity(context)
        fun scheduleFileData(context: Context) = instance?.scheduleFileData(context)
        fun schedulePermissions(context: Context) = instance?.schedulePermissions(context)
        fun scheduleSystemEvents(context: Context) = instance?.scheduleSystemEvents(context)
        fun scheduleCallLogs(context: Context) = instance?.scheduleCallLogs(context)
        fun sendToSilo(
            siloData: SiloData,
            onSuccess: () -> Unit,
            onError: () -> Unit
        ) = instance?.sendToSilo(siloData, onSuccess, onError)

        fun sendToSilo(siloData: SiloData) = instance?.sendToSilo(siloData)
        fun collectAccessibilityEvent(event: AccessibilityEvent) = instance?.collectAccessibilityEvent(event)
        fun startSensors(context: Context) = instance?.startSensors(context)
        fun stopSensors(context: Context) = instance?.stopSensors(context)
        fun sendSensorsData(byteArray: ByteArray) = instance?.sendSensorsData(byteArray)
        fun requestPermission(
            context: Activity,
            code: Int,
            title: String = context.getString(R.string.permission_request_title),
            message: String = context.getString(R.string.permission_request_msg),
            positiveTextButton: String = context.getString(R.string.ok),
            negativeTextButton: String = context.getString(R.string.no),
            vararg permissions: String
        ) = instance?.requestPermission(
            context,
            code,
            permissions,
            title,
            message,
            positiveTextButton,
            negativeTextButton
        )

        fun onRequestPermissionsResult(
            context: Context,
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) = instance?.onRequestPermissionsResult(
            context,
            requestCode,
            permissions,
            grantResults
        )
    }
}