package ru.livli.swsdk.utils

import android.content.Context
import com.google.gson.Gson
import io.objectbox.Box
import io.objectbox.BoxStore
import org.jetbrains.annotations.NotNull
import ru.livli.swsdk.models.BaseEntity
import ru.livli.swsdk.models.MyObjectBox
import ru.livli.swsdk.models.SWEvent

internal class Queueable {
    companion object {
        private val gson by lazy { Gson() }
        @Volatile
        var boxStore: BoxStore? = null

        fun getInstance(@NotNull context: Context): BoxStore =
            boxStore ?: synchronized(this) {
                boxStore
                    ?: MyObjectBox.builder().androidContext(context).build().also { boxStore = it }
            }

        fun queue(@NotNull name: String, @NotNull collection: Collection<Any?>) {
            boxStore?.let {
                it.boxFor(SWEvent::class.java).apply {
                    arrayListOf<SWEvent>().apply {
                        collection.forEach {
                            add(SWEvent(name, gson.toJson(it)))
                        }
                        put(this)
                        "--- any: $this".error
                    }
                }
            }
        }

        fun queue(@NotNull name: String, @NotNull obj: Any?) {
            boxStore?.let {
                it.boxFor(SWEvent::class.java).apply {
                    put(SWEvent(name, gson.toJson(obj))) { any ->
                        "--- any: $any".error
                    }
                }
            }
        }

        fun saveForLater(obj: BaseEntity) {
            boxStore?.let {
                val clazz = SWEvent::class.java
                it.boxFor(clazz).apply {
                    put(SWEvent(clazz.simpleName, Gson().toJson(obj)))
                }
            }
        }
    }
}

private fun <T> Box<T>.put(swEvent: T, function: (T) -> String) {
    put(swEvent)
    function.invoke(swEvent)
}
