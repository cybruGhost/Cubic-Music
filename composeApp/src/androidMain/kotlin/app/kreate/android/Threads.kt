package app.kreate.android

import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * Collection of useful threads.
 *
 * Must be closed individually after use to prevent unwanted outcome
 */
object Threads {

    /**
     * Single thread dispatcher guarantee jobs are
     * executed in the order that were given to it.
     *
     * Should only be used by DataspecServices.kt
     */
    val DATASPEC_DISPATCHER = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
}