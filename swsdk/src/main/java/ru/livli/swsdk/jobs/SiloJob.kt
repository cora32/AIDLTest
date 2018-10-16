package ru.livli.swsdk.jobs

import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.google.gson.JsonParser
import org.jetbrains.anko.coroutines.experimental.bg
import ru.livli.swsdk.SWSdk
import ru.livli.swsdk.jobs.Silo.MB1
import ru.livli.swsdk.models.SWEvent
import ru.livli.swsdk.models.SWSensorData
import ru.livli.swsdk.models.SiloData
import ru.livli.swsdk.utils.Queueable

internal class SiloJob : JobService() {
    private var inProgress = false

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        if (inProgress) {
            jobFinished(jobParameters, true)
            return false
        }

        bg {
            inProgress = true
            Silo.uploadData()
        }.invokeOnCompletion {
            jobFinished(jobParameters, true)
            inProgress = false
        }
        return false
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        inProgress = false
        return false
    }
}

internal object Silo {
    const val MB1 = 1 * 1024 * 1024

    fun uploadData() {
        //Send saved sensors data
        Queueable.boxStore?.boxFor(SWSensorData::class.java)
            ?.let { db ->
                try {
                    db.all.forEach {
                        val response = SWSdk.sendSensorsData(it.byteArray)?.execute()
                        if (response?.isSuccessful != true)
                            return@let
                    }
                    db.removeAll()
                } catch (ex: Exception) {
                }
            }

        //Send silo data
        Queueable.boxStore?.boxFor(SWEvent::class.java)
            ?.let { db ->
                val gson = JsonParser()
                var batch = 0
                val query = db.query().build()
                var entries = query.find(0, 3000)
                while (entries.isNotEmpty()) {
                    entries.chunkedMb(MB1)
                        .forEach {
                            val chunk = it.groupBy({ it.name }, { restoreObj(gson, it.params) })
                            if (chunk.isNotEmpty() && chunk.values.isNotEmpty())
                                SWSdk.sendToSilo(
                                    SiloData(
                                        batch = batch++,
                                        data = chunk,
                                        uuid = ""
                                    )
                                )?.let {
                                    val response = it.execute()
                                    if (response.isSuccessful && response.body()?.status != "ok") {
                                    } else {
                                        return
                                    }
                                }
                        }
                    db.remove(entries)
                    entries = query.find(0, 3000)
                }
            }
    }

    //    private val anyToken = object : TypeToken<Any>() {}.type
//    private fun restoreObj(gson: Gson, json: String) = gson.fromJson<Any>(json, anyToken)?:Any()
    private fun restoreObj(gson: JsonParser, json: String) =
        gson.parse(json) //Have to use JsonParser as Gson cannot parse correctly Long values
}

private fun List<SWEvent>.chunkedMb(maxByteSize: Int = MB1): List<List<SWEvent>> {
    val result = arrayListOf<List<SWEvent>>()
    val chunkList = arrayListOf<SWEvent>()
    var chunkSize = 0
    forEach {
        if (chunkSize >= maxByteSize) {
            chunkSize = 0
            result.add(chunkList.slice(0 until chunkList.size))
            chunkList.clear()
        }

        chunkList.add(it)
        chunkSize += it.name.length + it.params.length
    }
    //Saving last chunk
    result.add(chunkList)

    return result
}