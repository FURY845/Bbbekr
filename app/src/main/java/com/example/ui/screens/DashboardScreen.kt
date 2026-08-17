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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.InvoiceWithItems
import com.example.data.model.PaymentType
import com.example.ui.theme.*

import com.example.ui.viewmodel.AccountingViewModel
import com.example.ui.viewmodel.AppTab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: AccountingViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.storeSettings.collectAsState()
    val todaySales by viewModel.todaySalesTotal.collectAsState()
    val todayProfit by viewModel.todayProfitTotal.collectAsState()
    val safeBalance by viewModel.currentSafeBalance.collectAsState()
    val customerDebts by viewModel.totalCustomerDebts.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val recentInvoices by viewModel.allInvoicesWithItems.collectAsState()
    val todayInvoices by viewModel.todayInvoices.collectAsState()
    val todayExpenses by viewModel.todayExpensesTotal.collectAsState()

    val isDark = settings.isDarkMode

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp)
    ) {
        // 1. Bento Grid Hero Section
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Bento Row 1 (Top Section): Today's Activity (Emerald) & Net Profit (Blue)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Bento Tile 1: Today's Activity (Sales)
                    BentoTile(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_card_sales_today"),
                        title = "نشاط اليوم (المبيعات)",
                        amount = todaySales,
                        currency = settings.currency,
                        icon = Icons.Default.PointOfSale,
                        bgColor = if (isDark) BentoEmeraldBgDark else BentoEmeraldBgLight,
                        borderColor = if (isDark) BentoEmeraldBorderDark else BentoEmeraldBorderLight,
                        textColor = if (isDark) BentoEmeraldTextDark else BentoEmeraldTextLight,
                        iconBgColor = BentoEmeraldDark,
                        badgeText = "نشط اليوم",
                        viewModel = viewModel
                    )

                    // Bento Tile 2: Net Profit
                    BentoTile(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_card_profit_today"),
                        title = "صافي الأرباح",
                        amount = todayProfit,
                        currency = settings.currency,
                        icon = Icons.Default.TrendingUp,
                        bgColor = if (isDark) BentoBlueBgDark else BentoBlueBgLight,
                        borderColor = if (isDark) BentoBlueBorderDark else BentoBlueBorderLight,
                        textColor = if (isDark) BentoBlueTextDark else BentoBlueTextLight,
                        iconBgColor = BentoBlueDark,
                        badgeText = "صافي الربح",
                        viewModel = viewModel
                    )
                }

                // Bento Row 2: Treasury / Cash Balance (Indigo Gradient Hero Tile)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.setTab(AppTab.SAFE) }
                        .testTag("metric_card_safe_balance"),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(BentoIndigoDark, BentoIndigo, Color(0xFF6366F1))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "الرصيد النقدي المتوفر (الخزنة)",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = Color.White.copy(alpha = 0.9f)
                                        )
                                        Text(
                                            text = "جاهز للصرف والتسليم",
                                            fontSize = 10.sp,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = "الخزينة",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = viewModel.formatMoney(safeBalance),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 26.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = settings.currency,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }

                                OutlinedButton(
                                    onClick = { viewModel.setTab(AppTab.SAFE) },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color.White
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("عرض الحركات", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                // Bento Row 3: Customer Debts (Rose) - Bottom
                BentoTile(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("metric_card_customer_debts"),
                    title = "إجمالي ديون العملاء المستحقة",
                    amount = customerDebts,
                    currency = settings.currency,
                    icon = Icons.Default.People,
                    bgColor = if (isDark) BentoRoseBgDark else BentoRoseBgLight,
                    borderColor = if (isDark) BentoRoseBorderDark else BentoRoseBorderLight,
                    textColor = if (isDark) BentoRoseTextDark else BentoRoseTextLight,
                    iconBgColor = BentoRoseDark,
                    badgeText = "مطلوب تحصيلها",
                    onClick = { viewModel.setTab(AppTab.CUSTOMERS) },
                    viewModel = viewModel
                )
            }
        }

        // 2. Quick Actions Bento Row
        item {
            Text(
                text = "الإجراءات السريعة",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionBentoCard(
                    title = "إضافة صنف",
                    icon = Icons.Default.Add,
                    color = Color(0xFF0284C7),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_add_product"),
                    onClick = { viewModel.openAddProductDialog() }
                )

                QuickActionBentoCard(
                    title = "عميل جديد",
                    icon = Icons.Default.People,
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_add_customer"),
                    onClick = { viewModel.openAddCustomerDialog() }
                )

                QuickActionBentoCard(
                    title = "تسجيل مصروف",
                    icon = Icons.Default.MoneyOff,
                    color = Color(0xFFE11D48),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_add_expense"),
                    onClick = { viewModel.openAddExpenseDialog() }
                )

                QuickActionBentoCard(
                    title = "جرد اليوم",
                    icon = Icons.Default.Assessment,
                    color = Color(0xFF10B981),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_action_daily_inventory"),
                    onClick = { viewModel.openDailyInventoryDialog() }
                )
            }
        }

        // 3. Low Stock Alert Banner (if any)
        if (lowStockProducts.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .testTag("low_stock_banner"),
                    colors = CardDefaults.cardColors(containerColor = BentoRoseBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BentoRoseBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(BentoRoseDark),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "تنبيه: نواقص المخزون (${lowStockProducts.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BentoRoseText
                                )
                                Text(
                                    text = "أصناف وصلت للحد الأدنى من الرصيد",
                                    fontSize = 11.sp,
                                    color = BentoRoseText.copy(alpha = 0.8f)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.toggleShowOnlyLowStock()
                                viewModel.setTab(AppTab.PRODUCTS)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BentoRoseDark),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("عرض النواقص", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }

        // 4. Daily Summary Bento Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ملخص حركة اليوم",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        TextButton(
                            onClick = { viewModel.openDailyInventoryDialog() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("كشف الجرد اليومي", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatItem(
                            label = "عدد الفواتير",
                            value = "${todayInvoices.size} فاتورة",
                            color = MaterialTheme.colorScheme.primary
                        )
                        StatItem(
                            label = "مصروفات اليوم",
                            value = "${viewModel.formatMoney(todayExpenses)} ${settings.currency}",
                            color = DebtRed
                        )
                        StatItem(
                            label = "صافي الحركة",
                            value = "${viewModel.formatMoney(todaySales - todayExpenses)} ${settings.currency}",
                            color = ProfitGreen
                        )
                    }
                }
            }
        }

        // 5. Recent Invoices Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آخر فواتير المبيعات",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (recentInvoices.isNotEmpty()) {
                    Text(
                        text = "المجموع: ${recentInvoices.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (recentInvoices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "لا توجد فواتير بعد. ابدأ أول عملية بيع من شاشة الكاشير!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(recentInvoices.take(5)) { invoiceWithItems ->
                InvoiceCardItem(
                    invoiceWithItems = invoiceWithItems,
                    currency = settings.currency,
                    onViewReceipt = { viewModel.openInvoiceReceipt(invoiceWithItems) }
                )
            }
        }
    }
}

