package ru.livli.swsdk.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Looper
import com.google.android.gms.location.*
import ru.livli.swsdk.models.GoogleActivity
import ru.livli.swsdk.models.GoogleLocation


internal class GoogleClient {
    companion object {
        const val LOCATION = "location"
        @JvmStatic
        @SuppressLint("MissingPermission")
        fun trackLocation(context: Context) {
            if (context.hasPermissions(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                val currentLocationRequest = LocationRequest()
                currentLocationRequest.setInterval(1000)
                    .setFastestInterval(1000)
                    .setMaxWaitTime(0)
                    .setSmallestDisplacement(0f).priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                val client = LocationServices.getFusedLocationProviderClient(context)
                client.requestLocationUpdates(currentLocationRequest, object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        Queueable.queue(
                            LOCATION, GoogleLocation(
                                result.lastLocation.latitude,
                                result.lastLocation.longitude,
                                result.lastLocation.altitude,
                                result.lastLocation.accuracy,
                                result.lastLocation.speed
                            )
                        )
                        client.removeLocationUpdates(this)
                    }
                }, Looper.getMainLooper())
            }
        }

        @SuppressLint("MissingPermission")
        @JvmStatic
        fun trackActivity(context: Context) {
            with(ActivityRecognition.getClient(context)) {
                val pIntent = PendingIntent.getService(
                    context, 0,
                    Intent(context, GoogleActivityService::class.java), 0
                )
                requestActivityTransitionUpdates(
                    ActivityTransitionRequest(getAllTransitions()),
                    pIntent
                )
            }
        }

        private fun getAllTransitions(): List<ActivityTransition> {
            val activityTransitionList = arrayListOf<ActivityTransition>()

            activityTransitionList.add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.IN_VEHICLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )
            activityTransitionList.add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.IN_VEHICLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
            activityTransitionList.add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.ON_BICYCLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )
            activityTransitionList.add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.ON_BICYCLE)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
            activityTransitionList.add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.ON_FOOT)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )
            activityTransitionList.add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.ON_FOOT)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
            activityTransitionList.add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.STILL)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )
            activityTransitionList.add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.STILL)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
            activityTransitionList.add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )
            activityTransitionList.add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.WALKING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )
            activityTransitionList.add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.RUNNING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                    .build()
            )
            activityTransitionList.add(
                ActivityTransition.Builder()
                    .setActivityType(DetectedActivity.RUNNING)
                    .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                    .build()
            )

            return activityTransitionList
        }
    }
}

class GoogleActivityService : IntentService("Api service") {
    companion object {
        const val ACTIVITY_LIST = "activity_list"
    }

    override fun onHandleIntent(intent: Intent?) {
        intent?.let {
            if (ActivityTransitionResult.hasResult(it)) {
                with(arrayListOf<Any>()) {
                    ActivityTransitionResult.extractResult(it)
                        ?.transitionEvents
                        ?.forEach {
                            add(
                                GoogleActivity(
                                    it.activityType.decodeActivity(),
                                    it.transitionType.decodeTransition(),
                                    it.elapsedRealTimeNanos
                                )
                            )
                        }
                    Queueable.queue(ACTIVITY_LIST, this)
                }
            }
        }
    }
}

private fun Int.decodeActivity(): String = when (this) {
    DetectedActivity.IN_VEHICLE -> "IN_VEHICLE"
    DetectedActivity.ON_BICYCLE -> "ON_BICYCLE"
    DetectedActivity.ON_FOOT -> "ON_FOOT"
    DetectedActivity.STILL -> "STILL"
    DetectedActivity.WALKING -> "WALKING"
    DetectedActivity.RUNNING -> "RUNNING"
    DetectedActivity.UNKNOWN -> "UNKNOWN"
    DetectedActivity.TILTING -> "TILTING"
    else -> "UNKNOWN"
}

private fun Int.decodeTransition(): String = when (this) {
    ActivityTransition.ACTIVITY_TRANSITION_ENTER -> "ACTIVITY_TRANSITION_ENTER"
    ActivityTransition.ACTIVITY_TRANSITION_EXIT -> "ACTIVITY_TRANSITION_EXIT"
    else -> "UNKNOWN"
}
