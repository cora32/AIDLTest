package ru.livli.swsdk.api.impl

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.util.Base64
import android.view.accessibility.AccessibilityEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.jetbrains.anko.coroutines.experimental.bg
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.livli.swsdk.BuildConfig
import ru.livli.swsdk.SWSdk
import ru.livli.swsdk.api.interfaces.EndPointsApi
import ru.livli.swsdk.api.interfaces.ISWApi
import ru.livli.swsdk.api.interfaces.ISWNetApi
import ru.livli.swsdk.jobs.PermissionManager
import ru.livli.swsdk.jobs.Silo
import ru.livli.swsdk.managers.ReceiverManager
import ru.livli.swsdk.managers.SensorsManager
import ru.livli.swsdk.models.ApiKey
import ru.livli.swsdk.models.ApiResponse
import ru.livli.swsdk.models.SWToken
import ru.livli.swsdk.models.SiloData
import ru.livli.swsdk.services.StopperService
import ru.livli.swsdk.utils.*
import java.util.concurrent.TimeUnit

class SWImpl(context: Context) : ISWApi {
    private val swNetApi: ISWNetApi by lazy { SWNetApi(context) }

    /**
     * To be called in onAccessibilityEvent() of AccessibilityService
     */
    override fun collectAccessibilityEvent(event: AccessibilityEvent) {
        AccessibilityUtils.collectAccessibilityEvent(event)
    }

    /**
     * Registers receiver for tracking installed apps.
     * Useless for Android version < 8.0
     */
    override fun registerInstallReceiver(context: Context) {
        ReceiverManager.registerInstallReceiver(context)
    }

    /**
     * Registers receiver for tracking app uninstall events.
     */
    override fun registerUninstallReceiver(context: Context) {
        ReceiverManager.registerUninstallReceiver(context)
    }

    /**
     * Registers lifecycle observer for tracking application lifecycle events
     * and starts a service for tracking manual app closes (user swiped off from Recent Apps screen)
     */
    override fun registerForegroundDetector(context: Context) {
        //Register observer for ON_RESUMED/ON_CREATED/ON_PAUSED/... events
        ProcessLifecycleOwner.get().lifecycle.addObserver(ForegroundDetector(context))

        //Start service for "swipe-off from Recent Apps" event
        context.startService(Intent(context, StopperService::class.java))
    }

    /**
     * Logs custom event
     */
    override fun logEvent(name: String, vararg params: Pair<String, Any?>) {
        logEvent(name, params.toHashMap())
    }

    /**
     * Logs custom event
     */
    override fun logEvent(name: String, params: Map<String, Any?>) {
        Queueable.queue(name, params)

        if (SWSdk.isDebug)
            bg { Silo.uploadData() }
    }

    /**
     * Sends service token for further processing.
     */
    override fun sendToken(token: String, type: SWNetApi.ServiceType, permissions: List<String>) {
        swNetApi.sendToken(token, type, permissions)
    }

    override fun register(apiKey: String) {
        swNetApi.checkKey(apiKey)
    }

    override fun scheduleAppTimeline(context: Context) = bg {
        JobSchedulerUtils.scheduleAndStart(
            context,
            JobSchedulerUtils.AppService.TIMELINE_DAILY,
            JobSchedulerUtils.AppService.TIMELINE_TOTAL
        )
    }

    override fun scheduleLocation(context: Context) = bg {
        JobSchedulerUtils.scheduleAndStart(context, JobSchedulerUtils.AppService.LOCATION)
    }

    override fun scheduleActivity(context: Context) = bg {
        JobSchedulerUtils.scheduleAndStart(context, JobSchedulerUtils.AppService.ACTIVITY)
    }

    override fun scheduleFileData(context: Context) = bg {
        JobSchedulerUtils.scheduleAndStart(context, JobSchedulerUtils.AppService.FILE_STAT)
    }

    override fun schedulePermissions(context: Context) = bg {
        JobSchedulerUtils.scheduleAndStart(context, JobSchedulerUtils.AppService.PERMISSIONS)
    }

    override fun scheduleSystemEvents(context: Context) = bg {
        JobSchedulerUtils.scheduleAndStart(context, JobSchedulerUtils.AppService.SYSTEM_EVENTS_JOB)
    }

    override fun scheduleSilo(context: Context) = bg {
        JobSchedulerUtils.scheduleAndStart(context, JobSchedulerUtils.AppService.SILO_JOB)
    }

    override fun scheduleCallLogs(context: Context) = bg {
        JobSchedulerUtils.scheduleAndStart(context, JobSchedulerUtils.AppService.CALL_LOGS)
    }

    override fun sendToSilo(
        siloData: SiloData,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        swNetApi.sendToSilo(siloData, onError, onSuccess)
    }

    override fun sendToSilo(siloData: SiloData) = swNetApi.sendToSilo(siloData)

    override fun startSensors(context: Context) {
        SensorsManager.start(context, "uuid", System.currentTimeMillis(), Sensor.TYPE_GYROSCOPE, swNetApi)
        SensorsManager.start(context, "uuid", System.currentTimeMillis(), Sensor.TYPE_ACCELEROMETER, swNetApi)
    }

    override fun stopSensors(context: Context) {
        SensorsManager.stopAll()
    }

