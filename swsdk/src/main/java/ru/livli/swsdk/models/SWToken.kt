package ru.livli.swsdk.models

import com.google.gson.annotations.SerializedName
import org.jetbrains.annotations.NotNull

internal data class SWToken(
    @SerializedName("token") @NotNull val token: String,
    @SerializedName("type") @NotNull val type: String,
    @SerializedName("permissions") @NotNull val permissions: List<String>
) : BaseEntity()