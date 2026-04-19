package com.example.familyvault.security

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinScreen(onSuccess: () -> Unit) {
    val darkColorScheme = darkColorScheme(
        primary = Color(0xFF4CAF50),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF1B5E20),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        onBackground = Color(0xFFE0E0E0),
        onSurface = Color(0xFFE0E0E0),
        error = Color(0xFFCF6679)
    )

    MaterialTheme(colorScheme = darkColorScheme) {
        PinScreenContent(onSuccess = onSuccess)
    }
}

@Composable
fun PinScreenContent(onSuccess: () -> Unit) {
    val fixedPin = "00000"

    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var attempts by remember { mutableStateOf(0) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var shakeAnimation by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val maxAttempts = 3
    val isLocked = attempts >= maxAttempts

    // Shake animation for error
    val shakeOffset by animateDpAsState(
        targetValue = if (shakeAnimation) 5.dp else 0.dp,
        animationSpec = repeatable(
            iterations = 3,
            animation = tween(durationMillis = 50, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .offset(x = shakeOffset),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo/Icon
            AnimatedContent(
                targetState = isLocked,
                transitionSpec = {
                    fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                },
                label = "icon"
            ) { locked ->
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            if (locked)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (locked) Icons.Outlined.Lock else Icons.Outlined.Security,
                        contentDescription = null,
                        tint = if (locked)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                if (isLocked) "Access Denied" else "Welcome Back",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle
            Text(
                if (isLocked)
                    "Too many failed attempts.\nPlease try again later."
                else
                    "Enter your PIN to access Family Vault",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!isLocked) {
                // PIN Input Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // PIN Dots
                        Row(
                            modifier = Modifier.padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            repeat(5) { index ->
                                PinDot(
                                    filled = index < pin.length,
                                    color = if (error.isNotEmpty())
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // PIN Input Field
                        OutlinedTextField(
                            value = pin,
                            onValueChange = {
                                if (it.length <= 5 && it.all { ch -> ch.isDigit() }) {
                                    pin = it
                                    error = ""
                                    if (it.length == 5) {
                                        focusManager.clearFocus()
                                    }
                                }
                            },
                            label = {
                                Text(
                                    "Enter 5-digit PIN",
                                    fontSize = 14.sp
                                )
                            },
                            visualTransformation = if (isPasswordVisible)
                                VisualTransformation.None
                            else
                                PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (pin.length == 5) {
                                        verifyPin(pin, fixedPin, onSuccess, attempts,
                                            { attempts = it },
                                            { error = it },
                                            { shakeAnimation = it },
                                            { isLoading = it }
                                        )
                                    }
                                }
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                errorBorderColor = MaterialTheme.colorScheme.error
                            ),
                            isError = error.isNotEmpty(),
                            singleLine = true,
                            enabled = !isLoading
                        )

                        // Show/Hide PIN Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            TextButton(
                                onClick = { isPasswordVisible = !isPasswordVisible }
                            ) {
                                Icon(
                                    if (isPasswordVisible)
                                        Icons.Outlined.VisibilityOff
                                    else
                                        Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    if (isPasswordVisible) "Hide PIN" else "Show PIN",
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Error Message
                        AnimatedVisibility(
                            visible = error.isNotEmpty(),
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    error,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Continue Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        verifyPin(pin, fixedPin, onSuccess, attempts,
                            { attempts = it },
                            { error = it },
                            { shakeAnimation = it },
                            { isLoading = it }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = pin.length == 5 && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Attempts counter
                if (attempts > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Attempts remaining: ${maxAttempts - attempts}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Locked state button
                Button(
                    onClick = { /* Could implement a timer or reset mechanism */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    enabled = false
                ) {
                    Text(
                        "Please Wait",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun PinDot(filled: Boolean, color: Color) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(
                if (filled) color else color.copy(alpha = 0.2f)
            )
            .border(
                width = if (filled) 0.dp else 1.dp,
                color = color,
                shape = CircleShape
            )
            .animateContentSize()
    )
}

private fun verifyPin(
    pin: String,
    fixedPin: String,
    onSuccess: () -> Unit,
    currentAttempts: Int,
    onAttemptsUpdate: (Int) -> Unit,
    onErrorUpdate: (String) -> Unit,
    onShakeUpdate: (Boolean) -> Unit,
    onLoadingUpdate: (Boolean) -> Unit
) {
    if (pin.length != 5) {
        onErrorUpdate("Please enter 5-digit PIN")
        onShakeUpdate(true)
        return
    }

    onLoadingUpdate(true)

    // Simulate verification delay
    kotlinx.coroutines.GlobalScope.launch {
        delay(500)

        if (pin == fixedPin) {
            onSuccess()
            onLoadingUpdate(false)
        } else {
            val newAttempts = currentAttempts + 1
            onAttemptsUpdate(newAttempts)
            onErrorUpdate(if (newAttempts >= 3) "Too many attempts" else "Wrong PIN. Try again")
            onShakeUpdate(true)
            onLoadingUpdate(false)
        }
    }
}

// Helper extension for animations
fun Modifier.animateContentSize() = this.then(
    Modifier.animateContentSize(
        animationSpec = tween(durationMillis = 300)
    )
)