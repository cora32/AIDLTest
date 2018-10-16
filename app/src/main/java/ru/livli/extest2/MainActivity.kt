package ru.livli.extest2

import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import ru.livli.swsdk.SWSdk

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val sConn = object : ServiceConnection {
//            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
//                "---2 MainActivity onServiceConnected $name".error
////                val b = binder as IMyBinder
////                b.test()
//            }
//
//            override fun onServiceDisconnected(name: ComponentName) {
//                "---2 MainActivity onServiceDisconnected".error
//            }
//        }
//        val intent = Intent("ru.livli.swsdk.AnalyticsService")
//        intent.setPackage("ru.livli.swsdk")
////        intent.setPackage(this.packageName)
////        intent.setPackage("swsdk.Service")
////        intent.setPackage("remote")
////        intent.setPackage("swsdkService")
////        intent.setPackage("ru.livli.extest2")
////        intent.setClassName("swsdk.Service", "ru.livli.extest1.Serv")
//        bindService(intent, sConn, BIND_AUTO_CREATE)


//        val intent = Intent("ru.livli.swsdk.AnalyticsService")
////                        intent.setPackage("ru.livli.swsdk")
////        intent.setPackage("io.humanteq.df2")
//        intent.setPackage(packageName)
////        startService(intent)
//        bindService(intent, sConn, AppCompatActivity.BIND_AUTO_CREATE)

//        val implicit = Intent(ICom::class.java.name)
//        val matches = context.packageManager
//            .queryIntentServices(implicit, 0)
//        "---- SIZE: ${matches.size} $matches".error
//
//        val explicit = Intent(implicit)
//        val sConn = object : ServiceConnection {
//            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
//                "--- SWSDK onServiceConnected $name".error
////                val b = binder as IMyBinder
////                b.test()
//            }
//
//            override fun onServiceDisconnected(name: ComponentName) {
//                "--- SWSDK onServiceDisconnected $name".error
//            }
//        }
//        if(matches.isNotEmpty()) {
//            val svcInfo = matches[0].serviceInfo
//            val cn = ComponentName(svcInfo.applicationInfo.packageName, svcInfo.name)
//            explicit.component = cn
//
//            context.bindService(explicit, sConn, Context.BIND_AUTO_CREATE)
//        }


        SWSdk.getInstance(this, "sw_api_key")
    }
}

interface IMyBinder : IBinder {
    fun test()
}
//
//class Serv : Service() {
//    override fun onDestroy() {
//        super.onDestroy()
//        "----2 onDestroy".error
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        "----2 onStartCommand".error
//        return super.onStartCommand(intent, flags, startId)
//    }
//
//    override fun onCreate() {
//        "----2 onCreate".error
//        super.onCreate()
//    }
//
//    override fun onBind(intent: Intent?): IBinder? {
//        "---2 onBind".error
//        return Binder()
//    }
//
//    override fun onUnbind(intent: Intent?): Boolean {
//        "---2 onUnbind".error
//        return super.onUnbind(intent)
//    }
//
//    override fun onRebind(intent: Intent?) {
//        "---2 onRebind".error
//        super.onRebind(intent)
//    }
//}

private val String.error: Unit
    get() {
        Log.e("---", this)
    }
