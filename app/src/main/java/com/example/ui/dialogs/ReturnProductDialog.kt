package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Customer
import com.example.data.model.Product
import com.example.ui.theme.DebtRed
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.AccountingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnProductDialog(
    initialProduct: Product?,
    viewModel: AccountingViewModel,
    onDismiss: () -> Unit
) {
    val allProducts by viewModel.allProducts.collectAsState()
    val allCustomers by viewModel.allCustomers.collectAsState()
    val settings by viewModel.storeSettings.collectAsState()

    var selectedProduct by remember { mutableStateOf(initialProduct ?: allProducts.firstOrNull()) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var quantityText by remember { mutableStateOf("1") }
    var refundCash by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    var productExpanded by remember { mutableStateOf(false) }
    var customerExpanded by remember { mutableStateOf(false) }

    val prod = selectedProduct
    val qty = quantityText.toDoubleOrNull() ?: 0.0
    val totalReturnValue = (prod?.sellPrice ?: 0.0) * qty

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
                        Icon(Icons.Default.AssignmentReturn, contentDescription = null, tint = DebtRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تسجيل مرتجع صنف للمخزن",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "إغلاق")
                    }
                }

                // Select Product Dropdown
                ExposedDropdownMenuBox(
                    expanded = productExpanded,
                    onExpandedChange = { productExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.name ?: "اختر الصنف المراد إرجاعه",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("الصنف المرتجع") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = productExpanded,
                        onDismissRequest = { productExpanded = false }
                    ) {
                        allProducts.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.name} (المخزون الحالي: ${viewModel.formatMoney(item.stockQuantity)} ${item.unit})") },
                                onClick = {
                                    selectedProduct = item
                                    productExpanded = false
                                }
                            )
                        }
                    }
                }

                // Quantity Input
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("الكمية المرتجعة (${selectedProduct?.unit ?: "قطعة"})") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                // Select Customer Dropdown (Optional)
                ExposedDropdownMenuBox(
                    expanded = customerExpanded,
                    onExpandedChange = { customerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCustomer?.name ?: "مرتجع عام بدون ربط بعميل",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("العميل (اختياري لخصم الدين)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = customerExpanded,
                        onDismissRequest = { customerExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("مرتجع عام بدون ربط بعميل") },
                            onClick = {
                                selectedCustomer = null
                                customerExpanded = false
                            }
                        )
                        allCustomers.forEach { customer ->
                            DropdownMenuItem(
                                text = { Text("${customer.name} (الدين: ${viewModel.formatMoney(customer.balance)} ${settings.currency})") },
                                onClick = {
                                    selectedCustomer = customer
                                    customerExpanded = false
                                }
                            )
                        }
                    }
                }

                // Refund Cash switch (if customer selected, can either refund cash or deduct debt)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                                text = if (selectedCustomer != null) "استرجاع نقدي من الخزنة" else "صرف المبلغ نقداً من الخزنة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (selectedCustomer != null) "إذا تم التعطيل سيتم خصم المبلغ من دين العميل تلقائياً" else "تسجيل حركة منصرف من الخزنة",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = refundCash,
                            onCheckedChange = { refundCash = it }
                        )
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات المرتجع (سبب الإرجاع)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Total Return Summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("قيمة المرتجع الإجمالية:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${viewModel.formatMoney(totalReturnValue)} ${settings.currency}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                    }
                }

                if (errorMsg.isNotBlank()) {
                    Text(text = errorMsg, color = DebtRed, fontSize = 12.sp)
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (prod == null) {
                                errorMsg = "يرجى تحديد الصنف المراد إرجاعه"
                                return@Button
                            }
                            if (qty <= 0) {
                                errorMsg = "يرجى إدخال كمية صحيحة"
                                return@Button
                            }
                            viewModel.processProductReturn(
                                product = prod,
                                quantity = qty,
                                customer = selectedCustomer,
                                refundCash = refundCash,
                                notes = notes.ifBlank { "مرتجع صنف ${prod.name}" }
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("confirm_return_product_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DebtRed)
                    ) {
                        Icon(Icons.Default.AssignmentReturn, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تأكيد إرجاع الصنف", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.7f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إلغاء")
                    }
                }
            }
        }
    }
}
