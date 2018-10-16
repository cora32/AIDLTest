package ru.livli.swsdk.jobs

import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import org.jetbrains.anko.coroutines.experimental.bg
import ru.livli.swsdk.utils.GoogleClient

internal class LocationJob : JobService() {
    override fun onStartJob(jobParameters: JobParameters): Boolean {
        bg {
            GoogleClient.trackLocation(applicationContext)
        }.invokeOnCompletion {
            jobFinished(jobParameters, true)
        }
        return false
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean = false
}
