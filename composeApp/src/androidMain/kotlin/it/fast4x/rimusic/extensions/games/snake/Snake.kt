package it.fast4x.rimusic.extensions.games.snake

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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random
import kotlin.math.cos
import kotlin.math.sin

data class Cell(val x: Int, val y: Int)

enum class Direction {
    UP, DOWN, LEFT, RIGHT, NONE
}

data class Villager(val position: Cell, val type: VillagerType, val movementPattern: MovementPattern)

enum class VillagerType(val color: Color, val emoji: String, val points: Int) {
    FARMER(Color(0xFF8B4513), "👨‍🌾", 20),      // Brown
    MERCHANT(Color(0xFF4682B4), "👨‍💼", 30),   // Steel Blue
    BLACKSMITH(Color(0xFF2F4F4F), "👨‍🏭", 40), // Dark Slate Gray
    BAKER(Color(0xFFFFD700), "👨‍🍳", 50)       // Gold
}

enum class MovementPattern {
    RANDOM, CIRCULAR, FLEEING, PATROL
}

enum class GameState {
    PLAYING, PAUSED, GAME_OVER, LEVEL_COMPLETE
}

@Composable
fun SnakeGame() {
    val gridSize = 20
    var direction by remember { mutableStateOf(Direction.RIGHT) }
    var nextDirection by remember { mutableStateOf(Direction.RIGHT) }
    var snake by remember { mutableStateOf(listOf(Cell(5, 5), Cell(4, 5), Cell(3, 5))) }
    var food by remember { mutableStateOf(generateFood(snake, gridSize)) }
    var specialFood by remember { mutableStateOf<Cell?>(null) }
    var specialFoodTimer by remember { mutableStateOf(0) }
    var villagers by remember { mutableStateOf(generateVillagers(gridSize, 3)) }
    var gameState by remember { mutableStateOf(GameState.PLAYING) }
    var score by remember { mutableStateOf(0) }
    var highScore by remember { mutableStateOf(0) }
    var level by remember { mutableStateOf(1) }
    var snakeSpeed by remember { mutableStateOf(150L) }
    var lastMoveTime by remember { mutableStateOf(0L) }
    var lastVillagerMoveTime by remember { mutableStateOf(0L) }
    var lastSpecialFoodTime by remember { mutableStateOf(0L) }
    var invincible by remember { mutableStateOf(false) }
    var invincibleTimer by remember { mutableStateOf(0) }

    // Game loop
    LaunchedEffect(key1 = gameState) {
        if (gameState == GameState.PLAYING) {
            while (gameState == GameState.PLAYING) {
                val currentTime = System.currentTimeMillis()
                
                // Move snake
                if (currentTime - lastMoveTime >= snakeSpeed) {
                    direction = nextDirection
                    val newSnake = moveSnake(snake, direction, gridSize)
                    
                    // Check collisions
                    val head = newSnake.first()
                    if (head in newSnake.drop(1) && !invincible) {
                        gameState = GameState.GAME_OVER
                        if (score > highScore) highScore = score
                        break
                    }
                    
                    snake = newSnake
                    lastMoveTime = currentTime
                    
                    // Check food collection
                    if (head == food) {
                        food = generateFood(snake + villagers.map { it.position }, gridSize)
                        snake = growSnake(snake)
                        score += 10
                        // Occasionally spawn special food
                        if (Random.nextInt(100) < 30) {
                            specialFood = generateFood(snake + villagers.map { it.position } + listOf(food), gridSize)
                            specialFoodTimer = 100 // Special food disappears after 100 moves
                            lastSpecialFoodTime = currentTime
                        }
                        // Level up after collecting certain amount of food
                        if (score % 100 == 0) {
                            level++
                            snakeSpeed = (snakeSpeed * 0.9).toLong().coerceAtLeast(80L)
                            villagers = generateVillagers(gridSize, 3 + level.coerceAtMost(5))
                        }
                    }
                    
                    // Check special food collection
                    if (specialFood != null && head == specialFood) {
                        specialFood = null
                        score += 50
                        invincible = true
                        invincibleTimer = 50 // Invincibility for 50 moves
                    }
                    
                    // Check villager collection
                    val caughtVillager = villagers.find { it.position == head }
                    if (caughtVillager != null && !invincible) {
                        villagers = villagers.filter { it != caughtVillager }
                        score += caughtVillager.type.points
                        // Add new villager if below max
                        if (villagers.size < 5 + level) {
                            villagers = villagers + generateVillagers(gridSize, 1)
                        }
                    }
                }
                
                // Move villagers every 600ms
                if (currentTime - lastVillagerMoveTime >= 600) {
                    villagers = moveVillagers(villagers, snake.first(), gridSize)
                    lastVillagerMoveTime = currentTime
                }
                
                // Update special food timer
                if (specialFood != null && currentTime - lastSpecialFoodTime >= snakeSpeed) {
                    specialFoodTimer--
                    lastSpecialFoodTime = currentTime
                    if (specialFoodTimer <= 0) {
                        specialFood = null
                    }
                }
                
                // Update invincibility timer
                if (invincible && currentTime - lastMoveTime >= snakeSpeed) {
                    invincibleTimer--
                    if (invincibleTimer <= 0) {
                        invincible = false
                    }
                }
                
                delay(16) // ~60 FPS
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2C3E50))) {
        when (gameState) {
            GameState.GAME_OVER -> {
                GameOverScreen(score, highScore, onRestart = {
                    snake = listOf(Cell(5, 5), Cell(4, 5), Cell(3, 5))
                    direction = Direction.RIGHT
                    nextDirection = Direction.RIGHT
                    food = generateFood(snake, gridSize)
                    villagers = generateVillagers(gridSize, 3)
                    gameState = GameState.PLAYING
                    score = 0
                    level = 1
                    snakeSpeed = 150L
                    specialFood = null
                    invincible = false
                })
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Score, level and controls
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Score: $score",
                                color = Color.White,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "High: $highScore",
                                color = Color.LightGray,
                                fontSize = 14.sp
                            )
                        }
                        
                        Text(
                            text = "Level: $level",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        
                        Row {
                            Button(
                                onClick = { 
                                    gameState = if (gameState == GameState.PLAYING) 
                                        GameState.PAUSED 
                                    else 
                                        GameState.PLAYING 
                                },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(if (gameState == GameState.PAUSED) "Resume" else "Pause")
                            }
                        }
                    }
                    
                    // Game board
                    GameBoard(
                        snake, 
                        food, 
                        specialFood,
                        villagers, 
                        gridSize, 
                        direction,
                        invincible,
                        { if (gameState == GameState.PLAYING) nextDirection = it }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Controls
                    Controls(currentDirection = nextDirection, onDirectionChange = { 
                        if (gameState == GameState.PLAYING) nextDirection = it 
                    })
                }
            }
        }
    }
}