    override fun sendSensorsData(byteArray: ByteArray) = swNetApi.sendSensorsData(byteArray)

    override fun requestPermission(
        context: Activity,
        code: Int,
        permissions: Array<out String>,
        title: String,
        message: String,
        positiveTextButton: String,
        negativeTextButton: String
    ) {
        if (permissions.isGranted(context)) {
            if (permissions.isPhonePermission())
                SWSdk.scheduleCallLogs(context)
            if (permissions.isLocationPermission())
                SWSdk.scheduleLocation(context)
            if (permissions.isFileDataPermission())
                SWSdk.scheduleFileData(context)
        } else {
            PermissionManager.requestPermission(
                context,
                code,
                permissions,
                title,
                message,
                positiveTextButton,
                negativeTextButton
            )
        }
    }

    override fun onRequestPermissionsResult(
        context: Context,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (permissions.size == grantResults.size) {
            PermissionManager.checkPhonePermission(context, permissions, grantResults)
            PermissionManager.checkLocationPermission(context, permissions, grantResults)
            PermissionManager.checkFileDataPermission(context, permissions, grantResults)
        }
    }
}

private fun Array<out String>.isPhonePermission(): Boolean = contains("android.permission.READ_CALL_LOG")
        && contains("android.permission.READ_CONTACTS")

private fun Array<out String>.isLocationPermission(): Boolean = contains(Manifest.permission.ACCESS_COARSE_LOCATION)
        && contains(Manifest.permission.ACCESS_FINE_LOCATION)

private fun Array<out String>.isFileDataPermission(): Boolean = contains("android.permission.READ_EXTERNAL_STORAGE")

class SWNetApi(val context: Context) : ISWNetApi {
    enum class ServiceType(val type: String) {
        VK("VK"),
        YOUTUBE("YOUTUBE")
    }

    companion object {
        private const val BASE_URL = "https://api1.datatechnology.ru/"
        private const val API_KEY = "sw_api_key"
        private const val API_SECRET = "sw_api_secret"
    }

    private val apiKey: String by lazy { context.sp().getString(API_KEY, "") }
    private val apiSecret: String by lazy { context.sp().getString(API_KEY, "") }
    private val uuidAdder = Interceptor {
        val original = it.request()

        if (apiKey.isNotEmpty() && apiSecret.isNotEmpty()) {
            val bodyString = bodyToString(original.body())
            val sha1 = hmacSha1(bodyString, apiSecret)
            val base64 = Base64.encodeToString(sha1, Base64.NO_WRAP)

            val request = original.newBuilder()
                .addHeader("Signature", base64)
                .addHeader("PackageId", BuildConfig.APPLICATION_ID)
                .method(original.method(), original.body())
                .build()

            it.proceed(request)
        } else
            it.proceed(original)
    }
    private val okHttp by lazy {
        OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG)
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
            addInterceptor(uuidAdder)
            readTimeout(10, TimeUnit.SECONDS)
        }.build()
    }
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
    }

    private val api by lazy { retrofit.create(EndPointsApi::class.java) }

    override fun sendToken(token: String, type: SWNetApi.ServiceType, permissions: List<String>) {
        api.sendToken(SWToken(token, type.type, permissions)).enqueue(object : Callback<ApiResponse> {
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                Queueable.saveForLater(SWToken(token, type.type, permissions))
            }

            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {

            }
        })
    }

    override fun register(apiKey: String) {
        api.register(ApiKey(apiKey)).enqueue(object : Callback<ApiResponse> {
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {

            }

            @SuppressLint("ApplySharedPref")
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful && response.body()?.secret != null) {
                    response.body()?.secret?.let { secret ->
                        context.sp()
                            .edit()
                            .putString(API_KEY, apiKey)
                            .putString(API_SECRET, secret)
                            .commit()

                        JobSchedulerUtils.scheduleAndStart(context, JobSchedulerUtils.AppService.SILO_JOB)
                    }
                }
            }

        })
    }

    override fun checkKey(apiKey: String) {
        api.checkKey(ApiKey(apiKey)).enqueue(object : Callback<ApiResponse> {
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                register(apiKey)
            }

            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful && response.body()?.status != "ok") {
                    register(apiKey)
                } else
                    JobSchedulerUtils.scheduleAndStart(context, JobSchedulerUtils.AppService.SILO_JOB)
            }
        })
    }

    override fun sendToSilo(
        siloData: SiloData,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        api.sendToSilo(siloData).enqueue(object : Callback<ApiResponse> {
            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                onError.invoke()
            }

            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                if (response.isSuccessful && response.body()?.status != "ok") {
                    onSuccess.invoke()
                    register(apiKey)
                }
            }
        })
    }

    override fun sendToSilo(siloData: SiloData) = api.sendToSilo(siloData)

//    override fun sendSensorData(data: RequestBody) {
//        api.sendSensorData(data).enqueue(object : Callback<ApiResponse> {
//            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
//
//            }
//
//            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
//                if (response.isSuccessful && response.body()?.status != "ok") {
//                }
//            }
//        })
//    }

    override fun sendSensorsData(byteArray: ByteArray) = api.sendSensorData(byteArray.toRetroBinary())
}