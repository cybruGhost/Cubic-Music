package it.fast4x.compose.persist

import android.util.Log
import androidx.compose.runtime.compositionLocalOf

typealias PersistMap = HashMap<String, Any?>

val LocalPersistMap = compositionLocalOf<PersistMap?> { 
    // Don't log error when PersistMap is not available (it's disabled)
    null 
}