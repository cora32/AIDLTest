package ru.livli.swsdk.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import ru.livli.swsdk.ICom
import ru.livli.swsdk.utils.error

class AnalyticsService : Service() {
    override fun onDestroy() {
        super.onDestroy()
        "--- onDestroy".error
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        "--- onStartCommand".error
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        "--- onCreate".error
        super.onCreate()
    }

    override fun onBind(intent: Intent?): IBinder? {
        "--- onBind".error
        return ComImpl(applicationContext)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        "--- onUnbind".error
        return super.onUnbind(intent)
    }

    override fun onRebind(intent: Intent?) {
        "--- onRebind".error
        super.onRebind(intent)
    }

    class ComImpl(val context: Context) : ICom.Stub() {
        override fun basicTypes(
            anInt: Int,
            aLong: Long,
            aBoolean: Boolean,
            aFloat: Float,
            aDouble: Double,
            aString: String?
        ) {

        }

        override fun test() {
            "--- 2 TEST".error
        }

        override fun getUuid(): String {
//            val uuid = UUID.randomUUID().toString()
//            "--- 2 uuid".error


            val sp = context.getSharedPreferences("SWSDK_SP", Context.MODE_PRIVATE)
//                        uuid = UUID.randomUUID().toString()
            val uuid = sp.getString("UUID", "")
            "--- external app uuid: $uuid".error
            return "none"
        }

    }
}