package ru.livli.swsdk.jobs

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AlertDialog
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import org.jetbrains.anko.coroutines.experimental.bg
import ru.livli.swsdk.R
import ru.livli.swsdk.SWSdk
import ru.livli.swsdk.models.ActivePermission
import ru.livli.swsdk.utils.Queueable
import ru.livli.swsdk.utils.canReadStorage
import ru.livli.swsdk.utils.isAppUsageAccessGranted
import ru.livli.swsdk.utils.requestPermission

internal class PermissionsJob : JobService() {
    override fun onStartJob(jobParameters: JobParameters): Boolean {
        bg {
            PermissionManager.saveActivePermissions(applicationContext)
        }.invokeOnCompletion {
            jobFinished(jobParameters, true)
        }
        return false
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean = false
}

internal object PermissionManager {
    private const val ACTIVE_PERMISSIONS = "activePermissions"

    fun saveActivePermissions(context: Context?) {
        context?.let {
            with(arrayListOf<ActivePermission>()) {
                add(it.checkActivePermission(Manifest.permission.ACCESS_FINE_LOCATION))
                add(it.checkActivePermission(Manifest.permission.ACCESS_COARSE_LOCATION))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    add(it.checkActivePermission(Manifest.permission.READ_CALL_LOG))
                }
                add(it.checkActivePermission(Manifest.permission.READ_CONTACTS))
//                add(it.checkActiveAccessibilityPermission())
                add(it.checkActiveAppUsagePermission())
//                add(it.checkYoutubePermission())
                add(it.checkExternalStoragePermission())

                Queueable.queue(ACTIVE_PERMISSIONS, this)
            }
        }
    }

    fun requestPermission(
        context: Activity,
        code: Int,
        permissions: Array<out String>,
        title: String = "",
        message: String = "",
        positiveButtonText: String = context.getString(R.string.ok),
        negativeButtonText: String = context.getString(R.string.no)
    ) {
        with(AlertDialog.Builder(context)
            .setPositiveButton(positiveButtonText) { dialogInterface, i ->
                permissions.requestPermission(context, code)
            }
            .setNegativeButton(negativeButtonText) { dialogInterface, i -> }
            .create()) {
            setTitle(title)
            setMessage(message)
            show()
        }
    }

    fun checkPhonePermission(
        context: Context,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val firstIndex = permissions.indexOf("android.permission.READ_CALL_LOG")
        val secondIndex = permissions.indexOf("android.permission.READ_CONTACTS")

        if (firstIndex != -1 && secondIndex != -1 && grantResults[firstIndex] == 0 && grantResults[secondIndex] == 0)
            SWSdk.scheduleCallLogs(context)
    }

    fun checkLocationPermission(
        context: Context,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val firstIndex = permissions.indexOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        val secondIndex = permissions.indexOf(Manifest.permission.ACCESS_FINE_LOCATION)

        if (firstIndex != -1 && secondIndex != -1 && grantResults[firstIndex] == 0 && grantResults[secondIndex] == 0)
            SWSdk.scheduleLocation(context)
    }

    fun checkFileDataPermission(
        context: Context,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val firstIndex = permissions.indexOf("android.permission.READ_EXTERNAL_STORAGE")

        if (firstIndex != -1 && grantResults[firstIndex] == 0)
            SWSdk.scheduleFileData(context)
    }
}

//private fun Context.checkYoutubePermission(): ActivePermission =
//        ActivePermission("YoutubePermission", YTHelper.checkPermission(this))

private fun Context.checkExternalStoragePermission(): ActivePermission =
    ActivePermission("ReadExternalStoragePermission", canReadStorage())

private fun Context.checkActiveAppUsagePermission(): ActivePermission =
    ActivePermission("AppUsage", isAppUsageAccessGranted())

//private fun Context.checkActiveAccessibilityPermission(): ActivePermission = ActivePermission("AccessibilityPermission",
//        isAccessibilityEnabled(TelemetryAccessibilityService.getPath(this)))

private fun Context.checkActivePermission(permissionName: String): ActivePermission =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        ActivePermission(permissionName, checkSelfPermission(permissionName) == PackageManager.PERMISSION_GRANTED)
    else
        ActivePermission(permissionName, true)