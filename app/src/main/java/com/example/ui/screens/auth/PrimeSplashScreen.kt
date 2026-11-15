package com.example.ui.screens.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.PrimeBlue
import com.example.ui.theme.PrimeBorder
import com.example.ui.theme.PrimeCardBackground
import com.example.ui.theme.PrimeDarkBackground
import com.example.ui.theme.PrimeRed
import com.example.ui.theme.PrimeTextMuted
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary

@Composable
fun PrimeSplashScreen(
    statusText: String = "Loading your PRIME ESPORT profile...",
    errorMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    onSignOut: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_ring")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )

    Surface(
        color = PrimeDarkBackground,
        modifier = Modifier
            .fillMaxSize()
            .testTag("prime_splash_screen")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Branded PRIME ESPORT splash artwork (crowned P, PRIME ESPORT, PLAY • CONNECT • COMPETE)
            Image(
                painter = painterResource(id = R.drawable.prime_splash_bg),
                contentDescription = "PRIME ESPORT Splash",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom loading ring / error & retry controls aligned over the bottom loading region
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.62f to Color.Transparent,
                            1.0f to Color(0xFF05070E).copy(alpha = 0.92f)
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 40.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 460.dp)
                ) {
                    if (errorMessage == null) {
                        Canvas(modifier = Modifier.size(44.dp)) {
                            val strokeWidth = 4.dp.toPx()
                            drawCircle(
                                color = Color.White.copy(alpha = 0.09f),
                                style = Stroke(width = strokeWidth)
                            )
                            rotate(ringRotation) {
                                drawArc(
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            PrimeBlue,
                                            PrimeBlue.copy(alpha = 0.25f),
                                            PrimeRed,
                                            PrimeRed
                                        )
                                    ),
                                    startAngle = 0f,
                                    sweepAngle = 290f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = statusText,
                            color = PrimeTextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Surface(
                            color = PrimeCardBackground.copy(alpha = 0.96f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, PrimeBorder, RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(18.dp)
                            ) {
                                Text(
                                    text = errorMessage,
                                    color = PrimeRed,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (onRetry != null) {
                                        Button(
                                            onClick = onRetry,
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimeBlue),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("splash_retry_button")
                                        ) {
                                            Text("Retry", fontWeight = FontWeight.Bold, color = PrimeTextPrimary)
                                        }
                                    }
                                    if (onSignOut != null) {
                                        OutlinedButton(
                                            onClick = onSignOut,
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("Sign Out", color = PrimeTextMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