fun moveSnake(snake: List<Cell>, direction: Direction, gridSize: Int): List<Cell> {
    if (direction == Direction.NONE) return snake
    
    val head = snake.first()
    val newHead = when (direction) {
        Direction.UP -> Cell(head.x, (head.y - 1).takeIf { it >= 0 } ?: (gridSize - 1))
        Direction.DOWN -> Cell(head.x, (head.y + 1) % gridSize)
        Direction.LEFT -> Cell((head.x - 1).takeIf { it >= 0 } ?: (gridSize - 1), head.y)
        Direction.RIGHT -> Cell((head.x + 1) % gridSize, head.y)
        else -> head
    }
    
    return listOf(newHead) + snake.dropLast(1)
}

@Composable
fun GameOverScreen(score: Int, highScore: Int, onRestart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "GAME OVER",
            color = Color.Red,
            fontSize = 32.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Text(
            text = "Final Score: $score",
            color = Color.White,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        if (score == highScore && score > 0) {
            Text(
                text = "New High Score!",
                color = Color.Yellow,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
        
        Button(
            onClick = onRestart,
            modifier = Modifier.padding(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60))
        ) {
            Text("Play Again", fontSize = 18.sp)
        }
    }
}

@Composable
fun GameBoard(
    snake: List<Cell>,
    food: Cell,
    specialFood: Cell?,
    villagers: List<Villager>,
    gridSize: Int,
    currentDirection: Direction,
    invincible: Boolean,
    onDirectionChange: (Direction) -> Unit
) {
    val cellSize = 20.dp
    val pulseAnim = remember { Animatable(1f) }
    
    LaunchedEffect(food) {
        pulseAnim.animateTo(1.2f, tween(500, easing = LinearEasing))
        pulseAnim.animateTo(1f, tween(500, easing = LinearEasing))
    }

    Column(
        modifier = Modifier
            .background(Color(0xFF34495E), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        for (y in 0 until gridSize) {
            Row {
                for (x in 0 until gridSize) {
                    val cell = Cell(x, y)
                    val isSnakeHead = cell == snake.first()
                    val isSnakeBody = cell in snake.drop(1)
                    val villager = villagers.find { it.position == cell }
                    
                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .border(BorderStroke(0.5.dp, Color(0xFF7F8C8D)))
                            .background(
                                when {
                                    isSnakeHead -> if (invincible) Color(0xFF9B59B6) else Color(0xFF27AE60)
                                    isSnakeBody -> if (invincible) Color(0xFF8E44AD) else Color(0xFF2ECC71)
                                    villager != null -> villager.type.color
                                    cell == food -> Color(0xFFE74C3C)
                                    cell == specialFood -> Color(0xFF9B59B6) // Purple for special food
                                    else -> Color(0xFF2C3E50)
                                }
                            )
                            .graphicsLayer {
                                scaleX = if (cell == food) pulseAnim.value else 1f
                                scaleY = if (cell == food) pulseAnim.value else 1f
                            }
                    ) {
                        if (villager != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp)
                                    .background(Color.White, CircleShape)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = villager.type.emoji,
                                    fontSize = (cellSize.value / 2).sp
                                )
                            }
                        } else if (isSnakeHead) {
                            // Snake eyes
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Draw eyes based on direction
                                val eyeOffset = when (currentDirection) {
                                    Direction.UP -> Pair(0.3f to 0.2f, 0.7f to 0.2f)
                                    Direction.DOWN -> Pair(0.3f to 0.8f, 0.7f to 0.8f)
                                    Direction.LEFT -> Pair(0.2f to 0.3f, 0.2f to 0.7f)
                                    Direction.RIGHT -> Pair(0.8f to 0.3f, 0.8f to 0.7f)
                                    else -> Pair(0.3f to 0.2f, 0.7f to 0.2f)
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .size(cellSize / 5)
                                        .align(Alignment.TopStart)
                                        .graphicsLayer {
                                            translationX = eyeOffset.first.first * cellSize.value
                                            translationY = eyeOffset.first.second * cellSize.value
                                        }
                                        .background(Color.Black, CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(cellSize / 5)
                                        .align(Alignment.TopStart)
                                        .graphicsLayer {
                                            translationX = eyeOffset.second.first * cellSize.value
                                            translationY = eyeOffset.second.second * cellSize.value
                                        }
                                        .background(Color.Black, CircleShape)
                                )
                            }
                        } else if (cell == specialFood) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp)
                                    .background(Color.White, CircleShape)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⭐",
                                    fontSize = (cellSize.value / 2).sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Controls(currentDirection: Direction, onDirectionChange: (Direction) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Up button
        ControlButton(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = "Up",
            enabled = currentDirection != Direction.DOWN
        ) {
            onDirectionChange(Direction.UP)
        }
        
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            // Left button
            ControlButton(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Left",
                enabled = currentDirection != Direction.RIGHT
            ) {
                onDirectionChange(Direction.LEFT)
            }

            Spacer(modifier = Modifier.width(60.dp))

            // Right button
            ControlButton(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Right",
                enabled = currentDirection != Direction.LEFT
            ) {
                onDirectionChange(Direction.RIGHT)
            }
        }
        
        // Down button
        ControlButton(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = "Down",
            enabled = currentDirection != Direction.UP
        ) {
            onDirectionChange(Direction.DOWN)
        }
    }
}

@Composable
fun ControlButton(
    imageVector: ImageVector,
    contentDescription: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(if (enabled) Color(0xFF3498DB) else Color.Gray),
        onClick = onClick,
        enabled = enabled
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(40.dp)
        )
    }
}

