package com.example.ui.dialogs

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pattern
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.StoreSettings
import com.example.ui.screens.PatternLockView
import com.example.ui.theme.DebtRed
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ProfitGreen

@Composable
fun ChangePasscodeDialog(
    settings: StoreSettings,
    onDismiss: () -> Unit,
    onSave: (StoreSettings) -> Unit
) {
    val context = LocalContext.current

    var isLockEnabled by remember { mutableStateOf(settings.isAppLockEnabled) }
    var selectedLockType by remember { mutableStateOf(settings.lockType) }
    var isBiometricEnabled by remember { mutableStateOf(settings.isBiometricEnabled) }

    var currentPasscodeInput by remember { mutableStateOf("") }
    var newPasscodeInput by remember { mutableStateOf("") }
    var confirmPasscodeInput by remember { mutableStateOf("") }

    var isPasswordVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val lockOptions = listOf(
        Triple("PIN_4", "رمز PIN (4 أرقام)", Icons.Default.Pin),
        Triple("PIN_6", "رمز PIN (6 أرقام)", Icons.Default.Pin),
        Triple("PASSWORD", "كلمة سر مخصصة", Icons.Default.Lock),
        Triple("PATTERN", "نمط قفل", Icons.Default.Pattern)
    )

    fun handleSave() {
        errorMsg = ""
        // Check current passcode if lock is currently enabled
        if (settings.isAppLockEnabled && currentPasscodeInput != settings.passcode) {
            errorMsg = "رمز الدخول الحالي غير صحيح!"
            return
        }

        if (newPasscodeInput.isNotBlank()) {
            when (selectedLockType) {
                "PIN_4" -> {
                    if (newPasscodeInput.length != 4 || !newPasscodeInput.all { it.isDigit() }) {
                        errorMsg = "رمز PIN يجب أن يتكون من 4 أرقام!"
                        return
                    }
                }
                "PIN_6" -> {
                    if (newPasscodeInput.length != 6 || !newPasscodeInput.all { it.isDigit() }) {
                        errorMsg = "رمز PIN يجب أن يتكون من 6 أرقام!"
                        return
                    }
                }
                "PASSWORD" -> {
                    if (newPasscodeInput.length < 3) {
                        errorMsg = "كلمة المرور يجب أن تكون 3 أحرف على الأقل!"
                        return
                    }
                }
                "PATTERN" -> {
                    if (newPasscodeInput.length < 3) {
                        errorMsg = "يجب توصيل 3 نقاط على الأقل للنمط!"
                        return
                    }
                }
            }

            if (newPasscodeInput != confirmPasscodeInput) {
                errorMsg = "تأكيد الرمز غير متطابق!"
                return
            }
        }

        val finalPasscode = if (newPasscodeInput.isNotBlank()) newPasscodeInput else settings.passcode
        val updated = settings.copy(
            isAppLockEnabled = isLockEnabled,
            lockType = selectedLockType,
            passcode = finalPasscode,
            isBiometricEnabled = isBiometricEnabled
        )
        onSave(updated)
        Toast.makeText(context, "تم حفظ إعدادات الأمان وقفل التطبيق بنجاح", Toast.LENGTH_SHORT).show()
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "أمان وقفل التطبيق",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "إغلاق")
                    }
                }

                // 1. Enable/Disable Lock Switch Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLockEnabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تفعيل القفل عند فتح التطبيق",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "طلب الرمز السري أو البصمة في كل مرة يتم فتح البرنامج",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isLockEnabled,
                            onCheckedChange = { isLockEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                        )
                    }
                }

                // 2. Biometric Fingerprint Switch
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "الدخول ببصمة الإصبع",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "فتح التطبيق سريعاً بمستشعر البصمة",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { isBiometricEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = EmeraldPrimary)
                        )
                    }
                }

                // 3. Select Lock Type
                Text(
                    text = "نوع الحماية والقفل:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    lockOptions.forEach { (typeKey, typeLabel, typeIcon) ->
                        val isSelected = selectedLockType == typeKey
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedLockType = typeKey },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(EmeraldPrimary)
                            ) else CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = typeIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = typeLabel,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 14.sp
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Change Passcode Fields
                Text(
                    text = "تغيير أو تحديث الرمز السري:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (settings.isAppLockEnabled) {
                    OutlinedTextField(
                        value = currentPasscodeInput,
                        onValueChange = { currentPasscodeInput = it },
                        label = { Text("رمز الدخول الحالي *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (selectedLockType == "PATTERN") {
                    Text(
                        text = "ارسم النمط الجديد أدناه:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )

                    PatternLockView(
                        onPatternEntered = { patternStr ->
                            if (newPasscodeInput.isEmpty()) {
                                newPasscodeInput = patternStr
                                Toast.makeText(context, "تم تسجيل النمط. أعد رسمه للتأكيد.", Toast.LENGTH_SHORT).show()
                            } else {
                                confirmPasscodeInput = patternStr
                                if (newPasscodeInput == confirmPasscodeInput) {
                                    Toast.makeText(context, "النمط متطابق وجاهز للحفظ!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "النمط غير متطابق، حاول مجدداً", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )

                    if (newPasscodeInput.isNotEmpty()) {
                        Text(
                            text = if (newPasscodeInput == confirmPasscodeInput) "✔ تم تأكيد النمط بنجاح" else "⏳ يرجى رسم نفس النمط للتأكيد",
                            color = if (newPasscodeInput == confirmPasscodeInput) ProfitGreen else MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = newPasscodeInput,
                        onValueChange = { newPasscodeInput = it },
                        label = {
                            Text(
                                when (selectedLockType) {
                                    "PIN_4" -> "رمز PIN الجديد (4 أرقام)"
                                    "PIN_6" -> "رمز PIN الجديد (6 أرقام)"
                                    else -> "كلمة المرور الجديدة"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = confirmPasscodeInput,
                        onValueChange = { confirmPasscodeInput = it },
                        label = { Text("تأكيد الرمز الجديد") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (errorMsg.isNotBlank()) {
                    Text(
                        text = errorMsg,
                        color = DebtRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = { handleSave() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_passcode_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حفظ إعدادات الأمان", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
