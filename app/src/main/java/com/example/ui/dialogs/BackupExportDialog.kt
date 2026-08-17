package com.example.ui.dialogs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.StoreSettings
import com.example.ui.theme.DebtRed
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.AccountingViewModel
import kotlinx.coroutines.launch

@Composable
fun BackupExportDialog(
    settings: StoreSettings,
    viewModel: AccountingViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isPasswordVerified by remember { mutableStateOf(!settings.isAppLockEnabled) }
    var enteredPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    // Granular selection flags
    var includeAll by remember { mutableStateOf(true) }
    var includeProducts by remember { mutableStateOf(true) }
    var includeCustomers by remember { mutableStateOf(true) }
    var includeTodayInvoicesOnly by remember { mutableStateOf(false) }
    var includeAllInvoices by remember { mutableStateOf(true) }
    var includeSafeTransactions by remember { mutableStateOf(true) }

    var isGenerating by remember { mutableStateOf(false) }
    var generatedEncryptedCode by remember { mutableStateOf<String?>(null) }

    fun handleVerifyPassword() {
        if (enteredPassword == settings.passcode) {
            isPasswordVerified = true
            passwordError = ""
        } else {
            passwordError = "الرمز السري غير صحيح!"
        }
    }

    fun handleGenerateBackup() {
        isGenerating = true
        scope.launch {
            val includedList = mutableListOf<String>()
            if (includeAll) {
                includedList.addAll(listOf("PRODUCTS", "CUSTOMERS", "INVOICES_ALL", "SAFE", "SETTINGS"))
            } else {
                if (includeProducts) includedList.add("PRODUCTS")
                if (includeCustomers) includedList.add("CUSTOMERS")
                if (includeTodayInvoicesOnly) includedList.add("INVOICES_TODAY")
                else if (includeAllInvoices) includedList.add("INVOICES_ALL")
                if (includeSafeTransactions) includedList.add("SAFE")
            }

            val encryptedPayload = viewModel.createEncryptedBackup(includedList)
            generatedEncryptedCode = encryptedPayload
            isGenerating = false
        }
    }

    fun shareEncryptedFile(payload: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, payload)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "إرسال ملف المعلومات المشفر")
        context.startActivity(shareIntent)
    }

    fun copyToClipboard(payload: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("AlHesab Backup", payload)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ رمز المعلومات المشفر للحافظة", Toast.LENGTH_SHORT).show()
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
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "سحب ملف المعلومات (تصدير مشفر)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "إغلاق")
                    }
                }

                // Step 1: Security Passcode Verification
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
                                text = "لحماية البيانات، أدخل الرمز السري للتطبيق لمتابعة سحب الملف",
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
                } else if (generatedEncryptedCode == null) {
                    // Step 2: Customization Options
                    Text(
                        text = "اختر البيانات التي تريد تضمينها في الملف:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Select All Option
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (includeAll) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = includeAll,
                                    onCheckedChange = {
                                        includeAll = it
                                        if (it) {
                                            includeProducts = true
                                            includeCustomers = true
                                            includeAllInvoices = true
                                            includeTodayInvoicesOnly = false
                                            includeSafeTransactions = true
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("نسخة شاملة لكل البيانات (الموصى به)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    if (!includeAll) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Products Checkbox
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = includeProducts,
                                    onCheckedChange = { includeProducts = it },
                                    colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("قائمة المنتجات والمخزون فقط", fontSize = 13.sp)
                            }

                            // Customers Checkbox
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = includeCustomers,
                                    onCheckedChange = { includeCustomers = it },
                                    colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("بيانات العملاء وسجل الديون فقط", fontSize = 13.sp)
                            }

                            // Invoices Option
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = includeTodayInvoicesOnly,
                                    onCheckedChange = {
                                        includeTodayInvoicesOnly = it
                                        if (it) includeAllInvoices = false
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("فواتير ومبيعات اليوم فقط", fontSize = 13.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = includeAllInvoices,
                                    onCheckedChange = {
                                        includeAllInvoices = it
                                        if (it) includeTodayInvoicesOnly = false
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("سجل كافة الفواتير السابقة", fontSize = 13.sp)
                            }

                            // Safe Checkbox
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = includeSafeTransactions,
                                    onCheckedChange = { includeSafeTransactions = it },
                                    colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حركة الخزينة والمصروفات", fontSize = 13.sp)
                            }
                        }
                    }

                    // Security Info Note
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "الملف سيكون مشفراً بالكامل بتقنية AES-256 ولا يمكن قراءته إلا بواسطة نفس البرنامج لضمان السرية التامة.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = { handleGenerateBackup() },
                        enabled = !isGenerating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("create_backup_file_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جارٍ تشفير وتجهيز الملف...")
                        } else {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تشفير وسحب ملف المعلومات", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                } else {
                    // Step 3: Result Ready
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(ProfitGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(34.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "تم إنشاء وتشفير ملف المعلومات بنجاح!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "يمكنك الآن مشاركة الملف عبر واتساب أو نسخه أو نقله لأي جهاز آخر يحتوي على نفس التطبيق.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Actions
                        Button(
                            onClick = { shareEncryptedFile(generatedEncryptedCode!!) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("share_backup_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مشاركة وإرسال الملف (واتساب / درايف)", fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { copyToClipboard(generatedEncryptedCode!!) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("نسخ الرمز المشفر للحافظة", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
