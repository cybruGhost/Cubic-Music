package app.it.fast4x.rimusic.extensions.games.snake

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.it.fast4x.rimusic.utils.DataStoreUtils
import kotlinx.coroutines.delay
import kotlin.random.Random

// --- Data Classes & Enums ---
data class Cell(val x: Int, val y: Int)

enum class Direction { UP, DOWN, LEFT, RIGHT, NONE }
enum class GameState { PLAYING, PAUSED, GAME_OVER }

// --- Main Game Composable ---
@Composable
fun SnakeGame(
    modifier: Modifier = Modifier,
    colorPalette: app.it.fast4x.rimusic.ui.styling.ColorPalette,
    typography: app.it.fast4x.rimusic.ui.styling.Typography
) {
    val context = LocalContext.current
    val gridSize = 20
    var direction by remember { mutableStateOf(Direction.RIGHT) }
    var nextDirection by remember { mutableStateOf(Direction.RIGHT) }
    var snake by remember { mutableStateOf(listOf(Cell(10, 10), Cell(9, 10), Cell(8, 10))) }
    var food by remember { mutableStateOf(generateFood(snake, gridSize)) }
    var starFood by remember { mutableStateOf<Cell?>(null) }
    var starFoodTimer by remember { mutableStateOf(0) }
    var gameState by remember { mutableStateOf(GameState.PLAYING) }
    var score by remember { mutableStateOf(0) }
    var highScore by remember { mutableStateOf(getCachedHighScore(context)) }
    var snakeSpeed by remember { mutableStateOf(150L) }
    var lastMoveTime by remember { mutableStateOf(0L) }
    var lastStarFoodTime by remember { mutableStateOf(0L) }
    var invincible by remember { mutableStateOf(false) }
    var invincibleTimer by remember { mutableStateOf(0) }

    // Dynamic colors using app's color palette
    val cellBorder = colorPalette.textDisabled.copy(alpha = 0.3f)
    val emptyCellColor = colorPalette.background2
    val snakeHeadColor = if (invincible) colorPalette.accent else colorPalette.accent.copy(alpha = 0.9f)
    val snakeBodyColor = if (invincible) colorPalette.accent.copy(alpha = 0.7f) else colorPalette.accent.copy(alpha = 0.6f)
    val foodColor = colorPalette.red
    val starColor = Color(0xFFFFD700) // Gold color for star
    val textColor = colorPalette.text

    // Game loop
    LaunchedEffect(key1 = gameState) {
        if (gameState == GameState.PLAYING) {
            while (gameState == GameState.PLAYING) {
                val currentTime = System.currentTimeMillis()

                // Move snake
                if (currentTime - lastMoveTime >= snakeSpeed) {
                    direction = nextDirection
                    val newSnake = moveSnake(snake, direction, gridSize)

                    // Check self-collision - GAME OVER if snake hits itself
                    val head = newSnake.first()
                    if (!invincible && head in newSnake.drop(1)) {
                        gameState = GameState.GAME_OVER
                        if (score > highScore) {
                            highScore = score
                            saveHighScore(context, score)
                        }
                        break
                    }

                    snake = newSnake
                    lastMoveTime = currentTime

                    // Check normal food collection
                    if (head == food) {
                        food = generateFood(snake, gridSize)
                        snake = growSnake(snake)
                        score += 10
                        
                        // Spawn star food occasionally (beneficial)
                        if (Random.nextInt(100) < 20 && starFood == null) {
                            starFood = generateFood(snake + listOf(food), gridSize)
                            starFoodTimer = 80
                            lastStarFoodTime = currentTime
                        }
                    }

                    // Check star food collection - BENEFICIAL: extra points + invincibility
                    if (starFood != null && head == starFood) {
                        starFood = null
                        score += 50  // Bonus points
                        invincible = true
                        invincibleTimer = 40  // Invincibility for 40 moves
                    }
                }

                // Update star food timer
                if (starFood != null && currentTime - lastStarFoodTime >= snakeSpeed) {
                    starFoodTimer--
                    lastStarFoodTime = currentTime
                    if (starFoodTimer <= 0) {
                        starFood = null
                    }
                }

                // Update invincibility timer
                if (invincible && currentTime - lastMoveTime >= snakeSpeed) {
                    invincibleTimer--
                    if (invincibleTimer <= 0) {
                        invincible = false
                    }
                }

                delay(16)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(colorPalette.background0)) {
        when (gameState) {
            GameState.GAME_OVER -> {
                GameOverScreen(
                    score = score,
                    highScore = highScore,
                    onRestart = {
                        snake = listOf(Cell(10, 10), Cell(9, 10), Cell(8, 10))
                        direction = Direction.RIGHT
                        nextDirection = Direction.RIGHT
                        food = generateFood(snake, gridSize)
                        starFood = null
                        gameState = GameState.PLAYING
                        score = 0
                        snakeSpeed = 150L
                        invincible = false
                        invincibleTimer = 0
                    },
                    onClearHighScore = {
                        clearHighScore(context)
                        highScore = 0
                    },
                    colorPalette = colorPalette,
                    typography = typography
                )
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Score header
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Score: $score",
                                color = textColor,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Best: $highScore",
                                color = textColor.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }

                        if (invincible) {
                            Text(
                                text = "⭐ INVINCIBLE ⭐",
                                color = starColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                gameState = if (gameState == GameState.PLAYING)
                                    GameState.PAUSED
                                else
                                    GameState.PLAYING
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorPalette.background2,
                                contentColor = textColor
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(if (gameState == GameState.PAUSED) "Resume" else "Pause")
                        }
                    }

                    // Game board
                    GameBoard(
                        snake = snake,
                        food = food,
                        starFood = starFood,
                        gridSize = gridSize,
                        currentDirection = direction,
                        invincible = invincible,
                        snakeHeadColor = snakeHeadColor,
                        snakeBodyColor = snakeBodyColor,
                        foodColor = foodColor,
                        starColor = starColor,
                        emptyCellColor = emptyCellColor,
                        cellBorderColor = cellBorder
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Directional controls
                    Controls(
                        currentDirection = nextDirection,
                        onDirectionChange = { if (gameState == GameState.PLAYING) nextDirection = it },
                        colorPalette = colorPalette
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

// --- Game Board ---
@Composable
fun GameBoard(
    snake: List<Cell>,
    food: Cell,
    starFood: Cell?,
    gridSize: Int,
    currentDirection: Direction,
    invincible: Boolean,
    snakeHeadColor: Color,
    snakeBodyColor: Color,
    foodColor: Color,
    starColor: Color,
    emptyCellColor: Color,
    cellBorderColor: Color
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val maxBoardSize = screenWidth - 32.dp
    val cellSize = (maxBoardSize / gridSize).coerceAtLeast(12.dp)

    val pulseAnim = remember { Animatable(1f) }
    LaunchedEffect(food) {
        pulseAnim.animateTo(1.2f, tween(300, easing = LinearEasing))
        pulseAnim.animateTo(1f, tween(300, easing = LinearEasing))
    }

    val starPulseAnim = remember { Animatable(1f) }
    LaunchedEffect(starFood) {
        if (starFood != null) {
            starPulseAnim.animateTo(1.3f, tween(200, easing = LinearEasing))
            starPulseAnim.animateTo(1f, tween(200, easing = LinearEasing))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(emptyCellColor, RoundedCornerShape(24.dp))
            .border(1.dp, cellBorderColor, RoundedCornerShape(24.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(cellSize * gridSize)
                .background(emptyCellColor, RoundedCornerShape(12.dp))
        ) {
            for (y in 0 until gridSize) {
                Row {
                    for (x in 0 until gridSize) {
                        val cell = Cell(x, y)
                        val isSnakeHead = cell == snake.first()
                        val isSnakeBody = cell in snake.drop(1)

                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .border(BorderStroke(0.5.dp, cellBorderColor))
                                .background(
                                    when {
                                        isSnakeHead -> snakeHeadColor
                                        isSnakeBody -> snakeBodyColor
                                        cell == food -> foodColor
                                        cell == starFood -> starColor
                                        else -> emptyCellColor
                                    }
                                )
                                .graphicsLayer {
                                    scaleX = when {
                                        cell == food -> pulseAnim.value
                                        cell == starFood -> starPulseAnim.value
                                        else -> 1f
                                    }
                                    scaleY = when {
                                        cell == food -> pulseAnim.value
                                        cell == starFood -> starPulseAnim.value
                                        else -> 1f
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isSnakeHead -> {
                                    // Snake eyes based on direction
                                    val eyeOffset = when (currentDirection) {
                                        Direction.UP -> listOf(Offset(0.3f, 0.25f), Offset(0.7f, 0.25f))
                                        Direction.DOWN -> listOf(Offset(0.3f, 0.75f), Offset(0.7f, 0.75f))
                                        Direction.LEFT -> listOf(Offset(0.25f, 0.3f), Offset(0.25f, 0.7f))
                                        Direction.RIGHT -> listOf(Offset(0.75f, 0.3f), Offset(0.75f, 0.7f))
                                        else -> listOf(Offset(0.3f, 0.25f), Offset(0.7f, 0.25f))
                                    }
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        eyeOffset.forEach { offset ->
                                            Box(
                                                modifier = Modifier
                                                    .size(cellSize * 0.2f)
                                                    .align(Alignment.TopStart)
                                                    .graphicsLayer {
                                                        translationX = offset.x * cellSize.value
                                                        translationY = offset.y * cellSize.value
                                                    }
                                                    .background(Color.White, CircleShape)
                                                    .border(1.dp, Color.Black, CircleShape)
                                            )
                                        }
                                    }
                                }
                                cell == starFood -> {
                                    Text("⭐", fontSize = (cellSize.value * 0.6f).sp)
                                }
                                cell == food -> {
                                    Text("🍎", fontSize = (cellSize.value * 0.6f).sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Controls ---
@Composable
fun Controls(
    currentDirection: Direction,
    onDirectionChange: (Direction) -> Unit,
    colorPalette: app.it.fast4x.rimusic.ui.styling.ColorPalette
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        ControlButton(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = "Up",
            enabled = currentDirection != Direction.DOWN,
            onClick = { onDirectionChange(Direction.UP) },
            colorPalette = colorPalette
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            ControlButton(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Left",
                enabled = currentDirection != Direction.RIGHT,
                onClick = { onDirectionChange(Direction.LEFT) },
                colorPalette = colorPalette
            )
            ControlButton(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Right",
                enabled = currentDirection != Direction.LEFT,
                onClick = { onDirectionChange(Direction.RIGHT) },
                colorPalette = colorPalette
            )
        }
        ControlButton(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Down",
            enabled = currentDirection != Direction.UP,
            onClick = { onDirectionChange(Direction.DOWN) },
            colorPalette = colorPalette
        )
    }
}

@Composable
fun ControlButton(
    imageVector: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    colorPalette: app.it.fast4x.rimusic.ui.styling.ColorPalette
) {
    IconButton(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(if (enabled) colorPalette.background3 else colorPalette.textDisabled.copy(alpha = 0.3f)),
        onClick = onClick,
        enabled = enabled
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = if (enabled) colorPalette.text else colorPalette.textDisabled,
            modifier = Modifier.size(40.dp)
        )
    }
}

// --- Game Over Screen ---
@Composable
fun GameOverScreen(
    score: Int,
    highScore: Int,
    onRestart: () -> Unit,
    onClearHighScore: () -> Unit,
    colorPalette: app.it.fast4x.rimusic.ui.styling.ColorPalette,
    typography: app.it.fast4x.rimusic.ui.styling.Typography
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GAME OVER",
            color = colorPalette.red,
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Text(
            text = "Score: $score",
            color = colorPalette.text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (score == highScore && score > 0) {
            Text(
                text = "🏆 New High Score! 🏆",
                color = colorPalette.accent,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
        Button(
            onClick = onRestart,
            modifier = Modifier.padding(8.dp).width(160.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorPalette.accent),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("Play Again", fontSize = 18.sp, color = colorPalette.onAccent)
        }
        Button(
            onClick = onClearHighScore,
            modifier = Modifier.padding(8.dp).width(160.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colorPalette.background3),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("Clear High Score", fontSize = 18.sp, color = colorPalette.text)
        }
    }
}

// --- Game Logic Functions ---
fun moveSnake(snake: List<Cell>, direction: Direction, gridSize: Int): List<Cell> {
    if (direction == Direction.NONE) return snake
    val head = snake.first()
    val newHead = when (direction) {
        Direction.UP -> Cell(head.x, if (head.y - 1 < 0) gridSize - 1 else head.y - 1)
        Direction.DOWN -> Cell(head.x, (head.y + 1) % gridSize)
        Direction.LEFT -> Cell(if (head.x - 1 < 0) gridSize - 1 else head.x - 1, head.y)
        Direction.RIGHT -> Cell((head.x + 1) % gridSize, head.y)
        else -> head
    }
    return listOf(newHead) + snake.dropLast(1)
}

fun growSnake(snake: List<Cell>): List<Cell> {
    val tail = snake.last()
    val secondLast = snake.dropLast(1).last()
    val dx = tail.x - secondLast.x
    val dy = tail.y - secondLast.y
    val newTail = Cell(tail.x + dx, tail.y + dy)
    return snake + newTail
}

fun generateFood(occupiedCells: List<Cell>, gridSize: Int): Cell {
    val emptyCells = (0 until gridSize).flatMap { x ->
        (0 until gridSize).map { y -> Cell(x, y) }
    }.filter { it !in occupiedCells }
    return if (emptyCells.isNotEmpty()) emptyCells[Random.nextInt(emptyCells.size)]
    else Cell(Random.nextInt(gridSize), Random.nextInt(gridSize))
}

// --- High Score Management ---
private const val SNAKE_HIGH_SCORE_KEY = "snake_high_score"

private fun getCachedHighScore(context: android.content.Context): Int =
    DataStoreUtils.getIntBlocking(context, SNAKE_HIGH_SCORE_KEY, 0)

private fun saveHighScore(context: android.content.Context, score: Int) {
    DataStoreUtils.saveIntBlocking(context, SNAKE_HIGH_SCORE_KEY, score)
}

private fun clearHighScore(context: android.content.Context) {
    DataStoreUtils.saveIntBlocking(context, SNAKE_HIGH_SCORE_KEY, 0)
}
