package ru.livli.swsdk.jobs

import android.annotation.SuppressLint
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateUtils
import androidx.annotation.RequiresApi
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import org.jetbrains.anko.coroutines.experimental.bg
import ru.livli.swsdk.jobs.TimelineManager.TIMELINE_LAST_FETCHED
import ru.livli.swsdk.jobs.TimelineManager.TIMELINE_LAST_FETCHED_TOTAL
import ru.livli.swsdk.models.AppInfoData
import ru.livli.swsdk.models.TimelineDailyData
import ru.livli.swsdk.models.TimelinePerYearsData
import ru.livli.swsdk.utils.Queueable
import ru.livli.swsdk.utils.isAppUsageAccessGranted
import ru.livli.swsdk.utils.sp

internal class TimelineJob : JobService() {
    var lastTimeFetched
        get() = applicationContext.sp().getLong(TIMELINE_LAST_FETCHED_TOTAL, 0L)
        set(value) = applicationContext.sp().edit().putLong(TIMELINE_LAST_FETCHED_TOTAL, value).apply()

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        bg {
            applicationContext?.let {
                TimelineManager.saveInstalledApps(it)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (it.isAppUsageAccessGranted()) {
                        val now = System.currentTimeMillis()
                        if (now > lastTimeFetched + DateUtils.WEEK_IN_MILLIS) {
                            TimelineManager.saveTimelinePerYear(lastTimeFetched, it)
                            lastTimeFetched = now
                        }
                    }
                }
            }
        }.invokeOnCompletion {
            jobFinished(jobParameters, true)
        }
        return false
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean = false
}

internal class TimelineDailyJob : JobService() {
    var lastTimeFetched
        get() = applicationContext.sp().getLong(TIMELINE_LAST_FETCHED, 0L)
        set(value) = applicationContext.sp().edit().putLong(TIMELINE_LAST_FETCHED, value).apply()

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        bg {
            applicationContext?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if (it.isAppUsageAccessGranted()) {
                        val now = System.currentTimeMillis()
                        TimelineManager.saveTimelineDaily(lastTimeFetched, it)
                        lastTimeFetched = now
                    }
                }
            }
        }.invokeOnCompletion {
            jobFinished(jobParameters, true)
        }
        return false
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean = false
}

internal object TimelineManager {
    const val TIMELINE_LAST_FETCHED = "TIMELINE_LAST_FETCHED"
    const val TIMELINE_LAST_FETCHED_TOTAL = "TIMELINE_LAST_FETCHED_TOTAL"
    private const val APPINFO = "app_info"
    private const val TIMELINE_WEEK = "timeline_week"
    private const val TIMELINE_TOTAL = "timeline_total"
    private const val REMOVED = "[APP_WAS_REMOVED]"

    fun saveInstalledApps(context: Context) {
        val pm = context.packageManager
        with(arrayListOf<AppInfoData>()) {
            pm.getInstalledApplications(PackageManager.GET_META_DATA).forEach {
                it.let {
                    add(
                        AppInfoData(
                            it.loadLabel(pm).toString(),
                            it.packageName,
                            (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        )
                    )
                }
            }

            if (isNotEmpty())
                Queueable.queue(APPINFO, this)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressWarnings("WrongConstant")
    fun saveTimelineDaily(startFrom: Long = 0, context: Context) {
        val pm = context.packageManager
        val usageManager = context.getSystemService("usagestats") as UsageStatsManager
        with(usageManager.queryEvents(startFrom, System.currentTimeMillis())) {
            val event = UsageEvents.Event()
            var startTime = 0L
            var endTime = 0L
            val data = arrayListOf<TimelineDailyData>()
            while (hasNextEvent()) {
                // Fill event data
                getNextEvent(event)

//                if(event.packageName == "io.humanteq.df2") {
                if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    startTime = event.timeStamp
                } else if (event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                    endTime = event.timeStamp

                    val name = try {
                        pm.getApplicationInfo(event.packageName, 0).loadLabel(pm).toString()
                    } catch (ex: PackageManager.NameNotFoundException) {
                        REMOVED
                    }
                    data.add(
                        TimelineDailyData(
                            name,
                            event.packageName,
                            startTime,
                            endTime - startTime
                        )
                    )
                }
            }

            if (data.isNotEmpty())
                Queueable.queue(TIMELINE_WEEK, data)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("WrongConstant")
    fun saveTimelinePerYear(startFrom: Long = 0, context: Context) {
        val usageManager = context.getSystemService("usagestats") as UsageStatsManager
        val pm = context.packageManager
        with(arrayListOf<TimelinePerYearsData>()) {
            usageManager.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, startFrom, System.currentTimeMillis())
                .groupBy { it.packageName }
                .forEach { (name, usageStatsList) ->
                    var totalTime = 0L
                    usageStatsList.forEach { totalTime += it.totalTimeInForeground }
                    val first = usageStatsList.first()
                    val last = usageStatsList.last()
                    val firstTimeStamp = first.firstTimeStamp
                    val lastTimeStamp = last.lastTimeStamp
                    val lastTimeUsed = last.lastTimeUsed
                    val label = try {
                        pm.getApplicationInfo(name, 0).loadLabel(pm).toString()
                    } catch (ex: PackageManager.NameNotFoundException) {
                        REMOVED
                    }
                    val firstInstallTime = try {
                        pm.getPackageInfo(name, 0).firstInstallTime
                    } catch (ex: Exception) {
                        -1L
                    }

                    add(
                        TimelinePerYearsData(
                            label,
                            name,
                            firstTimeStamp,
                            lastTimeStamp,
                            lastTimeUsed,
                            totalTime,
                            firstInstallTime
                        )
                    )
                }
            if (isNotEmpty())
                Queueable.queue(TIMELINE_TOTAL, this)
        }
    }
}