@Composable
private fun BentoTile(
    title: String,
    amount: Double,
    currency: String,
    icon: ImageVector,
    bgColor: Color,
    borderColor: Color,
    textColor: Color,
    iconBgColor: Color,
    badgeText: String,
    onClick: (() -> Unit)? = null,
    viewModel: AccountingViewModel? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(130.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = textColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.85f)
                )
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val formatted = if (viewModel != null) {
                        viewModel.formatMoney(amount)
                    } else if (amount % 1.0 == 0.0) {
                        String.format(Locale.ENGLISH, "%,.0f", amount)
                    } else {
                        String.format(Locale.ENGLISH, "%,.2f", amount)
                    }
                    Text(
                        text = formatted,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        color = textColor
                    )
                    Text(
                        text = currency,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionBentoCard(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(84.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun InvoiceCardItem(
    invoiceWithItems: InvoiceWithItems,
    currency: String,
    onViewReceipt: () -> Unit,
    modifier: Modifier = Modifier
) {
    val inv = invoiceWithItems.invoice
    val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH)
    val dateStr = dateFormat.format(Date(inv.timestamp))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onViewReceipt),
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (inv.paymentType == PaymentType.CREDIT) BentoRoseBg
                            else BentoEmeraldBg
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Receipt,
                        contentDescription = null,
                        tint = if (inv.paymentType == PaymentType.CREDIT) BentoRoseDark else BentoEmeraldDark,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = inv.customerName.ifBlank { "عميل نقدي" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (inv.paymentType == PaymentType.CREDIT) BentoRoseBg else BentoEmeraldBg
                        ) {
                            Text(
                                text = inv.paymentType.titleAr,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (inv.paymentType == PaymentType.CREDIT) BentoRoseDark else BentoEmeraldDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "${inv.invoiceNumber} • $dateStr",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format(Locale.ENGLISH, "%.2f", inv.netAmount)} $currency",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${invoiceWithItems.items.size} أصناف",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

