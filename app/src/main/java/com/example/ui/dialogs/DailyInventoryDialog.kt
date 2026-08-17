package com.example.ui.dialogs

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.InventoryLog
import com.example.data.model.MovementType
import com.example.ui.theme.DebtRed
import com.example.ui.theme.DebtRedLight
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.ProfitGreenLight
import com.example.ui.viewmodel.AccountingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DailyInventoryDialog(
    viewModel: AccountingViewModel,
    onDismiss: () -> Unit
) {
    val logs by viewModel.todayInventoryLogs.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val todayOutflow by viewModel.todayOutflowByProduct.collectAsState()
    val settings by viewModel.storeSettings.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: All Products Stock, 1: Movement Logs

    val totalIn = logs.filter { it.movementType == MovementType.IN_PURCHASE }.sumOf { it.quantity }
    val totalOut = logs.filter { it.movementType == MovementType.OUT_SALE }.sumOf { it.quantity }

    Dialog(onDismissRequest = onDismiss) {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Assessment, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "كشف جرد الأصناف وحركة المخزون",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Clear, contentDescription = "إغلاق")
                        }
                    }
                }

                // Inflow / Outflow Summary Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = ProfitGreenLight)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(ProfitGreen),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CallReceived, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("وارد المخزون (+)", fontSize = 11.sp, color = Color(0xFF166534))
                                    Text("+${viewModel.formatMoney(totalIn)} قطعة", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ProfitGreen)
                                }
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = DebtRedLight)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(DebtRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CallMade, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("منصرف المبيعات (-)", fontSize = 11.sp, color = Color(0xFF991B1B))
                                    Text("-${viewModel.formatMoney(totalOut)} قطعة", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DebtRed)
                                }
                            }
                        }
                    }
                }

                // Segmented Tabs: [جرد رصيد كل الأصناف بعد شغل اليوم] & [سجل الحركات المنفذة اليوم]
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
                            modifier = Modifier.weight(1.2f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            elevation = null
                        ) {
                            Text("رصيد الأصناف الحالي (${allProducts.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.weight(0.9f).height(38.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent,
                                contentColor = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            elevation = null
                        ) {
                            Text("حركات اليوم (${logs.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Tab 0: All Products Inventory Status (shows remaining quantity after today's sales)
                if (selectedTab == 0) {
                    if (allProducts.isEmpty()) {
                        item {
                            Text(
                                text = "لا توجد أصناف مسجلة في المخزن",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        items(allProducts, key = { it.id }) { product ->
                            val soldToday = todayOutflow[product.id] ?: 0.0
                            val isLowStock = product.stockQuantity <= product.minStockAlert

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isLowStock) DebtRedLight.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
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
                                            text = product.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "القسم: ${product.category} • السعر: ${viewModel.formatMoney(product.sellPrice)} ${settings.currency}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (soldToday > 0) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "خرج اليوم: ${viewModel.formatMoney(soldToday)} ${product.unit}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = DebtRed
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "الرصيد المتبقي",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${viewModel.formatMoney(product.stockQuantity)} ${product.unit}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (isLowStock) DebtRed else ProfitGreen
                                        )
                                        if (isLowStock) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Warning, contentDescription = null, tint = DebtRed, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("نواقص", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DebtRed)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Tab 1: Detailed Logs
                    if (logs.isEmpty()) {
                        item {
                            Text(
                                text = "لا توجد حركات مخزنية مسجلة لليوم",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        items(logs, key = { it.id }) { log ->
                            val isOut = log.movementType == MovementType.OUT_SALE
                            val dateFormat = SimpleDateFormat("HH:mm", Locale.ENGLISH)
                            val timeStr = dateFormat.format(Date(log.timestamp))

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
                                            text = log.productName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${log.movementType.titleAr} • $timeStr",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (log.notes.isNotBlank()) {
                                            Text(
                                                text = log.notes,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${if (isOut) "-" else "+"}${viewModel.formatMoney(log.quantity)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isOut) DebtRed else ProfitGreen
                                        )
                                        Text(
                                            text = "المتبقي: ${viewModel.formatMoney(log.newStock)}",
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
