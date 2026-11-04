package com.example.ui.easteregg

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FmPureWhite
import com.example.ui.theme.FmRed
import com.example.ui.theme.FmSlate
import com.example.ui.theme.FmSlateDark
import com.example.ui.theme.FmSlateMuted
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Android11EasterEgg(
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val view = LocalView.current
    var knobAngle by remember { mutableFloatStateOf(0f) } // 0 to 300 degrees
    var currentLevel by remember { mutableIntStateOf(1) }
    var reachedEleven by remember { mutableStateOf(false) }

    // Audio/visual particle effect
    val scope = rememberCoroutineScope()
    val celebrationScale = remember { Animatable(0f) }

    // Map angle to dial step 1..11
    fun updateAngle(deltaDegrees: Float) {
        val newAngle = (knobAngle + deltaDegrees).coerceIn(0f, 300f)
        val newLevel = (1 + (newAngle / 300f * 10f)).toInt().coerceIn(1, 11)

        if (newLevel != currentLevel) {
            currentLevel = newLevel
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)

            if (newLevel == 11 && !reachedEleven) {
                reachedEleven = true
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                scope.launch {
                    celebrationScale.snapTo(0f)
                    celebrationScale.animateTo(
                        1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
            }
        }
        knobAngle = newAngle
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Android 11 Easter Egg",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = FmSlateDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = FmSlateDark)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        knobAngle = 0f
                        currentLevel = 1
                        reachedEleven = false
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = FmSlate)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FmPureWhite)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = if (reachedEleven) "11" else "$currentLevel",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    color = if (reachedEleven) FmRed else FmSlateDark,
                    letterSpacing = (-2).sp
                )
                Text(
                    text = if (reachedEleven) "Turned up to 11! Android 11 Cat Unlocked" else "Turn the dial to 11...",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (reachedEleven) FmRed else FmSlateMuted,
                    textAlign = TextAlign.Center
                )
            }

            // Dial Container
            Box(
                modifier = Modifier
                    .size(310.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Dial track and numbers
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.width / 2f - 26.dp.toPx()

                    // Draw outer subtle ring
                    drawCircle(
                        color = Color(0xFFE2E8F0),
                        radius = radius,
                        center = center,
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw tick marks for 1 to 11
                    // 0 deg start at bottom left (~120 deg standard clock: from 120 to 420 = 300 deg sweep)
                    val startAngle = 120.0
                    val sweepAngle = 300.0

                    for (i in 1..11) {
                        val fraction = (i - 1) / 10.0
                        val angleDeg = startAngle + (fraction * sweepAngle)
                        val angleRad = Math.toRadians(angleDeg)

                        val isPassed = i <= currentLevel
                        val tickColor = if (isPassed) {
                            if (reachedEleven) Color(0xFFE53935) else Color(0xFF2563EB)
                        } else {
                            Color(0xFFCBD5E1)
                        }

                        val innerR = radius - (if (i == 11) 16.dp.toPx() else 10.dp.toPx())
                        val outerR = radius + 6.dp.toPx()

                        val startX = (center.x + innerR * cos(angleRad)).toFloat()
                        val startY = (center.y + innerR * sin(angleRad)).toFloat()
                        val endX = (center.x + outerR * cos(angleRad)).toFloat()
                        val endY = (center.y + outerR * sin(angleRad)).toFloat()

                        drawLine(
                            color = tickColor,
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = if (i == 11 || i == 1) 4.dp.toPx() else 2.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Rotary Knob
                var lastPointerAngle by remember { mutableFloatStateOf(0f) }

                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .shadow(16.dp, CircleShape, spotColor = Color(0x33000000))
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFFFFF),
                                    Color(0xFFF1F5F9),
                                    Color(0xFFE2E8F0)
                                )
                            )
                        )
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    lastPointerAngle = Math.toDegrees(
                                        atan2((offset.y - centerY).toDouble(), (offset.x - centerX).toDouble())
                                    ).toFloat()
                                },
                                onDrag = { change, _ ->
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    val currentAngle = Math.toDegrees(
                                        atan2((change.position.y - centerY).toDouble(), (change.position.x - centerX).toDouble())
                                    ).toFloat()

                                    var delta = currentAngle - lastPointerAngle
                                    if (delta > 180f) delta -= 360f
                                    if (delta < -180f) delta += 360f

                                    updateAngle(delta * 1.1f)
                                    lastPointerAngle = currentAngle
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Inner knob circle
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFFFFF),
                                        Color(0xFFF8FAFC)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Indicator Pip at the perimeter of the knob
                        val pipRotation = knobAngle + 120f
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(pipRotation),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 10.dp)
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (reachedEleven) FmRed else Color(0xFF2563EB)
                                    )
                            )
                        }

                        // Center Emblem
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (reachedEleven) {
                                Icon(
                                    imageVector = Icons.Default.Pets,
                                    contentDescription = "Cat",
                                    tint = FmRed,
                                    modifier = Modifier.size(38.dp)
                                )
                            } else {
                                Text(
                                    text = "$currentLevel",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FmSlate
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Easter Egg celebration info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                AnimatedVisibility(
                    visible = reachedEleven,
                    enter = fadeIn() + scaleIn()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = FmPureWhite),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFEBEE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Celebration,
                                    contentDescription = "Celebration",
                                    tint = FmRed,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Android 11 Unlocked!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = FmSlateDark
                                )
                                Text(
                                    text = "You turned it all the way up to 11! \uD83D\uDC31 Powered by FmPDF v1.0",
                                    fontSize = 13.sp,
                                    color = FmSlateMuted
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FmSlateDark)
                ) {
                    Text("Return to FmPDF", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
        }
    }
}
