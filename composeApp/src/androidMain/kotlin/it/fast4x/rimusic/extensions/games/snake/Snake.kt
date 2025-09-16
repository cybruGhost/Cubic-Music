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
    UP, DOWN, LEFT, RIGHT
}

data class Villager(val position: Cell, val type: VillagerType, val movementPattern: MovementPattern)

enum class VillagerType(val color: Color, val emoji: String) {
    FARMER(Color(0xFF8B4513), "👨‍🌾"),      // Brown
    MERCHANT(Color(0xFF4682B4), "👨‍💼"),   // Steel Blue
    BLACKSMITH(Color(0xFF2F4F4F), "👨‍🏭"), // Dark Slate Gray
    BAKER(Color(0xFFFFD700), "👨‍🍳")       // Gold
}

enum class MovementPattern {
    RANDOM, CIRCULAR, FLEEING, PATROL
}

@Composable
fun SnakeGame() {
    val gridSize = 20 // Slightly smaller grid for better visibility
    var direction by remember { mutableStateOf(Direction.RIGHT) }
    var snake by remember { mutableStateOf(listOf(Cell(5, 5))) }
    var food by remember { mutableStateOf(generateFood(snake, gridSize)) }
    var villagers by remember { mutableStateOf(generateVillagers(gridSize, 4)) }
    var isGameOver by remember { mutableStateOf(false) }
    var gameId by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var gameSpeed by remember { mutableStateOf(50L) }
    var snakeSpeed by remember { mutableStateOf(200L) }
    var lastMoveTime by remember { mutableStateOf(0L) }
    var lastVillagerMoveTime by remember { mutableStateOf(0L) }
    var gamePaused by remember { mutableStateOf(false) }

    // Game loop
    LaunchedEffect(gameId) {
        while (!isGameOver) {
            if (!gamePaused) {
                delay(gameSpeed)
                
                val currentTime = System.currentTimeMillis()
                
                // Move snake
                if (currentTime - lastMoveTime >= snakeSpeed) {
                    snake = moveSnake(snake, direction, gridSize)
                    lastMoveTime = currentTime
                }
                
                // Move villagers every 500ms
                if (currentTime - lastVillagerMoveTime >= 500) {
                    villagers = moveVillagers(villagers, snake.first(), gridSize)
                    lastVillagerMoveTime = currentTime
                }
                
                // Check collisions
                val head = snake.first()
                if (head == food) {
                    food = generateFood(snake + villagers.map { it.position }, gridSize)
                    snake = growSnake(snake, direction, gridSize)
                    score += 10
                    // Increase speed slightly with each food
                    snakeSpeed = (snakeSpeed * 0.95).toLong().coerceAtLeast(100L)
                }
                
                // Check if caught a villager
                val caughtVillager = villagers.find { it.position == head }
                if (caughtVillager != null) {
                    villagers = villagers.filter { it != caughtVillager }
                    score += 50 // More points for catching villagers
                    // Add new villager
                    if (villagers.size < 8) {
                        villagers = villagers + generateVillagers(gridSize, 1)
                    }
                }
                
                isGameOver = checkGameOver(snake, gridSize) || villagers.isEmpty()
            }
            delay(16) // ~60 FPS
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF2C3E50))) {
        if (isGameOver) {
            GameOverScreen(score, onRestart = {
                snake = listOf(Cell(5, 5))
                direction = Direction.RIGHT
                food = generateFood(snake, gridSize)
                villagers = generateVillagers(gridSize, 4)
                isGameOver = false
                score = 0
                snakeSpeed = 200L
                gameId++
            })
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Score and controls
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Score: $score",
                        color = Color.White,
                        fontSize = 20.sp,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Button(
                        onClick = { gamePaused = !gamePaused },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(if (gamePaused) "Resume" else "Pause")
                    }
                }
                
                // Game board
                GameBoard(snake, food, villagers, gridSize, direction, { direction = it })
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Controls
                Controls(currentDirection = direction, onDirectionChange = { direction = it })
            }
        }
    }
}

