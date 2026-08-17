package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Product
import com.example.ui.dialogs.AddEditProductDialog
import com.example.ui.dialogs.AddStockDialog
import com.example.ui.dialogs.DailyInventoryDialog
import com.example.ui.dialogs.QuickEditPriceDialog
import com.example.ui.dialogs.ReturnProductDialog
import com.example.ui.theme.DebtRed
import com.example.ui.theme.DebtRedLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenLight
import com.example.ui.viewmodel.AccountingViewModel
import java.util.Locale

@Composable
fun ProductsScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.storeSettings.collectAsState()
    val products by viewModel.filteredProducts.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val searchQuery by viewModel.productSearchQuery.collectAsState()
    val selectedCategory by viewModel.selectedProductCategory.collectAsState()
    val showOnlyLowStock by viewModel.showOnlyLowStock.collectAsState()
    val todayOutflow by viewModel.todayOutflowByProduct.collectAsState()

    val showProductDialog by viewModel.showProductDialog.collectAsState()
    val editingProduct by viewModel.editingProduct.collectAsState()
    val showAddStockDialog by viewModel.showAddStockDialog.collectAsState()
    val selectedProductForStock by viewModel.selectedProductForStock.collectAsState()
    val showQuickPriceDialog by viewModel.showQuickPriceDialog.collectAsState()
    val quickPriceProduct by viewModel.quickPriceProduct.collectAsState()
    val showDailyInventoryDialog by viewModel.showDailyInventoryDialog.collectAsState()
    val showReturnProductDialog by viewModel.showReturnProductDialog.collectAsState()
    val selectedProductForReturn by viewModel.selectedProductForReturn.collectAsState()

    val totalCostValue = remember(allProducts) { allProducts.sumOf { it.purchasePrice * it.stockQuantity } }
    val totalSellValue = remember(allProducts) { allProducts.sumOf { it.sellPrice * it.stockQuantity } }
    val expectedProfit = remember(totalSellValue, totalCostValue) { totalSellValue - totalCostValue }

    val categories = remember(allProducts) {
        listOf("الكل") + allProducts.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
        ) {
            // 1. Inventory Total Valuation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "إجمالي تقييم المخزون الحالي",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Daily Inventory Button
                            TextButton(
                                onClick = { viewModel.openDailyInventoryDialog() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("جرد اليوم", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "قيمة الشراء",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${viewModel.formatMoney(totalCostValue)} ${settings.currency}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "قيمة البيع المتوقعة",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${viewModel.formatMoney(totalSellValue)} ${settings.currency}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "الأرباح المتوقعة",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "+${viewModel.formatMoney(expectedProfit)} ${settings.currency}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 2. Action Buttons: [إضافة صنف جديد] & [تسجيل مرتجع صنف]
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.openAddProductDialog() },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                            .testTag("add_product_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة صنف جديد", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = { viewModel.openReturnProductDialog(null) },
                        modifier = Modifier
                            .weight(0.9f)
                            .height(48.dp)
                            .testTag("open_returns_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = DebtRed),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مرتجع صنف", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // 3. Search & Low Stock Toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setProductSearchQuery(it) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("products_search_input"),
                        placeholder = { Text("بحث بالاسم، الباركود أو القسم...", fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setProductSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    FilterChip(
                        selected = showOnlyLowStock,
                        onClick = { viewModel.toggleShowOnlyLowStock() },
                        label = { Text("النواقص", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (showOnlyLowStock) Color.White else DebtRed
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DebtRed,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    )
                }
            }

            // 4. Categories Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { category ->
                        FilterChip(
                            selected = category == selectedCategory,
                            onClick = { viewModel.setProductCategory(category) },
                            label = { Text(category, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // 5. Products List
            if (products.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (showOnlyLowStock) "لا توجد أصناف ناقصة حالياً" else "لا توجد منتجات مسجلة",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(products, key = { it.id }) { product ->
                    val soldToday = todayOutflow[product.id] ?: 0.0

                    ProductCardItem(
                        product = product,
                        currency = settings.currency,
                        soldToday = soldToday,
                        viewModel = viewModel,
                        onEdit = { viewModel.openEditProductDialog(product) },
                        onAddStock = { viewModel.openAddStockDialog(product) },
                        onQuickPrice = { viewModel.openQuickPriceDialog(product) },
                        onReturn = { viewModel.openReturnProductDialog(product) }
                    )
                }
            }
        }

        // Dialogs
        if (showProductDialog) {
            AddEditProductDialog(
                product = editingProduct,
                currency = settings.currency,
                onDismiss = { viewModel.closeProductDialog() },
                onSave = { viewModel.saveProduct(it) },
                onDelete = { viewModel.deleteProduct(it) }
            )
        }

        if (showAddStockDialog && selectedProductForStock != null) {
            AddStockDialog(
                product = selectedProductForStock!!,
                currency = settings.currency,
                onDismiss = { viewModel.closeAddStockDialog() },
                onConfirm = { qty, cost, fromSafe, notes ->
                    viewModel.addStockToProduct(qty, cost, fromSafe, notes)
                }
            )
        }

        if (showQuickPriceDialog && quickPriceProduct != null) {
            QuickEditPriceDialog(
                product = quickPriceProduct!!,
                currency = settings.currency,
                onDismiss = { viewModel.closeQuickPriceDialog() },
                onSave = { viewModel.updateProductPrice(it) }
            )
        }

        if (showDailyInventoryDialog) {
            DailyInventoryDialog(
                viewModel = viewModel,
                onDismiss = { viewModel.closeDailyInventoryDialog() }
            )
        }

        if (showReturnProductDialog) {
            ReturnProductDialog(
                initialProduct = selectedProductForReturn,
                viewModel = viewModel,
                onDismiss = { viewModel.closeReturnProductDialog() }
            )
        }
    }
}

@Composable
fun ProductCardItem(
    product: Product,
    currency: String,
    soldToday: Double,
    viewModel: AccountingViewModel,
    onEdit: () -> Unit,
    onAddStock: () -> Unit,
    onQuickPrice: () -> Unit,
    onReturn: () -> Unit
) {
    val isLowStock = product.stockQuantity <= product.minStockAlert

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onEdit() }
            .testTag("product_card_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: Name, Category, Stock Quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "القسم: ${product.category}${if (product.barcode.isNotBlank()) " • باركود: ${product.barcode}" else ""}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Stock Badge
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isLowStock) DebtRedLight else ProfitGreenLight
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = if (isLowStock) "نواقص (${viewModel.formatMoney(product.stockQuantity)} ${product.unit})" else "${viewModel.formatMoney(product.stockQuantity)} ${product.unit}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLowStock) DebtRed else ProfitGreen
                        )
                        if (soldToday > 0) {
                            Text(
                                text = "خرج اليوم: ${viewModel.formatMoney(soldToday)}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Row 2: Price Details
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("سعر البيع: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${viewModel.formatMoney(product.sellPrice)} $currency",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfitGreen
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("سعر الشراء: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${viewModel.formatMoney(product.purchasePrice)} $currency",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val profitPerUnit = product.sellPrice - product.purchasePrice
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("الربح: ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "+${viewModel.formatMoney(profitPerUnit)} $currency",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Row 3: Quick Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add Stock
                OutlinedButton(
                    onClick = onAddStock,
                    modifier = Modifier.weight(1.1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(Icons.Default.PostAdd, contentDescription = null, modifier = Modifier.size(15.dp), tint = ProfitGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("توريد مخزون", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Return Product
                OutlinedButton(
                    onClick = onReturn,
                    modifier = Modifier.weight(0.9f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DebtRed),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("مرتجع", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Quick Price Edit
                OutlinedButton(
                    onClick = onQuickPrice,
                    modifier = Modifier.weight(0.9f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(Icons.Default.PriceChange, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("تعديل السعر", fontSize = 11.sp)
                }

                // Edit
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "تعديل كامل", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
