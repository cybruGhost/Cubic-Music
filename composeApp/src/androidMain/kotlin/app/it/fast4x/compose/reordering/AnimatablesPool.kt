package app.it.fast4x.compose.reordering

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.TwoWayConverter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AnimatablesPool<T, V : AnimationVector>(
    private val initialValue: T,
    private val typeConverter: TwoWayConverter<T, V>,
    private val visibilityThreshold: T? = null
) {
    private val animatables = mutableListOf<Animatable<T, V>>()
    private val mutex = Mutex()

    suspend fun acquire() = mutex.withLock {
        animatables.removeFirstOrNull() ?: Animatable(
            initialValue = initialValue,
            typeConverter = typeConverter,
            visibilityThreshold = visibilityThreshold,
            label = "AnimatablesPool: Animatable"
        )
    }

    suspend fun release(animatable: Animatable<T, V>) = mutex.withLock {
        animatable.snapTo(initialValue)
        animatables += animatable
    }
}
