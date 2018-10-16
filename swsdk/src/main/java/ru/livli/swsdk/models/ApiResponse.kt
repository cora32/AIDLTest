package ru.livli.swsdk.models

data class ApiResponse(
    val status: String,
    val message: String? = null,
    val secret: String? = null
)