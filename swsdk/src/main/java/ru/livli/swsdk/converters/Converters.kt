package ru.livli.swsdk.converters

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.objectbox.converter.PropertyConverter

internal class HashMapConverter : PropertyConverter<Map<String, String>, String> {
    private val gson by lazy { Gson() }

    override fun convertToDatabaseValue(entityProperty: Map<String, String>?): String {
        return gson.toJson(entityProperty)
    }

    override fun convertToEntityProperty(databaseValue: String?): Map<String, String> {
        return gson.fromJson<Map<String, String>>(databaseValue,
            object : TypeToken<Map<String, String>>() {}.type
        )
    }

}