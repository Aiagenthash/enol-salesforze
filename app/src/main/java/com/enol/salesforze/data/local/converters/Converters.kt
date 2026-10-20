package com.enol.salesforze.data.local.converters

import androidx.room.TypeConverter
import com.enol.salesforze.data.local.entities.ProductScheme
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.Date
import java.util.UUID

class Converters {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    // --- Date Converters ---
    @TypeConverter
    fun fromTimestampToDate(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // --- Instant Converters ---
    @TypeConverter
    fun fromEpochMilliToInstant(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun instantToEpochMilli(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }

    // --- UUID Converters ---
    @TypeConverter
    fun fromStringToUuid(value: String?): UUID? {
        return value?.let {
            try {
                UUID.fromString(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    @TypeConverter
    fun uuidToString(uuid: UUID?): String? {
        return uuid?.toString()
    }

    // --- List<String> Converters ---
    @TypeConverter
    fun fromStringToList(value: String?): List<String>? {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<String>>(value)
        } catch (e: Exception) {
            // Fallback to comma-separated if not valid JSON
            value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }
    }

    @TypeConverter
    fun stringListToString(list: List<String>?): String? {
        if (list == null) return null
        return try {
            json.encodeToString(list)
        } catch (e: Exception) {
            list.joinToString(",")
        }
    }

    // --- ProductScheme Converters ---
    @TypeConverter
    fun fromStringToProductSchemeList(value: String?): List<ProductScheme>? {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString<List<ProductScheme>>(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun productSchemeListToString(schemes: List<ProductScheme>?): String? {
        if (schemes == null) return null
        return try {
            json.encodeToString(schemes)
        } catch (e: Exception) {
            "[]"
        }
    }

    @TypeConverter
    fun fromStringToProductScheme(value: String?): ProductScheme? {
        if (value.isNullOrBlank()) return null
        return try {
            json.decodeFromString<ProductScheme>(value)
        } catch (e: Exception) {
            null
        }
    }

    @TypeConverter
    fun productSchemeToString(scheme: ProductScheme?): String? {
        if (scheme == null) return null
        return try {
            json.encodeToString(scheme)
        } catch (e: Exception) {
            null
        }
    }

    // --- Generic Map<String, String> Payload Converters ---
    @TypeConverter
    fun fromStringToMap(value: String?): Map<String, String>? {
        if (value.isNullOrBlank()) return emptyMap()
        return try {
            json.decodeFromString<Map<String, String>>(value)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun mapToString(map: Map<String, String>?): String? {
        if (map == null) return null
        return try {
            json.encodeToString(map)
        } catch (e: Exception) {
            "{}"
        }
    }
}
