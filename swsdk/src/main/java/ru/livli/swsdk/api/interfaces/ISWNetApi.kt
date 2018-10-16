package ru.livli.swsdk.api.interfaces

import org.jetbrains.annotations.NotNull
import retrofit2.Call
import ru.livli.swsdk.api.impl.SWNetApi
import ru.livli.swsdk.models.ApiResponse
import ru.livli.swsdk.models.SiloData

interface ISWNetApi {
    fun sendToken(@NotNull token: String, type: SWNetApi.ServiceType, permissions: List<String>)
    fun register(@NotNull apiKey: String)
    fun checkKey(@NotNull apiKey: String)
    fun sendToSilo(
        siloData: SiloData,
        onSuccess: () -> Unit,
        onError: () -> Unit
    )

    fun sendToSilo(siloData: SiloData): Call<ApiResponse>
    fun sendSensorsData(byteArray: ByteArray): Call<ApiResponse>
}