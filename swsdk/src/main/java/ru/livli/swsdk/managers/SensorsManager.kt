package ru.livli.swsdk.managers

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import org.jetbrains.anko.coroutines.experimental.bg
import ru.livli.swsdk.SWSdk
import ru.livli.swsdk.api.interfaces.ISWNetApi
import ru.livli.swsdk.models.SWSensorData
import ru.livli.swsdk.models.SavableByteArray
import ru.livli.swsdk.utils.Queueable
import ru.livli.swsdk.utils.sha1
import java.nio.ByteBuffer

internal class SensorsManager(
    private val sensorManager: SensorManager,
    val type: Int,
    val api: ISWNetApi
) : SensorEventListener {
    companion object {
        const val MB_1 = 1 * 1024 * 1024
        private const val PACK_SIZE = 14
        private const val MAX_CHUNKS = 50
        const val limit = MB_1 - PACK_SIZE
        private val serviceList = arrayListOf<SensorsManager?>()

        fun start(context: Context, uid: String, timestamp: Long, type: Int, api: ISWNetApi) {
            sha1("$uid$timestamp$type")?.let { sha ->
                //                "--- starting type: $type, ${sha.size} = $sha".error
                with(context.getSystemService(Context.SENSOR_SERVICE) as SensorManager) {
                    serviceList.add(SensorsManager(this, type, api).start(sha))
                }
            }
        }

        fun stopAll() {
            serviceList.forEach { it?.stop() }
            serviceList.clear()
        }
    }

    private var latestTimestamp = System.currentTimeMillis()
    private val byteBuffer = ByteBuffer.allocate(MB_1)
    private var id = ByteArray(0)
    private var counter = 0
    override fun onAccuracyChanged(sensor: Sensor?, p1: Int) {}
    private val saver by lazy { Queueable.boxStore?.boxFor(SWSensorData::class.java) }

    override fun onSensorChanged(event: SensorEvent?) {
        if (counter > MAX_CHUNKS) {
            stopAll()
            return
        }

        if (byteBuffer.position() > limit) {
            sendData()
        }

        event?.let {
            //            "--- type: ${type}; event: ${event.sensor}".error
            if (it.values.size == 3) {
                //Adding x,y,z data
                for (i in 0 until it.values.size) {
//                    Log.e("--- VALUE:", "${it.values[i]}")
                    byteBuffer.putFloat(it.values[i])
                }

                val now = System.currentTimeMillis()
                val dd = now - latestTimestamp
                val delta = (dd) / 1000
//                "--- x: ${it.values[0]} y: ${it.values[0]} z: ${it.values[0]} delta: $delta".error
//                Log.e("--- Delta:", "$now - $latestTimestamp = $delta")
                latestTimestamp = now
                //Adding timestamp delta
                byteBuffer.putShort(delta.toShort())
            }
        }

//        parseString()
    }

    private fun buildHeader(id: ByteArray) {
        byteBuffer.clear()

        //Put session contentId
        byteBuffer.put(id)

        //Put initial latestTimestamp
        latestTimestamp = System.currentTimeMillis()
        byteBuffer.putLong(latestTimestamp)
    }

    fun start(id: ByteArray): SensorsManager {
        counter = 0
        this.id = id
        buildHeader(id)

        sensorManager.getDefaultSensor(type)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        return this
    }

    fun stop() {
//        "--- stopping collecting for type: $type".error
        sensorManager.unregisterListener(this)
        sendData()
    }

    private fun sendData() {
        counter++
        val tempByteBuffer = SavableByteArray(byteBuffer.array().sliceArray(0..byteBuffer.position()))
        buildHeader(id)

        tempByteBuffer.byteArray?.let { byteArray ->
            if (byteArray.isNotEmpty()) {
                bg {
                    SWSdk.sendSensorsData(byteArray)?.execute()
                }.invokeOnCompletion {
                    it?.printStackTrace()
                    saver?.put(SWSensorData(byteArray))
                }
            }
        }
    }
}