fun generateVillagers(gridSize: Int, count: Int): List<Villager> {
    return List(count) {
        val x = Random.nextInt(2, gridSize - 2)
        val y = Random.nextInt(2, gridSize - 2)
        val type = VillagerType.values().random()
        val pattern = MovementPattern.values().random()
        Villager(Cell(x, y), type, pattern)
    }
}

fun moveVillagers(villagers: List<Villager>, snakeHead: Cell, gridSize: Int): List<Villager> {
    return villagers.map { villager ->
        val newPosition = when (villager.movementPattern) {
            MovementPattern.RANDOM -> moveRandom(villager.position, gridSize)
            MovementPattern.CIRCULAR -> moveCircular(villager.position, gridSize)
            MovementPattern.FLEEING -> moveFleeing(villager.position, snakeHead, gridSize)
            MovementPattern.PATROL -> movePatrol(villager.position, gridSize, System.currentTimeMillis())
        }
        villager.copy(position = newPosition)
    }
}

fun moveRandom(position: Cell, gridSize: Int): Cell {
    val directions = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
    val (dx, dy) = directions.random()
    return Cell(
        (position.x + dx).coerceIn(0, gridSize - 1),
        (position.y + dy).coerceIn(0, gridSize - 1)
    )
}

fun moveFleeing(position: Cell, snakeHead: Cell, gridSize: Int): Cell {
    // Move away from snake
    val dx = if (position.x < snakeHead.x) -1 else if (position.x > snakeHead.x) 1 else 0
    val dy = if (position.y < snakeHead.y) -1 else if (position.y > snakeHead.y) 1 else 0
    
    return Cell(
        (position.x - dx).coerceIn(0, gridSize - 1),
        (position.y - dy).coerceIn(0, gridSize - 1)
    )
}

