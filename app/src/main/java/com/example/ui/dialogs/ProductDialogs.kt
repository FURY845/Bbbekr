package com.example.ui.dialogs

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Product
import com.example.ui.theme.DebtRed
import com.example.ui.theme.EmeraldPrimary
import java.util.Random

@Composable
fun AddEditProductDialog(
    product: Product?,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit,
    onDelete: ((Product) -> Unit)? = null
) {
    val isEditing = product != null

    var name by remember { mutableStateOf(product?.name ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var purchasePrice by remember { mutableStateOf(product?.purchasePrice?.let { if (it > 0) it.toString() else "" } ?: "") }
    var sellPrice by remember { mutableStateOf(product?.sellPrice?.let { if (it > 0) it.toString() else "" } ?: "") }
    var stockQuantity by remember { mutableStateOf(product?.stockQuantity?.let { if (it >= 0) String.format(java.util.Locale.ENGLISH, "%.0f", it) else "" } ?: "10") }
    var minStockAlert by remember { mutableStateOf(product?.minStockAlert?.let { if (it >= 0) String.format(java.util.Locale.ENGLISH, "%.0f", it) else "" } ?: "5") }
    var category by remember { mutableStateOf(product?.category ?: "مواد غذائية") }
    var unit by remember { mutableStateOf(product?.unit ?: "قطعة") }

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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "تعديل الصنف" else "إضافة صنف جديد",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "إغلاق")
                    }
                }

                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المنتج / الصنف *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_dialog_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Barcode with auto-generate button
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("رقم الباركود") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_dialog_barcode_input"),
                    trailingIcon = {
                        IconButton(onClick = {
                            val randomDigits = (10000000..99999999).random()
                            barcode = "628$randomDigits"
                        }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "توليد باركود تلقائي", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Prices: Purchase Price & Selling Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = purchasePrice,
                        onValueChange = { purchasePrice = it },
                        label = { Text("سعر الشراء ($currency)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_dialog_buy_price_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = sellPrice,
                        onValueChange = { sellPrice = it },
                        label = { Text("سعر البيع ($currency) *") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_dialog_sell_price_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Stock & Min Stock Alert
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = stockQuantity,
                        onValueChange = { stockQuantity = it },
                        label = { Text("الكمية المتوفرة") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_dialog_stock_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = minStockAlert,
                        onValueChange = { minStockAlert = it },
                        label = { Text("حد التنبيه") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Category
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("القسم / التصنيف") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Interactive Unit Ribbon / Quick Selection
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "نوع الوحدة / العبوة:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "الوحدة المحددة: $unit",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Horizontal Ribbon of popular unit types
                    val quickUnits = listOf("كرتونة", "صفيحة", "كيلو", "شوال", "وحدة", "علبة", "لتر", "متر", "قطعة")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickUnits.forEach { option ->
                            val isSelected = unit.trim() == option
                            FilterChip(
                                selected = isSelected,
                                onClick = { unit = option },
                                label = {
                                    Text(
                                        text = option,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    // Editable custom unit field for flexibility
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("أو اكتب وحدة مخصصة يدوياً") },
                        placeholder = { Text("مثال: كرتونة، صفيحة، كيلو...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_dialog_unit_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isEditing && onDelete != null) {
                        OutlinedButton(
                            onClick = { onDelete(product!!) },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("product_dialog_delete_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DebtRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("حذف")
                        }
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) return@Button
                            val pPrice = purchasePrice.toDoubleOrNull() ?: 0.0
                            val sPrice = sellPrice.toDoubleOrNull() ?: 0.0
                            val stock = stockQuantity.toDoubleOrNull() ?: 0.0
                            val minStock = minStockAlert.toDoubleOrNull() ?: 5.0

                            val updated = product?.copy(
                                name = name,
                                barcode = barcode,
                                purchasePrice = pPrice,
                                sellPrice = sPrice,
                                stockQuantity = stock,
                                minStockAlert = minStock,
                                category = if (category.isBlank()) "عام" else category,
                                unit = if (unit.isBlank()) "قطعة" else unit
                            ) ?: Product(
                                name = name,
                                barcode = barcode,
                                purchasePrice = pPrice,
                                sellPrice = sPrice,
                                stockQuantity = stock,
                                minStockAlert = minStock,
                                category = if (category.isBlank()) "عام" else category,
                                unit = if (unit.isBlank()) "قطعة" else unit
                            )
                            onSave(updated)
                        },
                        enabled = name.isNotBlank() && sellPrice.isNotBlank(),
                        modifier = Modifier
                            .weight(1.5f)
                            .height(50.dp)
                            .testTag("product_dialog_save_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("حفظ الصنف", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickEditPriceDialog(
    product: Product,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (newPrice: Double) -> Unit
) {
    var priceText by remember { mutableStateOf(String.format(java.util.Locale.ENGLISH, "%.2f", product.sellPrice)) }

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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("تعديل سعر البيع اليومي", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(product.name, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "إغلاق")
                    }
                }

                Text(
                    text = "سعر الشراء الحالي: ${String.format(java.util.Locale.ENGLISH, "%.2f", product.purchasePrice)} $currency",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("سعر البيع الجديد اليوم ($currency) *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_edit_price_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        val newPrice = priceText.toDoubleOrNull() ?: 0.0
                        if (newPrice > 0) {
                            onSave(newPrice)
                        }
                    },
                    enabled = (priceText.toDoubleOrNull() ?: 0.0) > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_quick_price_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ السعر الجديد", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddStockDialog(
    product: Product,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (addedQuantity: Double, purchaseCost: Double, payFromSafe: Boolean, notes: String) -> Unit
) {
    var addedQty by remember { mutableStateOf("10") }
    var costPrice by remember { mutableStateOf(if (product.purchasePrice > 0) product.purchasePrice.toString() else "") }
    var payFromSafe by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }

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
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("توريد مخزون جديد", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(product.name, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Clear, contentDescription = "إغلاق")
                    }
                }

                OutlinedTextField(
                    value = addedQty,
                    onValueChange = { addedQty = it },
                    label = { Text("الكمية المضافة (${product.unit})") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_stock_qty_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = costPrice,
                    onValueChange = { costPrice = it },
                    label = { Text("سعر تكلفة الشراء للوحدة ($currency)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = payFromSafe,
                        onCheckedChange = { payFromSafe = it }
                    )
                    Text("خصم تكلفة الشراء من رصيد الخزنة كمصروف", fontSize = 13.sp)
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات / اسم المورد (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        val qty = addedQty.toDoubleOrNull() ?: 0.0
                        val cost = costPrice.toDoubleOrNull() ?: product.purchasePrice
                        if (qty > 0) {
                            onConfirm(qty, cost, payFromSafe, notes)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_add_stock_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تأكيد إضافة المخزون", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
