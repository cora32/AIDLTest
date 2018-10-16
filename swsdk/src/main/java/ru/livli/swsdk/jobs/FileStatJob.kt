package ru.livli.swsdk.jobs

import android.content.Context
import android.hardware.Camera
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import org.jetbrains.anko.coroutines.experimental.bg
import ru.livli.swsdk.models.FileInfo
import ru.livli.swsdk.utils.Queueable
import ru.livli.swsdk.utils.canReadStorage
import ru.livli.swsdk.utils.sp
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

internal class FileStatJob : JobService() {
    override fun onStartJob(jobParameters: JobParameters): Boolean {
        if (applicationContext.canReadStorage()) {
            bg {
                FileStatManager.start(applicationContext)
            }.invokeOnCompletion {
                jobFinished(jobParameters, true)
            }
        } else jobFinished(jobParameters, true)

        return false
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean = false
}


internal class FileStatManager(val context: Context) {
    companion object {
        const val ALL = "FILE_STATS_ALL"
        const val DOWNLOADS = "FILE_STATS_DOWNLOADS"
        const val LAST_FILE_SCAN_DATE = "LAST_FILE_SCAN_DATE"
        const val JPG = "jpg"
        const val JPEG = "jpeg"
        const val GIF = "gif"
        const val PNG = "png"
        const val WEBP = "webp"
        const val SVG = "svg"
        const val WEBM = "webm"
        const val AVI = "avi"
        const val MP4 = "mp4"
        const val MOV = "mov"
        const val FLV = "flv"
        const val WMV = "wmv"
        const val RM = "rm"
        const val AMV = "amv"
        const val MPG = "mpg"
        const val MPEG = "mpeg"
        const val M4V = "m4v"
        const val `3GP` = "3gp"
        const val OGG = "ogg"

        private fun parseFiles(context: Context, path: String, tag: String) {
            val fileStatArray = arrayListOf<FileInfo>()
            parse(context, path, tag, fileStatArray)
            Queueable.queue(tag, fileStatArray)
        }

        private fun parse(context: Context, path: String, tag: String, fileStatArray: MutableList<FileInfo>) {
            File(path).listFiles()?.forEach {
                val time = it.getFileTime()
                if (time > context.sp().getLong(FileStatManager.LAST_FILE_SCAN_DATE, 0L)) {
                    if (it.isDirectory) {
                        parse(context, it.absolutePath, tag, fileStatArray)
                    } else getFileData(context, it, tag, fileStatArray)
                }
            }
        }

        private fun getFileData(context: Context, file: File?, tag: String, fileStatArray: MutableList<FileInfo>) {
            file?.let {
                when (file.extension.toLowerCase()) {
                    JPG, JPEG, GIF, PNG, WEBP, SVG,
                    WEBM, AVI, MP4, MOV, FLV, WMV, RM, AMV, MPG, MPEG, M4V, `3GP`, OGG -> {
                        getExif(context, file, tag, fileStatArray)
                    }
                }
            }
        }

        //https://developer.android.com/reference/androidx/exifinterface/media/ExifInterface
        private fun getExif(context: Context, file: File, tag: String, fileStatArray: MutableList<FileInfo>) {
//        Log.e("---", "${file.name} ${file.length()} ${file.lastModified()}")
            context.contentResolver.openInputStream(Uri.fromFile(file)).use { inputStream ->
                inputStream?.let {
                    try {
                        val exif = ExifInterface(inputStream)
                        val copyRight = exif.getAttribute(ExifInterface.TAG_COPYRIGHT) ?: ""
                        val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME) ?: ""
                        val description = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)
                            ?: ""
                        val make = exif.getAttribute(ExifInterface.TAG_MAKE) ?: ""
                        val model = exif.getAttribute(ExifInterface.TAG_MODEL) ?: ""
                        val software = exif.getAttribute(ExifInterface.TAG_SOFTWARE) ?: ""
                        val artist = exif.getAttribute(ExifInterface.TAG_ARTIST) ?: ""
                        val exifVersion = exif.getAttribute(ExifInterface.TAG_EXIF_VERSION) ?: ""
                        val makerNote = exif.getAttribute(ExifInterface.TAG_MAKER_NOTE) ?: ""
                        val userComment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT) ?: ""
                        val dateOriginal = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                            ?: ""
                        val dateDigitized = exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)
                            ?: ""
                        val subSec = exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME) ?: ""
                        val subSecOrigin = exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME_ORIGINAL)
                            ?: ""
                        val subSecDigit = exif.getAttribute(ExifInterface.TAG_SUBSEC_TIME_DIGITIZED)
                            ?: ""
                        val subjectLocation = exif.getAttributeInt(ExifInterface.TAG_SUBJECT_LOCATION, -1)
                        val fileSource = exif.getAttribute(ExifInterface.TAG_FILE_SOURCE) ?: ""
                        val imageUniqueId = exif.getAttribute(ExifInterface.TAG_IMAGE_UNIQUE_ID)
                            ?: ""
                        val cameraOwnerName = exif.getAttribute(ExifInterface.TAG_CAMARA_OWNER_NAME)
                            ?: ""
                        val gpsLatRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF) ?: ""
                        val gpsLat = exif.getAttributeDouble(ExifInterface.TAG_GPS_LATITUDE, .0)
                        val gpsLongRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)
                            ?: ""
                        val gpsLong = exif.getAttributeDouble(ExifInterface.TAG_GPS_LONGITUDE, .0)
                        val gpsAlt = exif.getAttributeDouble(ExifInterface.TAG_GPS_ALTITUDE, .0)
                        val gpsAltRef = exif.getAttributeInt(ExifInterface.TAG_GPS_ALTITUDE_REF, -1)
                        val gpsTimestamp = exif.getAttributeDouble(ExifInterface.TAG_GPS_TIMESTAMP, .0)
                        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0)
                        val uniqueId = exif.getAttribute(ExifInterface.TAG_IMAGE_UNIQUE_ID) ?: ""
                        val xResolution = exif.getAttributeInt(ExifInterface.TAG_X_RESOLUTION, 0)
                        val yResolution = exif.getAttributeInt(ExifInterface.TAG_Y_RESOLUTION, 0)
                        val lensMake = exif.getAttribute(ExifInterface.TAG_LENS_MAKE) ?: ""
                        val lensModel = exif.getAttribute(ExifInterface.TAG_LENS_MODEL) ?: ""
                        val lensSerial = exif.getAttribute(ExifInterface.TAG_LENS_SERIAL_NUMBER)
                            ?: ""
                        val lensSpec = exif.getAttributeDouble(ExifInterface.TAG_LENS_SPECIFICATION, .0)
                        val gpsDirection = exif.getAttributeDouble(ExifInterface.TAG_GPS_IMG_DIRECTION, .0)
                        val gpsDirectionRef = exif.getAttribute(ExifInterface.TAG_GPS_IMG_DIRECTION_REF)
                            ?: ""
                        val xDimension = exif.getAttributeInt(ExifInterface.TAG_PIXEL_X_DIMENSION, 0)
                        val yDimension = exif.getAttributeInt(ExifInterface.TAG_PIXEL_Y_DIMENSION, 0)
                        val pix = (xDimension * yDimension) / 1000000.0
                        val megaPixels = Math.round(pix)
                        val probableCamDirection = compareWithCamData(context, megaPixels)

                        fileStatArray.add(
                            FileInfo(
                                tag,
                                file.name,
                                file.length(),
                                file.getFileTime(),
                                copyRight,
                                dateTime,
                                description,
                                make,
                                model,
                                software,
                                artist,
                                exifVersion,
                                makerNote,
                                userComment,
                                dateOriginal,
                                dateDigitized,
                                subSec,
                                subSecOrigin,
                                subSecDigit,
                                subjectLocation,
                                fileSource,
                                imageUniqueId,
                                cameraOwnerName,
                                gpsLatRef,
                                gpsLat,
                                gpsLongRef,
                                gpsLong,
                                gpsAltRef,
                                gpsAlt,
                                gpsTimestamp,
                                orientation,
                                uniqueId,
                                xResolution,
                                yResolution,
                                lensMake,
                                lensModel,
                                lensSerial,
                                lensSpec,
                                gpsDirection,
                                gpsDirectionRef,
                                xDimension,
                                yDimension,
                                probableCamDirection,
                                file.path
                            )
                        )
                    } catch (e: IOException) {
                        e.printStackTrace()
//                Firebase.logException(e, e.localizedMessage.toString())
                    }
                }
            }
        }

        private fun compareWithCamData(context: Context, megaPixels: Long): String {
            if (megaPixels == 0L)
                return ""

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    val dataArray = arrayListOf<Pair<Long, String>>()
                    val manager = context.getSystemService(AppCompatActivity.CAMERA_SERVICE) as CameraManager
                    manager.cameraIdList.forEach {
                        val camChar = manager.getCameraCharacteristics(it)
//                    val ver1 = camChar[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]
//                    val ver3 = if (Build.VERSION.SDK_INT >= 28) {
//                         camChar[CameraCharacteristics.INFO_VERSION]
//                    } else ""

                        val char = camChar[CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE]
                        val facing = camChar[CameraCharacteristics.LENS_FACING]
                        if (char != null) {
                            val pix = (char.width * char.height) / 1000000.0
                            val megaPix = Math.round(pix)
                            dataArray.add(
                                Pair(
                                    megaPix, when (facing) {
                                        CameraCharacteristics.LENS_FACING_FRONT -> "FRONT"
                                        CameraCharacteristics.LENS_FACING_BACK -> "BACK"
                                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "EXTERNAL"
                                        null -> "UNKNOWN"
                                        else -> "UNKNOWN_TYPE_$facing"
                                    }
                                )
                            )
                        }
                    }
                    dataArray.forEach {
                        if (megaPixels == it.first)
                            return it.second
                    }

                    return if (megaPixels > 10) "BACK" else "FRONT"
                } catch (ex: Exception) {
//                Firebase.logException(ex, "Nonfatal: exception in compareWithCamData()")
                }
            } else {
                val dataArray = arrayListOf<Pair<Long, String>>()
                val cam = Camera.getNumberOfCameras()
                val info = Camera.CameraInfo()
                repeat(cam) {
                    Camera.getCameraInfo(it, info)
                    val camera = Camera.open(it)
                    val sizes = camera.parameters.supportedPictureSizes
                    camera.release()
                    sizes?.forEach {
                        val pix = (it.width * it.height) / 1000000.0
                        val megaPix = Math.round(pix)
                        if (dataArray.count { it.first == megaPix } == 0)
                            dataArray.add(
                                Pair(
                                    megaPix, when (info.facing) {
                                        Camera.CameraInfo.CAMERA_FACING_FRONT -> "FRONT"
                                        Camera.CameraInfo.CAMERA_FACING_BACK -> "BACK"
                                        else -> "UNKNOWN_TYPE_${info.facing}"
                                    }
                                )
                            )
                    }
                }
                dataArray.forEach {
                    if (megaPixels == it.first)
                        return it.second
                }

                return if (megaPixels > 10) "BACK" else "FRONT"
            }

            return "UNKNOWN"
        }

        fun start(context: Context) {
            FileStatManager.parseFiles(context, "${Environment.getExternalStorageDirectory()}", ALL)
            FileStatManager.parseFiles(context, "${Environment.getExternalStorageDirectory()}/Downloads", DOWNLOADS)

            context.sp().edit().putLong(FileStatManager.LAST_FILE_SCAN_DATE, System.currentTimeMillis()).apply()
        }

        fun getPhotoPaths() = arrayListOf<Uri>().apply {
            collectPaths(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).toString(), this)
            collectPaths(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString(), this)
        }

        private fun collectPaths(path: String, arrayList: ArrayList<Uri>) {
            if (path.contains("cache") || path.contains("thumb"))
                return

            File(path).listFiles()?.forEach {
                if (it.isDirectory) {
                    collectPaths(it.absolutePath, arrayList)
                } else {
                    when (it.extension.toLowerCase()) {
                        JPG, JPEG, GIF, PNG, WEBP, SVG -> arrayList.add(it.toUri())
                    }
                }
            }
        }
    }
}

private fun File.getFileTime(): Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    Files.readAttributes(toPath(), BasicFileAttributes::class.java).creationTime().toMillis()
} else {
    lastModified()
}