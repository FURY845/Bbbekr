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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.InvoiceWithItems
import com.example.data.model.Customer
import com.example.data.model.CustomerTransaction
import com.example.data.model.CustomerTransactionType
import com.example.data.model.PaymentType
import com.example.ui.dialogs.AddEditCustomerDialog
import com.example.ui.dialogs.AdjustDebtDialog
import com.example.ui.dialogs.ConfirmDeleteCustomerDialog
import com.example.ui.dialogs.CustomerPaymentDialog
import com.example.ui.theme.DebtRed
import com.example.ui.theme.DebtRedLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenLight
import com.example.ui.viewmodel.AccountingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomersScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val customers by viewModel.filteredCustomers.collectAsState()
    val totalDebts by viewModel.totalCustomerDebts.collectAsState()
    val searchQuery by viewModel.customerSearchQuery.collectAsState()

    val selectedCustomerForProfile by viewModel.selectedCustomerForProfile.collectAsState()
    val showCustomerDialog by viewModel.showCustomerDialog.collectAsState()
    val editingCustomer by viewModel.editingCustomer.collectAsState()
    val customerToDelete by viewModel.customerToDelete.collectAsState()
    val showDebtAdjustDialog by viewModel.showDebtAdjustDialog.collectAsState()
    val debtAdjustCustomer by viewModel.debtAdjustCustomer.collectAsState()
    val showCustomerPaymentDialog by viewModel.showCustomerPaymentDialog.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
        ) {
            // 1. Wide Green "Add Customer" Button
            item {
                Button(
                    onClick = { viewModel.openAddCustomerDialog() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("add_customer_wide_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ProfitGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(14.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إضافة عميل جديد",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 2. Total Customer Debts Overview
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (totalDebts > 0) DebtRedLight else ProfitGreenLight
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "إجمالي ديون العملاء المستحقة",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (totalDebts > 0) Color(0xFF991B1B) else Color(0xFF166534)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${viewModel.formatMoney(totalDebts)} ${settings.currency}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalDebts > 0) DebtRed else ProfitGreen
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(if (totalDebts > 0) DebtRed else ProfitGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // 3. Search Field
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setCustomerSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customers_search_input"),
                    placeholder = { Text("بحث بالاسم، رقم الهاتف أو العنوان...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setCustomerSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 4. Customers List as Cards
            if (customers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.People,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "لا يوجد عملاء مضافين بعد",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.openAddCustomerDialog() },
                                colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إضافة عميل جديد", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(customers, key = { it.id }) { customer ->
                    CustomerCardItem(
                        customer = customer,
                        currency = settings.currency,
                        viewModel = viewModel,
                        onClick = { viewModel.openCustomerProfile(customer) },
                        onEdit = { viewModel.openEditCustomerDialog(customer) },
                        onDelete = { viewModel.openConfirmDeleteCustomerDialog(customer) },
                        onAdjustDebt = { viewModel.openDebtAdjustDialog(customer) }
                    )
                }
            }
        }

        // Add / Edit Customer Dialog
        if (showCustomerDialog) {
            AddEditCustomerDialog(
                customer = editingCustomer,
                currency = settings.currency,
                onDismiss = { viewModel.closeCustomerDialog() },
                onSave = { viewModel.saveCustomer(it) }
            )
        }

        // Adjust Debt & Date Dialog
        if (showDebtAdjustDialog && debtAdjustCustomer != null) {
            AdjustDebtDialog(
                customer = debtAdjustCustomer!!,
                currency = settings.currency,
                onDismiss = { viewModel.closeDebtAdjustDialog() },
                onConfirm = { newBal, date, notes ->
                    viewModel.saveAdjustedDebt(newBal, date, notes)
                }
            )
        }

        // Customer Profile Dialog (Detailed Ledger + All Invoices)
        if (selectedCustomerForProfile != null) {
            CustomerProfileDialog(
                customer = selectedCustomerForProfile!!,
                viewModel = viewModel,
                onClose = { viewModel.closeCustomerProfile() }
            )
        }

        // Customer Payment Dialog
        if (showCustomerPaymentDialog && selectedCustomerForProfile != null) {
            CustomerPaymentDialog(
                customer = selectedCustomerForProfile!!,
                currency = settings.currency,
                onDismiss = { viewModel.closeCustomerPaymentDialog() },
                onConfirm = { amount, depositToSafe, notes ->
                    viewModel.recordCustomerPayment(amount, depositToSafe, notes)
                }
            )
        }

        // Confirm Delete Customer Dialog
        if (customerToDelete != null) {
            ConfirmDeleteCustomerDialog(
                customer = customerToDelete!!,
                onDismiss = { viewModel.closeConfirmDeleteCustomerDialog() },
                onConfirm = { viewModel.confirmDeleteCustomer() }
            )
        }
    }
}

@Composable
fun CustomerCardItem(
    customer: Customer,
    currency: String,
    viewModel: AccountingViewModel,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAdjustDebt: () -> Unit
) {
    val hasDebt = customer.balance > 0
    val debtDateStr = if (customer.debtDate != null && customer.debtDate > 0) {
        val format = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
        format.format(Date(customer.debtDate))
    } else ""

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("customer_card_${customer.id}"),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasDebt) DebtRed.copy(alpha = 0.15f)
                                else ProfitGreen.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = customer.name.take(1),
                            fontWeight = FontWeight.Bold,
                            color = if (hasDebt) DebtRed else ProfitGreen,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = customer.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (customer.phone.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = customer.phone,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (customer.address.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = customer.address,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Balance Status Badge
                Column(horizontalAlignment = Alignment.End) {
                    if (hasDebt) {
                        Surface(shape = RoundedCornerShape(10.dp), color = DebtRedLight) {
                            Column(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text("دين سابق مستحق", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DebtRed)
                                Text("${viewModel.formatMoney(customer.balance)} $currency", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DebtRed)
                                if (debtDateStr.isNotBlank()) {
                                    Text("بتاريخ: $debtDateStr", fontSize = 9.sp, color = DebtRed)
                                }
                            }
                        }
                    } else {
                        Surface(shape = RoundedCornerShape(10.dp), color = ProfitGreenLight) {
                            Text(
                                text = "الحساب مسدد",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Prominent "سجل المعاملات والفواتير" Ledger Button
            Button(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("customer_ledger_btn_${customer.id}"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "سجل المعاملات والفواتير (كشف حساب العميل)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Bottom Actions Row with Small "Delete Customer" Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Adjust debt button
                    OutlinedButton(
                        onClick = onAdjustDebt,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = ProfitGreen)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تعديل الدين والتاريخ", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }

                    // Edit customer info
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تعديل", fontSize = 11.sp)
                    }
                }

                // Small Delete Customer Button
                OutlinedButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DebtRed),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("small_delete_customer_btn_${customer.id}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CustomerProfileDialog(
    customer: Customer,
    viewModel: AccountingViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.storeSettings.collectAsState()
    val transactions by viewModel.customerTransactions.collectAsState()
    val customerInvoices by viewModel.customerInvoicesWithItems.collectAsState()
    val hasDebt = customer.balance > 0

    var selectedTab by remember { mutableStateOf(0) } // 0: Invoices, 1: Ledger

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ملف وسجل فواتير العميل",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = customer.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row {
                            IconButton(onClick = { viewModel.openEditCustomerDialog(customer) }) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = onClose) {
                                Icon(Icons.Default.Clear, contentDescription = "إغلاق")
                            }
                        }
                    }
                }

                // Balance Hero Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (hasDebt) DebtRedLight else ProfitGreenLight
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (hasDebt) "إجمالي المديونية المستحقة" else "الرصيد الحالي",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (hasDebt) Color(0xFF991B1B) else Color(0xFF166534)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${viewModel.formatMoney(customer.balance)} ${settings.currency}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (hasDebt) DebtRed else ProfitGreen
                            )
                            if (customer.phone.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "الهاتف: ${customer.phone}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (customer.address.isNotBlank()) {
                                Text(
                                    text = "العنوان: ${customer.address}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Action Buttons: [فاتورة جديدة] [سداد] [واتساب]
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.startCreditSaleForCustomer(customer) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("cust_action_credit_sale_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("كتابة فاتورة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            Button(
                                onClick = { viewModel.openCustomerPaymentDialog() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("cust_action_payment_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ProfitGreen
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("سداد دفعة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        if (customer.phone.isNotBlank()) {
                            Button(
                                onClick = { viewModel.sendWhatsAppReminder(context, customer) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("cust_action_whatsapp_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF25D366),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إرسال تذكير بالواتساب", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Section Selector Tabs: [فواتير العميل كاملة] & [حركات الحساب والمدفوعات]
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            elevation = null
                        ) {
                            Text("كل فواتير العميل (${customerInvoices.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.weight(1f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            elevation = null
                        ) {
                            Text("حركات الحساب (${transactions.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Tab Content 0: All Customer Invoices (Cash and Credit with Full Items)
                if (selectedTab == 0) {
                    if (customerInvoices.isEmpty()) {
                        item {
                            Text(
                                text = "لا توجد فواتير مسجلة لهذا العميل حتى الآن",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        items(customerInvoices) { invWithItems ->
                            val inv = invWithItems.invoice
                            val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH)
                            val dateStr = dateFormat.format(Date(inv.timestamp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.openInvoiceReceipt(invWithItems)
                                    },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("فاتورة #${inv.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Text(
                                            text = inv.paymentType.titleAr,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (inv.paymentType == PaymentType.CASH) ProfitGreen else DebtRed
                                        )
                                    }

                                    Text(
                                        text = "الأصناف: " + invWithItems.items.joinToString("، ") { "${it.productName} (${if (it.quantity % 1.0 == 0.0) String.format(Locale.ENGLISH, "%.0f", it.quantity) else it.quantity})" },
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("التاريخ: $dateStr", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = "${viewModel.formatMoney(inv.netAmount)} ${settings.currency}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Tab Content 1: Transactions Ledger
                    if (transactions.isEmpty()) {
                        item {
                            Text(
                                text = "لا توجد حركات مسجلة",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        items(transactions) { tx ->
                            val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH)
                            val dateStr = dateFormat.format(Date(tx.timestamp))
                            val isDebit = tx.type == CustomerTransactionType.SALE_CREDIT || 
                                (tx.type == CustomerTransactionType.DEBT_ADJUSTMENT && tx.newBalance > tx.previousBalance)

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.type.titleAr,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (isDebit) DebtRed else ProfitGreen
                                        )
                                        if (tx.notes.isNotBlank()) {
                                            Text(
                                                text = tx.notes,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = dateStr,
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${if (isDebit) "+" else "-"}${viewModel.formatMoney(tx.amount)} ${settings.currency}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isDebit) DebtRed else ProfitGreen
                                        )
                                        Text(
                                            text = "الرصيد: ${viewModel.formatMoney(tx.newBalance)} ${settings.currency}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
