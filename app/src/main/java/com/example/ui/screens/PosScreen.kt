package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartItem
import com.example.data.model.Customer
import com.example.data.model.PaymentType
import com.example.data.model.Product
import com.example.ui.theme.DebtRed
import com.example.ui.theme.DebtRedLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.ProfitGreen
import com.example.ui.viewmodel.AccountingViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.storeSettings.collectAsState()
    val products by viewModel.filteredProducts.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val cartState by viewModel.cartState.collectAsState()
    val customers by viewModel.allCustomers.collectAsState()
    val searchQuery by viewModel.productSearchQuery.collectAsState()
    val selectedCategory by viewModel.selectedProductCategory.collectAsState()

    var showCartSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Categories list extracted from all products
    val categories = remember(allProducts) {
        listOf("الكل") + allProducts.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. Search Bar (Product name search, Barcode search button removed)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setProductSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pos_search_input"),
                placeholder = { Text("ابحث باسم المنتج...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setProductSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "مسح")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Category Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = category == selectedCategory,
                        onClick = { viewModel.setProductCategory(category) },
                        label = { Text(category, fontSize = 12.sp, fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Products Grid
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "لا توجد منتجات تطابق البحث",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.openAddProductDialog() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة صنف جديد")
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        val cartItem = cartState.items.find { it.product.id == product.id }
                        val inCartQty = cartItem?.quantity ?: 0.0

                        PosProductCard(
                            product = product,
                            currency = settings.currency,
                            inCartQty = inCartQty,
                            onQuantityChange = { qty -> viewModel.setCartItemQuantity(product, qty) },
                            onIncrement = { viewModel.addToCart(product, 1.0) },
                            onDecrement = {
                                if (inCartQty <= 1.0) {
                                    viewModel.removeFromCart(product.id)
                                } else {
                                    viewModel.updateCartItemQuantity(product.id, inCartQty - 1.0)
                                }
                            }
                        )
                    }
                }
            }
        }

        // 4. Floating / Docked Bottom Cart Bar (Min 48dp Touch Target)
        if (cartState.items.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 10.dp,
                shadowElevation = 8.dp,
                border = CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cart Info (tap opens sheet)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showCartSheet = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = "${cartState.items.size}",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "السلة",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "الإجمالي (${String.format(Locale.ENGLISH, "%.0f", cartState.totalItemsCount)} قطع):",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.ENGLISH, "%.2f", cartState.netTotal)} ${settings.currency}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Open Full Checkout Sheet Button
                    Button(
                        onClick = { showCartSheet = true },
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("pos_open_cart_sheet_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCartCheckout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إتمام البيع",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // Full POS Cart & Checkout Modal Bottom Sheet
    if (showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            PosCheckoutSheetContent(
                viewModel = viewModel,
                cartState = cartState,
                customers = customers,
                currency = settings.currency,
                onClose = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showCartSheet = false
                    }
                },
                onCompleteSale = {
                    viewModel.completeSaleAndShowReceipt {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showCartSheet = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun PosProductCard(
    product: Product,
    currency: String,
    inCartQty: Double,
    onQuantityChange: (Double) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    val isOutOfStock = product.stockQuantity <= 0
    val isLowStock = product.stockQuantity <= product.minStockAlert && !isOutOfStock

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (inCartQty > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (inCartQty > 0) CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(ProfitGreen)
        ) else CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header with Category & Cart Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = product.category,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (inCartQty > 0) {
                    Surface(
                        shape = CircleShape,
                        color = ProfitGreen
                    ) {
                        Text(
                            text = "في السلة: ${String.format(Locale.ENGLISH, "%.0f", inCartQty)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (isLowStock) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DebtRedLight
                    ) {
                        Text(
                            text = "منخفض",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = DebtRed,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Product Name
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Stock & Unit
            Text(
                text = if (isOutOfStock) "نفد المخزون" else "المتوفر: ${String.format(Locale.ENGLISH, "%.0f", product.stockQuantity)} ${product.unit}",
                fontSize = 11.sp,
                color = if (isOutOfStock) DebtRed else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Price
            Text(
                text = "${String.format(Locale.ENGLISH, "%.2f", product.sellPrice)} $currency / ${product.unit}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quantity Control: Red - on Right, Editable Number Box in Center (starts empty or with current count), Green + on Left
            if (isOutOfStock) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "غير متوفر",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                PosQuantityEditor(
                    quantity = inCartQty,
                    onQuantityChange = onQuantityChange,
                    onIncrement = onIncrement,
                    onDecrement = onDecrement,
                    modifier = Modifier.fillMaxWidth(),
                    isCompact = true
                )
            }
        }
    }
}

@Composable
private fun PosCheckoutSheetContent(
    viewModel: AccountingViewModel,
    cartState: com.example.ui.viewmodel.PosCartState,
    customers: List<Customer>,
    currency: String,
    onClose: () -> Unit,
    onCompleteSale: () -> Unit
) {
    var customerDropdownExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Sheet Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سلة المبيعات (${cartState.items.size} أصناف)",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                TextButton(onClick = { viewModel.clearCart() }) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = DebtRed, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إفراغ السلة", color = DebtRed, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Cart Items List
        items(cartState.items, key = { it.product.id }) { item ->
            CartItemRow(
                item = item,
                currency = currency,
                onQuantityChange = { newQty -> viewModel.setCartItemQuantity(item.product, newQty) },
                onIncrement = { viewModel.addToCart(item.product, 1.0) },
                onDecrement = { viewModel.updateCartItemQuantity(item.product.id, item.quantity - 1.0) },
                onRemove = { viewModel.removeFromCart(item.product.id) }
            )
        }

        // 1. Customer Selection
        item {
            Text(
                text = "اختيار العميل",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { customerDropdownExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("pos_customer_dropdown_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cartState.selectedCustomer?.name ?: "عميل نقدي (كاش)",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(Icons.Default.ExpandMore, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = customerDropdownExpanded,
                        onDismissRequest = { customerDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("عميل نقدي (كاش)") },
                            onClick = {
                                viewModel.setCartCustomer(null)
                                customerDropdownExpanded = false
                            }
                        )
                        customers.forEach { cust ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(cust.name, fontWeight = FontWeight.Bold)
                                        if (cust.balance > 0) {
                                            Text("عليه دين: ${String.format(Locale.ENGLISH, "%.2f", cust.balance)} $currency", fontSize = 11.sp, color = DebtRed)
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.setCartCustomer(cust)
                                    customerDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Add Customer Quick Action
                IconButton(
                    onClick = { viewModel.openAddCustomerDialog() },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "إضافة عميل",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // 2. Payment Method
        item {
            Text(
                text = "طريقة الدفع",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PaymentType.values().forEach { pType ->
                    val isSelected = cartState.paymentType == pType
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (pType == PaymentType.CREDIT && cartState.selectedCustomer == null) {
                                customerDropdownExpanded = true
                            }
                            viewModel.setCartPaymentType(pType)
                        },
                        label = { Text(pType.titleAr, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (pType == PaymentType.CREDIT) DebtRed else MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Summary & Net Total Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("إجمالي مشتريات الفاتورة الحالية:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${String.format(Locale.ENGLISH, "%.2f", cartState.netTotal)} $currency", fontWeight = FontWeight.Bold)
                    }

                    if (cartState.previousDebt > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("دين سابق على العميل:", color = DebtRed)
                            Text("+${String.format(Locale.ENGLISH, "%.2f", cartState.previousDebt)} $currency", color = DebtRed, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الصافي المطلوب:",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${String.format(Locale.ENGLISH, "%.2f", cartState.netTotal)} $currency",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }
        }

        // 5. Big Touch-Friendly Action Button: "بيع وطباعة" (Min 48dp height)
        item {
            val isCreditWithoutCustomer = cartState.paymentType == PaymentType.CREDIT && cartState.selectedCustomer == null

            Button(
                onClick = onCompleteSale,
                enabled = !isCreditWithoutCustomer && cartState.items.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("pos_sell_and_print_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isCreditWithoutCustomer) "يرجى تحديد العميل للبيع الآجل" else "بيع وطباعة الفاتورة 🧾",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    currency: String,
    onQuantityChange: (Double) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                    text = item.product.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${String.format(Locale.ENGLISH, "%.2f", item.customPrice)} $currency × ${String.format(Locale.ENGLISH, "%.0f", item.quantity)} = ${String.format(Locale.ENGLISH, "%.2f", item.total)} $currency",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Quantity Control: Red - (Right), Input field (Center), Green + (Left)
            PosQuantityEditor(
                quantity = item.quantity,
                onQuantityChange = onQuantityChange,
                onIncrement = onIncrement,
                onDecrement = onDecrement,
                isCompact = false
            )
        }
    }
}

/**
 * Universal POS Quantity Selector
 * Matches user's exact specification:
 * - Right: Red minus (-) button
 * - Center: Input field allowing typing custom numbers
 * - Left: Green plus (+) button
 */
@Composable
private fun PosQuantityEditor(
    quantity: Double,
    onQuantityChange: (Double) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    var textValue by remember(quantity) {
        mutableStateOf(if (quantity > 0) String.format(Locale.ENGLISH, "%.0f", quantity) else "")
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // 1. Right Button: Red Minus (-)
        Box(
            modifier = Modifier
                .size(if (isCompact) 32.dp else 38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (quantity > 0) DebtRed else MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = quantity > 0) { onDecrement() }
                .testTag("qty_minus_btn"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (quantity <= 1.0) Icons.Default.Delete else Icons.Default.Remove,
                contentDescription = "نقص",
                tint = if (quantity > 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(if (isCompact) 16.dp else 20.dp)
            )
        }

        // 2. Middle Field: Empty/Editable Text Field for direct number typing
        Box(
            modifier = Modifier
                .width(if (isCompact) 48.dp else 58.dp)
                .height(if (isCompact) 32.dp else 38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    width = 1.5.dp,
                    color = if (quantity > 0) ProfitGreen else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = textValue,
                onValueChange = { newText ->
                    val cleanText = newText.filter { it.isDigit() }
                    textValue = cleanText
                    val parsed = cleanText.toDoubleOrNull()
                    if (parsed != null && parsed > 0) {
                        onQuantityChange(parsed)
                    } else if (cleanText.isEmpty() || parsed == 0.0) {
                        onQuantityChange(0.0)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (textValue.isEmpty()) {
                            Text(
                                text = "0",
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Normal,
                                fontSize = if (isCompact) 13.sp else 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                        innerTextField()
                    }
                },
                textStyle = TextStyle(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isCompact) 13.sp else 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
                    .testTag("qty_input_field")
            )
        }

        // 3. Left Button: Green Plus (+)
        Box(
            modifier = Modifier
                .size(if (isCompact) 32.dp else 38.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(ProfitGreen)
                .clickable { onIncrement() }
                .testTag("qty_plus_btn"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "زيادة",
                tint = Color.White,
                modifier = Modifier.size(if (isCompact) 16.dp else 20.dp)
            )
        }
    }
}
