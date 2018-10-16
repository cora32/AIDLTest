package ru.livli.swsdk.jobs

import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import ru.livli.swsdk.utils.error

class CallsJob : JobService() {
    override fun onStopJob(job: JobParameters?): Boolean {
        "--- startining CallsJob".error
        return false
    }

    override fun onStartJob(job: JobParameters?): Boolean {
        return false
    }
}