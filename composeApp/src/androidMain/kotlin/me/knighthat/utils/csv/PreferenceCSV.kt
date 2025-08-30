package me.knighthat.utils.csv

import com.github.doyaaaaaken.kotlincsv.client.ICsvFileWriter

class PreferenceCSV(
    val type: String,
    val key: String,
    val value: Any
) {

    fun write( writer: ICsvFileWriter ) {
        writer.writeRow( type, key, value )
        writer.flush()      // Always flush after write to prevent overlapping
    }

    operator fun component1(): String = type

    operator fun component2(): String = key

    operator fun component3(): Any = value
}