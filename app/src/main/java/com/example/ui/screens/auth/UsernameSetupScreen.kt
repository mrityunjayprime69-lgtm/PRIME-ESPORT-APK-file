package com.example.ui.screens.auth

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthRepository
import com.example.data.repository.UsernameCheckResult
import com.example.ui.theme.PrimeBlue
import com.example.ui.theme.PrimeBorder
import com.example.ui.theme.PrimeCardBackground
import com.example.ui.theme.PrimeDarkBackground
import com.example.ui.theme.PrimeNavySurface
import com.example.ui.theme.PrimePurple
import com.example.ui.theme.PrimeRed
import com.example.ui.theme.PrimeTextMuted
import com.example.ui.theme.PrimeTextPrimary
import com.example.ui.theme.PrimeTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun UsernameSetupScreen(
    authRepository: AuthRepository,
    onSetupSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var checkStatus by remember { mutableStateOf<UsernameCheckResult>(UsernameCheckResult.Empty) }
    var isChecking by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var submitError by remember { mutableStateOf<String?>(null) }

    // Live debounced availability check
    LaunchedEffect(username) {
        val trimmed = username.trim()
        if (trimmed.isEmpty()) {
            checkStatus = UsernameCheckResult.Empty
            isChecking = false
            return@LaunchedEffect
        }
        isChecking = true
        delay(350) // 350ms debounce
        val result = authRepository.checkUsernameAvailability(trimmed)
        checkStatus = result
        isChecking = false
    }

    Surface(
        color = PrimeDarkBackground,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Monogram Badge
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(PrimeNavySurface, PrimeBlue.copy(alpha = 0.6f), PrimePurple.copy(alpha = 0.6f))
                            )
                        )
                        .border(2.dp, PrimeBlue, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "👑", fontSize = 20.sp)
                        Text(
                            text = "P",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title & Subtitle
                Text(
                    text = "Welcome to PRIME ESPORT!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = PrimeTextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Choose your unique gaming identity.",
                    fontSize = 14.sp,
                    color = PrimeBlue,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = 28.dp)
                )

                Surface(
                    color = PrimeNavySurface,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimeBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Username Field
                        Text(
                            text = "Gamer Username *",
                            color = PrimeTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = username,
                            onValueChange = { input ->
                                // Filter to valid characters: letters, numbers, underscores (max 20)
                                if (input.length <= 20) {
                                    username = input.filter { it.isLetterOrDigit() || it == '_' }
                                }
                            },
                            placeholder = { Text("e.g. shadow_strike99", color = PrimeTextMuted, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = PrimeBlue)
                            },
                            trailingIcon = {
                                if (isChecking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = PrimeBlue
                                    )
                                } else {
                                    when (checkStatus) {
                                        is UsernameCheckResult.Available -> {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = "Available",
                                                tint = Color(0xFF10B981)
                                            )
                                        }
                                        is UsernameCheckResult.Taken,
                                        is UsernameCheckResult.Reserved,
                                        is UsernameCheckResult.InvalidFormat -> {
                                            Icon(
                                                Icons.Default.Error,
                                                contentDescription = "Error",
                                                tint = PrimeRed
                                            )
                                        }
                                        else -> {}
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_unique_username_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = PrimeCardBackground,
                                unfocusedContainerColor = PrimeCardBackground,
                                focusedBorderColor = when (checkStatus) {
                                    is UsernameCheckResult.Available -> Color(0xFF10B981)
                                    is UsernameCheckResult.Taken,
                                    is UsernameCheckResult.Reserved,
                                    is UsernameCheckResult.InvalidFormat -> PrimeRed
                                    else -> PrimeBlue
                                },
                                unfocusedBorderColor = PrimeBorder,
                                focusedTextColor = PrimeTextPrimary,
                                unfocusedTextColor = PrimeTextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Availability & Validation Feedback
                        AnimatedVisibility(visible = username.isNotBlank()) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                when (val status = checkStatus) {
                                    is UsernameCheckResult.Available -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Username '@${status.normalized}' is available!",
                                                color = Color(0xFF10B981),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                    is UsernameCheckResult.Taken -> {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Error,
                                                contentDescription = null,
                                                tint = PrimeRed,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = status.message,
                                                color = PrimeRed,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    is UsernameCheckResult.Reserved -> {
                                        Text(
                                            text = status.message,
                                            color = PrimeRed,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    is UsernameCheckResult.InvalidFormat -> {
                                        Text(
                                            text = status.message,
                                            color = PrimeRed,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    is UsernameCheckResult.Checking -> {
                                        Text(
                                            text = "Checking availability...",
                                            color = PrimeTextMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        }

                        Text(
                            text = "3-20 characters • Letters, numbers, and underscores only",
                            color = PrimeTextMuted,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 6.dp, bottom = 18.dp)
                        )

                        // Optional Display Name Field
                        Text(
                            text = "Display Name (Optional)",
                            color = PrimeTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            placeholder = { Text("e.g. Phoenix Apex", color = PrimeTextMuted, fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = PrimePurple)
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_display_name_optional_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = PrimeCardBackground,
                                unfocusedContainerColor = PrimeCardBackground,
                                focusedBorderColor = PrimePurple,
                                unfocusedBorderColor = PrimeBorder,
                                focusedTextColor = PrimeTextPrimary,
                                unfocusedTextColor = PrimeTextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (submitError != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                color = PrimeRed.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, PrimeRed.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = PrimeRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = submitError ?: "",
                                        color = PrimeRed,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        val isContinueEnabled = checkStatus is UsernameCheckResult.Available && !isSubmitting && !isChecking

                        // Continue Button
                        Button(
                            onClick = {
                                isSubmitting = true
                                submitError = null
                                coroutineScope.launch {
                                    val result = authRepository.claimUsernameAndCompleteProfile(
                                        rawUsername = username,
                                        rawDisplayName = displayName
                                    )
                                    isSubmitting = false
                                    if (result.isSuccess) {
                                        onSetupSuccess()
                                    } else {
                                        submitError = result.exceptionOrNull()?.localizedMessage ?: "Failed to claim username. Please try another."
                                    }
                                }
                            },
                            enabled = isContinueEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimeBlue,
                                disabledContainerColor = PrimeBlue.copy(alpha = 0.3f),
                                contentColor = Color.White,
                                disabledContentColor = Color.White.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("setup_continue_button")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                            } else {
                                Text(
                                    text = "Continue",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
