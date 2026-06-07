package app.it.fast4x.rimusic.extensions.games.pacman

import android.util.Log
import android.util.Range
import androidx.compose.runtime.MutableState
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.it.fast4x.rimusic.extensions.games.pacman.utils.GameConstants.incrementValue
import kotlinx.coroutines.*
class GameViewModel : ViewModel() {

    private val logTag = "GameViewModel"
    private var leftPress: Boolean = false
    private var rightPress: Boolean = false
    private var downPress: Boolean = false
    private var upPress: Boolean = false
    private var leftJob: Job? = null
    private var rightJob: Job? = null
    private var upJob: Job? = null
    private var downJob: Job? = null
    private val movementDelayMs = 120L


    /* handles the direction the character is facing
         start angle
         when going left = 25f
         when going right = 200f
         when going down = 100f
         when going up = 280f
          */
    private var _characterStartAngle = MutableLiveData(25f)
    val characterStartAngle: LiveData<Float>
        get() = _characterStartAngle


    // handle presses
    fun rightPress(characterXOffset: MutableState<Float>, characterYOffset: MutableState<Float>) {
        if (rightPress) return
        releaseLeft()
        releaseUp()
        releaseDown()
        rightPress = true
        _characterStartAngle.postValue(25f) // change direction character is facing
        rightJob = viewModelScope.launch {
            while (rightPress) {
                // move character to opposite wall
                if (characterXOffset.value > 315f) characterXOffset.value = -400f

                // implement barrier constraints

                if (
                //Top Right
                    Range.create(-310f, -225f).contains(characterXOffset.value) &&
                    Range.create(-975f, -900f).contains(characterYOffset.value) ||
                    // Top Left
                    Range.create(75f, 150f).contains(characterXOffset.value) &&
                    Range.create(-975f, -900f).contains(characterYOffset.value) ||
                    // EnemyBox
                    Range.create(-150f, -75f).contains(characterXOffset.value) &&
                    Range.create(-450f, -375f).contains(characterYOffset.value) ||
                    // Bottom Left
                    Range.create(-310f, -225f).contains(characterXOffset.value) &&
                    Range.create(-150f, -75f).contains(characterYOffset.value) ||
                    // Bottom Right
                    Range.create(75f, 150f).contains(characterXOffset.value) &&
                    Range.create(-150f, -75f).contains(characterYOffset.value)

                ) characterXOffset.value += 0f else characterXOffset.value += incrementValue

                Log.d(
                    logTag,
                    "rightpress: x:  ${characterXOffset.value} y: ${characterYOffset.value}"
                )

                delay(movementDelayMs)
            }
        }
    }

    fun leftPress(characterXOffset: MutableState<Float>, characterYOffset: MutableState<Float>) {
        if (leftPress) return
        releaseRight()
        releaseUp()
        releaseDown()
        leftPress = true
        _characterStartAngle.postValue(200f) // change direction character is facing
        leftJob = viewModelScope.launch {
            while (leftPress) {
                // move character to opposite wall
                if (characterXOffset.value <= -290f) characterXOffset.value = +450f

                // implement barrier constraints

                if (
                //Top Right
                    Range.create(350f, 425f).contains(characterXOffset.value) &&
                    Range.create(-975f, -900f).contains(characterYOffset.value) ||
                    // Top Left
                    Range.create(-150f, -75f).contains(characterXOffset.value) &&
                    Range.create(-975f, -900f).contains(characterYOffset.value) ||
                    // EnemyBox
                    Range.create(75f, 150f).contains(characterXOffset.value) &&
                    Range.create(-450f, -375f).contains(characterYOffset.value) ||
                    // Bottom Left
                    Range.create(-150f, -75f).contains(characterXOffset.value) &&
                    Range.create(-150f, -75f).contains(characterYOffset.value) ||
                    // Bottom Right
                    Range.create(225f, 300f).contains(characterXOffset.value) &&
                    Range.create(-150f, -75f).contains(characterYOffset.value)

                ) characterXOffset.value -= 0f else characterXOffset.value -= incrementValue

                Log.d(
                    logTag,
                    "leftPress: X: ${characterXOffset.value} Y: ${characterYOffset.value}"
                )

                delay(movementDelayMs)
            }
        }
    }

