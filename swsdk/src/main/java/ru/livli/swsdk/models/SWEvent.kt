package ru.livli.swsdk.models

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import org.jetbrains.annotations.NotNull

@Entity
internal open class BaseEntity(
    @Id var innedId: Long = 0L,
    val utc: Long = System.currentTimeMillis()
)

//@Entity
//data class SWEvent(@NotNull val name: String = "",
//                   @NotNull
//                   @Convert(converter = HashMapConverter::class, dbType = String::class)
//                   val params: Map<String, Any?> = hashMapOf()) : BaseEntity()
@Entity
internal data class SWEvent(
    @NotNull val name: String = "",
    @NotNull val params: String = ""
) : BaseEntity()

@Entity
internal data class SWSensorData(@NotNull val byteArray: ByteArray = ByteArray(0)) : BaseEntity()