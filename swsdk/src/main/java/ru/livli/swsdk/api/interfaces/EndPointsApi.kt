package ru.livli.swsdk.api.interfaces

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import ru.livli.swsdk.models.ApiKey
import ru.livli.swsdk.models.ApiResponse
import ru.livli.swsdk.models.SWToken
import ru.livli.swsdk.models.SiloData

internal interface EndPointsApi {
    @POST("sdk/checkApiKey")
    @Headers("Accept: application/json")
    fun checkKey(@Body apiKey: ApiKey): Call<ApiResponse>

    @POST("sdk/registerApiKey")
    @Headers("Accept: application/json")
    fun register(@Body apiKey: ApiKey): Call<ApiResponse>

    @POST("sdk/sendToken")
    @Headers("Accept: application/json")
    fun sendToken(@Body token: SWToken): Call<ApiResponse>

    @POST("sdk/silo")
    @Headers("Accept: application/json")
    fun sendToSilo(@Body json: SiloData): Call<ApiResponse>

    @POST("sdk/sensors_bin")
    @Headers("Accept: application/json")
    fun sendSensorData(@Body bytes: RequestBody): Call<ApiResponse>
}