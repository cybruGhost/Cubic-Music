package app.kreate.android.me.knighthat.database

import android.os.Parcel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@TypeConverters
object Converters {

    @TypeConverter
    @JvmStatic
    fun fromString(stringListString: String): List<String> {
        return stringListString.split(",").map { it }
    }

    @TypeConverter
    @JvmStatic
    fun toString(stringList: List<String>): String {
        return stringList.joinToString(separator = ",")
    }

    @TypeConverter
    @JvmStatic
    @UnstableApi
    fun mediaItemFromByteArray(value: ByteArray?): MediaItem? {
        return value?.let { byteArray ->
            runCatching {
                val parcel = Parcel.obtain()
                parcel.unmarshall(byteArray, 0, byteArray.size)
                parcel.setDataPosition(0)
                val bundle = parcel.readBundle(MediaItem::class.java.classLoader)
                parcel.recycle()

                bundle?.let(MediaItem::fromBundle)
            }.getOrNull()
        }
    }

    @TypeConverter
    @JvmStatic
    @UnstableApi
    fun mediaItemToByteArray(mediaItem: MediaItem?): ByteArray? {
        return mediaItem?.toBundle()?.let { persistableBundle ->
            val parcel = Parcel.obtain()
            parcel.writeBundle(persistableBundle)
            val bytes = parcel.marshall()
            parcel.recycle()

            bytes
        }
    }
}