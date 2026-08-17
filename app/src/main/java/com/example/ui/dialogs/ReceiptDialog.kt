package com.example.ui.dialogs

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.InvoiceWithItems
import com.example.data.model.InvoiceItem
import com.example.data.model.PaymentType
import com.example.ui.theme.DebtRed
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.AccountingViewModel
import com.example.util.PrinterHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptDialog(
    invoiceWithItems: InvoiceWithItems,
    viewModel: AccountingViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val inv = invoiceWithItems.invoice
    val items = invoiceWithItems.items

    var selectedTemplate by remember(settings.invoiceTemplate) {
        mutableStateOf(settings.invoiceTemplate)
    }
    var includePreviousDebt by remember { mutableStateOf(true) }

    // Generate QR code for invoice containing readable info
    val fullText = remember(invoiceWithItems, settings) {
        viewModel.getFullInvoiceReadableText(invoiceWithItems)
    }
    val qrBitmap = remember(fullText) {
        viewModel.generateInvoiceQrBitmap(fullText, 240)
    }

    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH)
    val dateStr = dateFormat.format(Date(inv.timestamp))

    val grandTotal = inv.netAmount + if (includePreviousDebt && inv.previousDebt > 0) inv.previousDebt else 0.0

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header Bar (A5 Template Selectors & Close)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val templatesList = listOf(
                            "MODERN" to "عصري (A5)",
                            "ROYAL" to "ملكي (A5)",
                            "MINIMAL" to "مبسط (A5)"
                        )
                        templatesList.forEach { (tpl, label) ->
                            val isSel = selectedTemplate == tpl
                            Button(
                                onClick = { selectedTemplate = tpl },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Clear, contentDescription = "إغلاق")
                    }
                }

                // Previous Debt Include Switch (if customer has debt)
                if (inv.previousDebt > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "تضمين حساب الدين السابق (${viewModel.formatMoney(inv.previousDebt)} ${settings.currency})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = includePreviousDebt,
                            onCheckedChange = { includePreviousDebt = it },
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Invoice Body Container - Rendered according to selected A5 template
                val primaryColor = when (selectedTemplate) {
                    "ROYAL" -> Color(0xFF1E3A8A)
                    "MINIMAL" -> Color(0xFF0F172A)
                    else -> EmeraldPrimary
                }

                val borderColor = when (selectedTemplate) {
                    "ROYAL" -> Color(0xFFD97706)
                    "MINIMAL" -> Color(0xFF334155)
                    else -> Color(0xFFE2E8F0)
                }

                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(if (selectedTemplate == "ROYAL") 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Store Branding & Bakery Logo
                        if (selectedTemplate == "ROYAL") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E3A8A),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFD97706))
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = settings.storeName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (settings.phone.isNotBlank() || settings.address.isNotBlank()) {
                                        Text(
                                            text = "${if (settings.phone.isNotBlank()) "هاتف: ${settings.phone} • " else ""}${settings.address}",
                                            fontSize = 11.sp,
                                            color = Color(0xFFFEF08A)
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier.size(50.dp),
                                shape = CircleShape,
                                color = primaryColor.copy(alpha = 0.12f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Store,
                                        contentDescription = "شعار المحل",
                                        tint = primaryColor,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = settings.storeName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )

                            if (settings.phone.isNotBlank() || settings.address.isNotBlank()) {
                                Text(
                                    text = "${if (settings.phone.isNotBlank()) "هاتف: ${settings.phone} • " else ""}${settings.address}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        DashedDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Invoice Details Box
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("رقم الفاتورة: #${inv.invoiceNumber}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text("العميل: ${inv.customerName}", fontSize = 12.sp, color = Color(0xFF334155))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("التاريخ: $dateStr", fontSize = 10.sp, color = Color(0xFF64748B))
                                Text("الدفع: ${inv.paymentType.titleAr}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = primaryColor)
                            }
                        }

                        DashedDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Items Table Header
                        val tableHeaderBg = when (selectedTemplate) {
                            "ROYAL" -> Color(0xFFEEF2FF)
                            "MINIMAL" -> Color(0xFFF1F5F9)
                            else -> Color(0xFFF8FAFC)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(tableHeaderBg, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("الصنف", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.weight(2f))
                            Text("الكمية", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("السعر", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            Text("الإجمالي", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Items Rows
                        items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.productName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.weight(2f)
                                )
                                Text(
                                    text = "${if (item.quantity % 1.0 == 0.0) String.format(Locale.ENGLISH, "%.0f", item.quantity) else String.format(Locale.ENGLISH, "%.2f", item.quantity)} ${item.unit}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = viewModel.formatMoney(item.unitPrice),
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "${viewModel.formatMoney(item.total)} ${settings.currency}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.weight(1.2f),
                                    textAlign = TextAlign.End
                                )
                            }
                        }

                        DashedDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Financial Summary Totals
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("إجمالي الفاتورة:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                Text("${viewModel.formatMoney(inv.netAmount)} ${settings.currency}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }

                            if (includePreviousDebt && inv.previousDebt > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("دين سابق مستحق:", fontSize = 11.sp, color = DebtRed)
                                    Text("${viewModel.formatMoney(inv.previousDebt)} ${settings.currency}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DebtRed)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("المطلوب كلياً:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                                    Text("${viewModel.formatMoney(grandTotal)} ${settings.currency}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("المدفوع نقداً:", fontSize = 11.sp, color = Color(0xFF475569))
                                Text("${viewModel.formatMoney(inv.paidAmount)} ${settings.currency}", fontSize = 11.sp, color = Color(0xFF0F172A))
                            }

                            if (inv.remainingAmount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("المتبقي آجل:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DebtRed)
                                    Text("${viewModel.formatMoney(inv.remainingAmount)} ${settings.currency}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DebtRed)
                                }
                            }
                        }

                        // Barcode & QR Code Section
                        Spacer(modifier = Modifier.height(10.dp))
                        Image(
                            bitmap = qrBitmap.asImageBitmap(),
                            contentDescription = "باركود الفاتورة",
                            modifier = Modifier.size(105.dp)
                        )
                        Text(
                            text = "رمز الفاتورة: #${inv.invoiceNumber}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF64748B)
                        )

                        if (settings.invoiceFooterNote.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = settings.invoiceFooterNote,
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            PrinterHelper.printInvoiceA5(
                                context = context,
                                invoiceWithItems = invoiceWithItems,
                                settings = settings,
                                includeDebt = includePreviousDebt,
                                template = selectedTemplate
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("print_receipt_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("طباعة فاتورة A5", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.shareInvoiceText(context, invoiceWithItems) },
                        modifier = Modifier
                            .weight(0.9f)
                            .height(48.dp)
                            .testTag("share_receipt_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاركة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DashedDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFE2E8F0))
    )
}
