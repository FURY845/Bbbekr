package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StoreSettings
import com.example.ui.theme.DebtRed
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.ProfitGreen
import com.example.util.BiometricAuthHelper
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun LockScreen(
    storeSettings: StoreSettings,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    var enteredPasscode by remember { mutableStateOf("") }
    var passwordText by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val shakeOffset = remember { Animatable(0f) }

    val requiredDigits = when (storeSettings.lockType) {
        "PIN_6" -> 6
        else -> 4
    }

    fun triggerShake(msg: String) {
        errorMessage = msg
        isError = true
        scope.launch {
            shakeOffset.animateTo(20f, tween(50))
            shakeOffset.animateTo(-20f, tween(50))
            shakeOffset.animateTo(15f, tween(50))
            shakeOffset.animateTo(-15f, tween(50))
            shakeOffset.animateTo(0f, tween(50))
        }
    }

    fun verifyPasscode(input: String) {
        if (input == storeSettings.passcode) {
            isError = false
            onUnlocked()
        } else {
            triggerShake("الرمز السري غير صحيح!")
            enteredPasscode = ""
        }
    }

    fun launchBiometric() {
        if (activity != null && storeSettings.isBiometricEnabled) {
            BiometricAuthHelper.authenticate(
                activity = activity,
                title = "المصادقة ببصمة الإصبع",
                subtitle = storeSettings.storeName,
                onSuccess = {
                    onUnlocked()
                },
                onError = { err ->
                    if (!err.contains("إلغاء")) {
                        errorMessage = err
                    }
                }
            )
        }
    }

    // Auto-prompt biometric when screen opens if biometric is enabled
    LaunchedEffect(Unit) {
        if (storeSettings.isBiometricEnabled && activity != null && BiometricAuthHelper.isBiometricSupported(context)) {
            launchBiometric()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. App Header & Store Identity
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(EmeraldPrimary, GoldAccent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isError) Icons.Default.Lock else Icons.Default.Store,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = storeSettings.storeName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when (storeSettings.lockType) {
                        "PIN_4" -> "أدخل رمز PIN المكون من 4 أرقام"
                        "PIN_6" -> "أدخل رمز PIN المكون من 6 أرقام"
                        "PASSWORD" -> "أدخل كلمة المرور لفتح التطبيق"
                        "PATTERN" -> "ارسم نمط القفل لفتح التطبيق"
                        else -> "أدخل الرمز السري للمتابعة"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isError) DebtRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (errorMessage.isNotBlank() && isError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage,
                        color = DebtRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 2. Middle Input Body based on Lock Type
            when (storeSettings.lockType) {
                "PASSWORD" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = passwordText,
                            onValueChange = {
                                passwordText = it
                                isError = false
                            },
                            label = { Text("كلمة المرور") },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(
                                        imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "إظهار/إخفاء"
                                    )
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { verifyPasscode(passwordText) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { verifyPasscode(passwordText) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("unlock_password_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("فتح التطبيق", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        if (storeSettings.isBiometricEnabled && activity != null && BiometricAuthHelper.isBiometricSupported(context)) {
                            Spacer(modifier = Modifier.height(16.dp))
                            IconButton(
                                onClick = { launchBiometric() },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "بصمة الإصبع",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }

                "PATTERN" -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PatternLockView(
                            onPatternEntered = { patternStr ->
                                verifyPasscode(patternStr)
                            }
                        )

                        if (storeSettings.isBiometricEnabled && activity != null && BiometricAuthHelper.isBiometricSupported(context)) {
                            Spacer(modifier = Modifier.height(12.dp))
                            IconButton(
                                onClick = { launchBiometric() },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "بصمة الإصبع",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }

                else -> {
                    // PIN 4 or PIN 6
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Pin Dots Indicator
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            repeat(requiredDigits) { index ->
                                val isFilled = index < enteredPasscode.length
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isError) DebtRed
                                            else if (isFilled) EmeraldPrimary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isError) DebtRed else if (isFilled) EmeraldPrimary else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Keypad Grid
                        PinKeypad(
                            showBiometric = storeSettings.isBiometricEnabled && activity != null && BiometricAuthHelper.isBiometricSupported(context),
                            onDigitClick = { digit ->
                                if (enteredPasscode.length < requiredDigits) {
                                    isError = false
                                    val updated = enteredPasscode + digit
                                    enteredPasscode = updated
                                    if (updated.length == requiredDigits) {
                                        verifyPasscode(updated)
                                    }
                                }
                            },
                            onDeleteClick = {
                                if (enteredPasscode.isNotEmpty()) {
                                    isError = false
                                    enteredPasscode = enteredPasscode.dropLast(1)
                                }
                            },
                            onBiometricClick = {
                                launchBiometric()
                            }
                        )
                    }
                }
            }

            // Bottom Security Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "قاعدة بيانات مشفرة ومحمية محلياً",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun PinKeypad(
    showBiometric: Boolean,
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onBiometricClick: () -> Unit
) {
    var lastClickTimestamp by remember { mutableStateOf(0L) }

    fun safeDigitClick(digit: String) {
        val now = System.currentTimeMillis()
        if (now - lastClickTimestamp > 180L) {
            lastClickTimestamp = now
            onDigitClick(digit)
        }
    }

    fun safeDeleteClick() {
        val now = System.currentTimeMillis()
        if (now - lastClickTimestamp > 180L) {
            lastClickTimestamp = now
            onDeleteClick()
        }
    }

    fun safeBiometricClick() {
        val now = System.currentTimeMillis()
        if (now - lastClickTimestamp > 300L) {
            lastClickTimestamp = now
            onBiometricClick()
        }
    }

    val digits = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("BIO", "0", "DEL")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        digits.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { item ->
                    when (item) {
                        "BIO" -> {
                            if (showBiometric) {
                                Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable { safeBiometricClick() }
                                        .testTag("keypad_biometric_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = "بصمة",
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(68.dp))
                            }
                        }

                        "DEL" -> {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { safeDeleteClick() }
                                    .testTag("keypad_delete_btn"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "حذف",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        else -> {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant,
                                        CircleShape
                                    )
                                    .clickable { safeDigitClick(item) }
                                    .testTag("keypad_digit_$item"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3x3 Pattern Lock View
 */
@Composable
fun PatternLockView(
    onPatternEntered: (String) -> Unit
) {
    val selectedNodes = remember { mutableStateListOf<Int>() }
    var currentTouchPos by remember { mutableStateOf<Offset?>(null) }
    val nodeRadius = 14.dp
    val primaryColor = EmeraldPrimary
    val outlineColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(260.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            selectedNodes.clear()
                            val node = findNodeAt(offset, size.width, size.height)
                            if (node != null && !selectedNodes.contains(node)) {
                                selectedNodes.add(node)
                            }
                            currentTouchPos = offset
                        },
                        onDrag = { change, _ ->
                            val offset = change.position
                            currentTouchPos = offset
                            val node = findNodeAt(offset, size.width, size.height)
                            if (node != null && !selectedNodes.contains(node)) {
                                selectedNodes.add(node)
                            }
                        },
                        onDragEnd = {
                            currentTouchPos = null
                            if (selectedNodes.isNotEmpty()) {
                                onPatternEntered(selectedNodes.joinToString("-"))
                            }
                        },
                        onDragCancel = {
                            currentTouchPos = null
                            selectedNodes.clear()
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stepX = size.width / 3f
                val stepY = size.height / 3f

                // Draw connecting lines
                if (selectedNodes.size > 1) {
                    for (i in 0 until selectedNodes.size - 1) {
                        val nodeA = selectedNodes[i]
                        val nodeB = selectedNodes[i + 1]
                        val posA = getNodeCenter(nodeA, stepX, stepY)
                        val posB = getNodeCenter(nodeB, stepX, stepY)
                        drawLine(
                            color = primaryColor,
                            start = posA,
                            end = posB,
                            strokeWidth = 10f,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Draw line to current touch
                if (selectedNodes.isNotEmpty() && currentTouchPos != null) {
                    val lastNode = selectedNodes.last()
                    val posLast = getNodeCenter(lastNode, stepX, stepY)
                    drawLine(
                        color = primaryColor.copy(alpha = 0.6f),
                        start = posLast,
                        end = currentTouchPos!!,
                        strokeWidth = 8f,
                        cap = StrokeCap.Round
                    )
                }

                // Draw 9 Nodes
                for (index in 0 until 9) {
                    val center = getNodeCenter(index, stepX, stepY)
                    val isSelected = selectedNodes.contains(index)

                    // Outer circle
                    drawCircle(
                        color = if (isSelected) primaryColor else outlineColor,
                        radius = 28f,
                        center = center
                    )
                    // Inner dot
                    drawCircle(
                        color = if (isSelected) Color.White else primaryColor.copy(alpha = 0.8f),
                        radius = if (isSelected) 14f else 8f,
                        center = center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedNodes.isNotEmpty()) {
            Button(
                onClick = {
                    onPatternEntered(selectedNodes.joinToString("-"))
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("تأكيد النمط", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun getNodeCenter(index: Int, stepX: Float, stepY: Float): Offset {
    val row = index / 3
    val col = index % 3
    val x = (col * stepX) + (stepX / 2f)
    val y = (row * stepY) + (stepY / 2f)
    return Offset(x, y)
}

private fun findNodeAt(touch: Offset, width: Int, height: Int): Int? {
    val stepX = width / 3f
    val stepY = height / 3f
    val touchRadius = 60f

    for (index in 0 until 9) {
        val center = getNodeCenter(index, stepX, stepY)
        val dx = touch.x - center.x
        val dy = touch.y - center.y
        if (sqrt((dx * dx + dy * dy).toDouble()) <= touchRadius) {
            return index
        }
    }
    return null
}