    fun upPress(characterYOffset: MutableState<Float>, characterXOffset: MutableState<Float>) {
        if (upPress) return
        releaseLeft()
        releaseRight()
        releaseDown()
        upPress = true
        _characterStartAngle.postValue(280f) // change direction character is facing
        upJob = viewModelScope.launch {
            while (upPress) {
                // implement barrier constraints

                if (
                // keep inside border
                    characterYOffset.value <= -1000f ||
                    //Top Right
                    Range.create(150f, 300f).contains(characterXOffset.value) &&
                    Range.create(-975f, -900f).contains(characterYOffset.value) ||
                    // Top Left
                    Range.create(-225f, -75f).contains(characterXOffset.value) &&
                    Range.create(-975f, -900f).contains(characterYOffset.value) ||
                    // EnemyBox
                    Range.create(-75f, 150f).contains(characterXOffset.value) &&
                    Range.create(-375f, -300f).contains(characterYOffset.value) ||
                    // Bottom Left
                    Range.create(-225f, -0f).contains(characterXOffset.value) &&
                    Range.create(-150f, -75f).contains(characterYOffset.value) ||
                    // Bottom Right
                    Range.create(150f, 300f).contains(characterXOffset.value) &&
                    Range.create(-150f, -75f).contains(characterYOffset.value)

                ) characterYOffset.value -= 0f else characterYOffset.value -= incrementValue

                Log.d(logTag, "UpPress: Y: ${characterYOffset.value} x: ${characterXOffset.value}")
                delay(movementDelayMs)
            }
        }
    }

    fun downPress(characterYOffset: MutableState<Float>, characterXOffset: MutableState<Float>) {
        if (downPress) return
        releaseLeft()
        releaseRight()
        releaseUp()
        downPress = true
        _characterStartAngle.postValue(100f) // change direction character is facing
        downJob = viewModelScope.launch {
            while (downPress) {
                if (
                // keep inside border
                    characterYOffset.value >= 0f ||
                    //Top Right
                    Range.create(125f, 300f).contains(characterXOffset.value) &&
                    Range.create(-1050f, -825f).contains(characterYOffset.value) ||
                    // Top Left
                    Range.create(-250f, -75f).contains(characterXOffset.value) &&
                    Range.create(-1050f, -825f).contains(characterYOffset.value) ||
                    // EnemyBox
                    Range.create(-75f, 150f).contains(characterXOffset.value) &&
                    Range.create(-450f, -375f).contains(characterYOffset.value) ||
                    // Bottom Left
                    Range.create(-225f, -0f).contains(characterXOffset.value) &&
                    Range.create(-225f, -200f).contains(characterYOffset.value) ||
                    // Bottom Right
                    Range.create(150f, 300f).contains(characterXOffset.value) &&
                    Range.create(-225f, -200f).contains(characterYOffset.value)

                ) characterYOffset.value += 0f else characterYOffset.value += incrementValue

                Log.d(logTag, "downPress: y: ${characterYOffset.value}, x: ${characterXOffset.value}")

                delay(movementDelayMs)
            }
        }
    }

//Cancel presses

    fun releaseLeft() {
        leftPress = false
        leftJob?.cancel()
        leftJob = null
    }

    fun releaseRight() {
        rightPress = false
        rightJob?.cancel()
        rightJob = null
    }

    fun releaseUp() {
        upPress = false
        upJob?.cancel()
        upJob = null
    }

    fun releaseDown() {
        downPress = false
        downJob?.cancel()
        downJob = null
    }


}