fun moveSnake(snake: List<Cell>, direction: Direction, gridSize: Int): List<Cell> {
    val head = snake.first()
    val newHead = when (direction) {
        Direction.UP -> Cell(head.x, (head.y - 1 + gridSize) % gridSize)
        Direction.DOWN -> Cell(head.x, (head.y + 1) % gridSize)
        Direction.LEFT -> Cell((head.x - 1 + gridSize) % gridSize, head.y)
        Direction.RIGHT -> Cell((head.x + 1) % gridSize, head.y)
    }
    val newSnake = snake.toMutableList()
    newSnake.add(0, newHead)
    newSnake.removeAt(newSnake.size - 1)
    return newSnake
}

@Composable
fun GameOverScreen(score: Int, onRestart: () -> Unit) {
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
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        Button(
            onClick = onRestart,
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Play Again", fontSize = 18.sp)
        }
    }
}

@Composable
fun GameBoard(
    snake: List<Cell>,
    food: Cell,
    villagers: List<Villager>,
    gridSize: Int,
    currentDirection: Direction,
    onDirectionChange: (Direction) -> Unit
) {
    val cellSize = 24.dp
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
                    val villager = villagers.find { it.position == cell }
                    
                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .border(BorderStroke(0.5.dp, Color(0xFF7F8C8D)))
                            .background(
                                when {
                                    isSnakeHead -> Color(0xFF27AE60) // Bright green for head
                                    cell in snake -> Color(0xFF2ECC71) // Snake body
                                    villager != null -> villager.type.color
                                    cell == food -> Color(0xFFE74C3C) // Red for food
                                    else -> Color(0xFF2C3E50) // Dark background
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
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .size(cellSize / 4)
                                        .align(Alignment.TopStart)
                                        .graphicsLayer {
                                            translationX = eyeOffset.first.first * cellSize.value
                                            translationY = eyeOffset.first.second * cellSize.value
                                        }
                                        .background(Color.Black, CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(cellSize / 4)
                                        .align(Alignment.TopStart)
                                        .graphicsLayer {
                                            translationX = eyeOffset.second.first * cellSize.value
                                            translationY = eyeOffset.second.second * cellSize.value
                                        }
                                        .background(Color.Black, CircleShape)
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
            MovementPattern.PATROL -> movePatrol(villager.position, gridSize)
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

fun movePatrol(position: Cell, gridSize: Int): Cell {
    // Simple patrol pattern
    val time = System.currentTimeMillis() / 2000
    val dx = if (time % 4 < 2) 1 else -1
    
    return Cell(
        (position.x + dx).coerceIn(0, gridSize - 1),
        position.y
    )
}

fun generateFood(occupiedCells: List<Cell>, gridSize: Int): Cell {
    val emptyCells = (0 until gridSize).flatMap { x ->
        (0 until gridSize).map { y -> Cell(x, y) }
    }.filter { it !in occupiedCells }
    return emptyCells[Random.nextInt(emptyCells.size)]
}

fun growSnake(snake: List<Cell>, direction: Direction, gridSize: Int): List<Cell> {
    val tail = snake.last()
    val newTail = when (direction) {
        Direction.UP -> Cell(tail.x, (tail.y - 1 + gridSize) % gridSize)
        Direction.DOWN -> Cell(tail.x, (tail.y + 1) % gridSize)
        Direction.LEFT -> Cell((tail.x - 1 + gridSize) % gridSize, tail.y)
        Direction.RIGHT -> Cell((tail.x + 1) % gridSize, tail.y)
    }
    return snake + newTail
}

fun checkGameOver(snake: List<Cell>, gridSize: Int): Boolean {
    val head = snake.first()
    return head in snake.drop(1) || 
           head.x < 0 || head.y < 0 || 
           head.x >= gridSize || head.y >= gridSize
}