package com.example.ui.dialogs

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.StoreSettings
import com.example.ui.theme.DebtRed
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.AccountingViewModel
import com.example.util.BackupCryptoManager
import com.example.util.BackupPackage
import com.example.util.BackupPreviewInfo
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupImportDialog(
    settings: StoreSettings,
    viewModel: AccountingViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isPasswordVerified by remember { mutableStateOf(!settings.isAppLockEnabled) }
    var enteredPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    var encryptedInput by remember { mutableStateOf("") }
    var parsedBackup by remember { mutableStateOf<BackupPackage?>(null) }
    var previewInfo by remember { mutableStateOf<BackupPreviewInfo?>(null) }
    var decryptError by remember { mutableStateOf("") }

    // Import Mode: "MERGE" or "OVERWRITE"
    var importMode by remember { mutableStateOf("MERGE") }
    var isRestoring by remember { mutableStateOf(false) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val content = reader.readText()
                    encryptedInput = content.trim()
                    // Try to decrypt automatically
                    val decrypted = BackupCryptoManager.decryptBackup(encryptedInput)
                    if (decrypted != null) {
                        parsedBackup = decrypted
                        previewInfo = BackupCryptoManager.getPreviewInfo(decrypted)
                        decryptError = ""
                    } else {
                        decryptError = "الملف المختار غير صالح أو تالف أو تم إنشاؤه ببرنامج آخر!"
                    }
                }
            } catch (e: Exception) {
                decryptError = "تعذر قراءة الملف: ${e.localizedMessage}"
            }
        }
    }

    fun handleVerifyPassword() {
        if (enteredPassword == settings.passcode) {
            isPasswordVerified = true
            passwordError = ""
        } else {
            passwordError = "الرمز السري غير صحيح!"
        }
    }

    fun handleDecryptManual() {
        if (encryptedInput.isBlank()) {
            decryptError = "يرجى لصق الرمز المشفر أولاً!"
            return
        }
        val decrypted = BackupCryptoManager.decryptBackup(encryptedInput)
        if (decrypted != null) {
            parsedBackup = decrypted
            previewInfo = BackupCryptoManager.getPreviewInfo(decrypted)
            decryptError = ""
        } else {
            decryptError = "الرمز المشفر غير صحيح أو تالف! تأكد من نسخ الرمز كاملاً."
        }
    }

    fun executeRestore() {
        val backup = parsedBackup ?: return
        isRestoring = true
        scope.launch {
            try {
                viewModel.restoreBackupPackage(backup, isOverwrite = (importMode == "OVERWRITE"))
                Toast.makeText(context, "تم استيراد واستعادة البيانات بنجاح!", Toast.LENGTH_LONG).show()
                onSuccess()
                onDismiss()
            } catch (e: Exception) {
                decryptError = "حدث خطأ أثناء الاستعادة: ${e.localizedMessage}"
            } finally {
                isRestoring = false
            }
        }
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
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "وضع ملف معلومات (استيراد واسترجاع)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "إغلاق")
                    }
                }

                // Step 1: Security Verification
                if (!isPasswordVerified) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "التحقق الأمني مطلوب",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "أدخل الرمز السري للتطبيق لمتابعة وضع واسترجاع ملف المعلومات",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = enteredPassword,
                                onValueChange = {
                                    enteredPassword = it
                                    passwordError = ""
                                },
                                label = { Text("الرمز السري") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            )

                            if (passwordError.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = passwordError, color = DebtRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { handleVerifyPassword() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Text("تأكيد ومتابعة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (parsedBackup == null) {
                    // Step 2: Input Payload or Pick File
                    Text(
                        text = "اختر طريقة تزويد ملف المعلومات المشفر:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Option A: Choose file from device
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("اختيار ملف من الجهاز أو التنزيلات", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("أو الصق الكود المشفر هنا مباشرة:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Option B: Paste Text
                    OutlinedTextField(
                        value = encryptedInput,
                        onValueChange = {
                            encryptedInput = it
                            decryptError = ""
                        },
                        label = { Text("الصق رمز المعلومات المشفر") },
                        placeholder = { Text("ALHESAB_ENC_V1::...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (decryptError.isNotBlank()) {
                        Text(
                            text = decryptError,
                            color = DebtRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { handleDecryptManual() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("verify_decrypt_backup_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("فك التشفير وفحص البيانات", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                } else {
                    // Step 3: Decrypted Preview & Choose Merge/Overwrite
                    val info = previewInfo!!
                    val dateFormat = SimpleDateFormat("yyyy/MM/dd - hh:mm a", Locale.getDefault())
                    val formattedDate = dateFormat.format(Date(info.timestamp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = ProfitGreen.copy(alpha = 0.1f)),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تم فك تشفير الملف بنجاح!",
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen,
                                    fontSize = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = "تاريخ النسخة: $formattedDate", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "محتويات الملف:\n" +
                                        "• ${info.productsCount} منتج مسجل\n" +
                                        "• ${info.customersCount} عميل وحساب دين\n" +
                                        "• ${info.invoicesCount} فاتورة مبيعات\n" +
                                        "• ${info.safeTransactionsCount} حركة خزينة",
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Text(
                        text = "حدد طريقة الاستيراد:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Radio 1: Merge
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { importMode = "MERGE" },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (importMode == "MERGE") MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = importMode == "MERGE",
                                onClick = { importMode = "MERGE" },
                                colors = RadioButtonDefaults.colors(selectedColor = EmeraldPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("دمج مع البيانات الحالية (الموصى به)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("إضافة العناصر الجديدة وتحديث الموجود دون حذف أي بيانات أخرى", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Radio 2: Overwrite
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { importMode = "OVERWRITE" },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (importMode == "OVERWRITE") DebtRed.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = importMode == "OVERWRITE",
                                onClick = { importMode = "OVERWRITE" },
                                colors = RadioButtonDefaults.colors(selectedColor = DebtRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("استبدال شامل (مسح القديم وكتابة الملف)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DebtRed)
                                Text("حذف كل البيانات الحالية واستبدالها بمحتويات هذا الملف فقط", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (decryptError.isNotBlank()) {
                        Text(text = decryptError, color = DebtRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { executeRestore() },
                        enabled = !isRestoring,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("confirm_restore_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (importMode == "OVERWRITE") DebtRed else EmeraldPrimary)
                    ) {
                        if (isRestoring) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جارٍ معالجة وتطبيق البيانات...")
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (importMode == "OVERWRITE") "استبدال البيانات وتطبيق الملف" else "دمج البيانات وحفظها",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
