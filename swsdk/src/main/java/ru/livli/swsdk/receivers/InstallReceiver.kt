package ru.livli.swsdk.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ru.livli.swsdk.utils.Queueable
import ru.livli.swsdk.utils.error

internal class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        //This is not an option for 8.0+. Still should work for < 8.0
        //todo: test for < 8.0
        "--- install  rec".error
        intent?.let {
            val packageName = it.data?.encodedSchemeSpecificPart
            "--- install  rec $packageName".error

            it.extras?.let {
                val referrer = it.get("referrer")
                referrer?.let {
                    //                    Firebase.logCustomEvent("referrer", "referrer" to it)
                    Queueable.queue("referrer", "referrer" to it)
                }

                if (referrer != null) {
                    val data = hashMapOf<String, String?>()
                    referrer.toString().split("&").forEach {
                        val temp = it.split("=")
                        if (temp.size == 2)
                            data[temp[0]] = temp[1]
                    }

//                    val uidRef = data["uid"]
//                    val secretRef = data["secret"]
//                    if (uidRef != null && secretRef != null) {
//                        val uidStr = uidRef.toString()
//                        val secretStr = secretRef.toString()
//                        if (!uidStr.isEmpty() && !secretStr.isEmpty()) {
//                            uid = uidStr
//                            secret = secretStr
//                        }
//                    }
                }
            }
        }
    }
}
