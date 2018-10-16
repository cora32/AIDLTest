package ru.livli.swsdk.models

import com.google.gson.annotations.SerializedName
import ru.livli.swsdk.BuildConfig

open class WithUtc(@SerializedName("utc") val utc: Long = System.currentTimeMillis())

internal data class AppInfoData(
    @SerializedName("name") val name: String = "",
    @SerializedName("packageName") val packageName: String = "",
    @SerializedName("isSystemApp") val isSystemApp: Boolean = false
) : WithUtc()

internal data class TimelineDailyData(
    @SerializedName("name") val name: String = "",
    @SerializedName("packageName") val packageName: String = "",
    @SerializedName("startTime") val startTime: Long = 0L,
    @SerializedName("totalTime") val totalTime: Long = 0L
) : WithUtc()

internal data class TimelinePerYearsData(
    @SerializedName("name") val name: String = "",
    @SerializedName("packageName") val packageName: String = "",
    @SerializedName("firstTimeStamp") val firstTimeStamp: Long = 0L,
    @SerializedName("lastTimeStamp") val lastTimeStamp: Long = 0L,
    @SerializedName("lastTimeUsed") val lastTimeUsed: Long = 0L,
    @SerializedName("totalTimeInForeground") val totalTimeInForeground: Long = 0L,
    @SerializedName("firstInstallTime") val firstInstallTime: Long = 0L
) : WithUtc()

internal data class GoogleActivity(
    @SerializedName("activityType") val activityType: String = "",
    @SerializedName("transitionType") val transitionType: String = "",
    @SerializedName("elapsedRealTimeNanos") val elapsedRealTimeNanos: Long = 0L
) : WithUtc()

internal data class GoogleLocation(
    @SerializedName("latitude") val latitude: Double = .0,
    @SerializedName("longtitude") val longtitude: Double = .0,// do NOT fix this typo.
    @SerializedName("altitude") val altitude: Double = .0,
    @SerializedName("accuracy") val accuracy: Float = 0f,
    @SerializedName("speed") val speed: Float = 0f
) : WithUtc()

internal data class ActivePermission(
    @SerializedName("name") val name: String = "",
    @SerializedName("isActive") val isActive: Boolean = false
) : WithUtc()

internal data class FileInfo(
    @SerializedName("type") val type: String,
    @SerializedName("name") val name: String,
    @SerializedName("size") val size: Long,
    @SerializedName("lastModified") val lastModified: Long,
    @SerializedName("copyRight") val copyRight: String,
    @SerializedName("dateTime") val dateTime: String,
    @SerializedName("description") val description: String,
    @SerializedName("make") val make: String,
    @SerializedName("model") val model: String,
    @SerializedName("software") val software: String,
    @SerializedName("artist") val artist: String,
    @SerializedName("exifVersion") val exifVersion: String,
    @SerializedName("makerNote") val makerNote: String,
    @SerializedName("userComment") val userComment: String,
    @SerializedName("dateOriginal") val dateOriginal: String,
    @SerializedName("dateDigitized") val dateDigitized: String,
    @SerializedName("subSec") val subSec: String,
    @SerializedName("subSecOrigin") val subSecOrigin: String,
    @SerializedName("subSecDigit") val subSecDigit: String,
    @SerializedName("subjectLocation") val subjectLocation: Int,
    @SerializedName("fileSource") val fileSource: String,
    @SerializedName("imageUniqueId") val imageUniqueId: String,
    @SerializedName("cameraOwnerName") val cameraOwnerName: String,
    @SerializedName("gpsLatRef") val gpsLatRef: String,
    @SerializedName("gpsLat") val gpsLat: Double,
    @SerializedName("gpsLongRef") val gpsLongRef: String,
    @SerializedName("gpsLong") val gpsLong: Double,
    @SerializedName("gpsAltRef") val gpsAltRef: Int,
    @SerializedName("gpsAlt") val gpsAlt: Double,
    @SerializedName("gpsTimestamp") val gpsTimestamp: Double,
    @SerializedName("orientation") val orientation: Int,
    @SerializedName("uniqueId") val uniqueId: String,
    @SerializedName("xResolution") val xResolution: Int,
    @SerializedName("yResolution") val yResolution: Int,
    @SerializedName("lensMake") val lensMake: String,
    @SerializedName("lensModel") val lensModel: String,
    @SerializedName("lensSerial") val lensSerial: String,
    @SerializedName("lensSpec") val lensSpec: Double,
    @SerializedName("gpsDirection") val gpsDirection: Double,
    @SerializedName("gpsDirectionRef") val gpsDirectionRef: String,
    @SerializedName("xDimension") val xDimension: Int,
    @SerializedName("yDimension") val yDimension: Int,
    @SerializedName("probableCamDirection") val probableCamDirection: String,
    @SerializedName("path") val path: String?
) : WithUtc()

internal data class SystemState(
    @SerializedName("action") val action: String,
    @SerializedName("data") val data: String?
) : WithUtc()


data class SiloData(
    @SerializedName("version") val version: String = BuildConfig.VERSION_NAME,
    @SerializedName("uuid") val uuid: String,
    @SerializedName("batch") val batch: Int = 0,
    @SerializedName("data") val data: Map<String, List<Any>> = mapOf()
) : WithUtc()

internal data class Time(@SerializedName("time") val time: Long) : WithUtc()

internal data class RemovedEvent(@SerializedName("name") val name: String = "") : WithUtc()

internal data class AccessibilityForSilo(
    @SerializedName("text") val text: String,
    @SerializedName("packageName") val packageName: String
) : WithUtc()

data class SavableByteArray(@SerializedName("byteArray") val byteArray: ByteArray? = null)

data class ApiKey(@SerializedName("apiKey") val apiKey: String)