fun moveCircular(position: Cell, gridSize: Int): Cell {
    // Simple circular pattern
    val angle = System.currentTimeMillis() / 1000.0
    val dx = (cos(angle) * 1.5).toInt()
    val dy = (sin(angle) * 1.5).toInt()
    
    return Cell(
        (position.x + dx).coerceIn(0, gridSize - 1),
        (position.y + dy).coerceIn(0, gridSize - 1)
    )
}

fun movePatrol(position: Cell, gridSize: Int, time: Long): Cell {
    // Simple patrol pattern
    val direction = ((time / 2000) % 4).toInt()
    return when (direction) {
        0 -> Cell((position.x + 1).coerceAtMost(gridSize - 1), position.y)
        1 -> Cell(position.x, (position.y + 1).coerceAtMost(gridSize - 1))
        2 -> Cell((position.x - 1).coerceAtLeast(0), position.y)
        else -> Cell(position.x, (position.y - 1).coerceAtLeast(0))
    }
}

fun generateFood(occupiedCells: List<Cell>, gridSize: Int): Cell {
    val emptyCells = (0 until gridSize).flatMap { x ->
        (0 until gridSize).map { y -> Cell(x, y) }
    }.filter { it !in occupiedCells }
    
    return if (emptyCells.isNotEmpty()) {
        emptyCells[Random.nextInt(emptyCells.size)]
    } else {
        // Fallback if no empty cells (shouldn't happen in normal gameplay)
        Cell(Random.nextInt(gridSize), Random.nextInt(gridSize))
    }
}

fun growSnake(snake: List<Cell>): List<Cell> {
    val tail = snake.last()
    val secondLast = snake.dropLast(1).last()
    
    // Determine growth direction based on the last two segments
    val dx = tail.x - secondLast.x
    val dy = tail.y - secondLast.y
    
    val newTail = Cell(tail.x + dx, tail.y + dy)
    return snake + newTail
}