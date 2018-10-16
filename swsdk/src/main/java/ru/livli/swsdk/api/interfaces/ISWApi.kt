package ru.livli.swsdk.api.interfaces

import android.app.Activity
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.experimental.Deferred
import org.jetbrains.annotations.NotNull
import retrofit2.Call
import ru.livli.swsdk.api.impl.SWNetApi
import ru.livli.swsdk.models.ApiResponse
import ru.livli.swsdk.models.SiloData

internal interface ISWApi {
    fun logEvent(@NotNull name: String, vararg params: Pair<String, Any?>)
    fun logEvent(@NotNull name: String, params: Map<String, Any?>)
    fun sendToken(@NotNull token: String, @NotNull type: SWNetApi.ServiceType, @NotNull permissions: List<String>)
    fun register(@NotNull apiKey: String)
    fun scheduleAppTimeline(context: Context): Deferred<Unit>
    fun scheduleLocation(context: Context): Deferred<Unit>
    fun scheduleActivity(context: Context): Deferred<Unit>
    fun scheduleFileData(context: Context): Deferred<Unit>
    fun schedulePermissions(context: Context): Deferred<Unit>
    fun scheduleSystemEvents(context: Context): Deferred<Unit>
    fun scheduleSilo(context: Context): Deferred<Unit>
    fun scheduleCallLogs(context: Context): Deferred<Unit>
    fun sendToSilo(
        siloData: SiloData,
        onSuccess: () -> Unit,
        onError: () -> Unit
    )

    fun sendToSilo(siloData: SiloData): Call<ApiResponse>
    fun registerForegroundDetector(context: Context)
    fun registerInstallReceiver(context: Context)
    fun registerUninstallReceiver(context: Context)
    fun collectAccessibilityEvent(event: AccessibilityEvent)
    fun startSensors(context: Context)
    fun stopSensors(context: Context)
    fun sendSensorsData(byteArray: ByteArray): Call<ApiResponse>
    fun requestPermission(
        context: Activity,
        code: Int,
        permissions: Array<out String>,
        title: String,
        message: String,
        positiveTextButton: String,
        negativeTextButton: String
    )

    fun onRequestPermissionsResult(
        context: Context,
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    )
}