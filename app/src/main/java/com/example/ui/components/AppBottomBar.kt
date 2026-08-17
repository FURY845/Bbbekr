package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DebtRed
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.AppTab

@Composable
fun AppBottomBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    cartItemCount: Double = 0.0,
    lowStockCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                tab = AppTab.DASHBOARD,
                selected = currentTab == AppTab.DASHBOARD,
                label = "الرئيسية",
                selectedIcon = Icons.Filled.Dashboard,
                unselectedIcon = Icons.Outlined.Dashboard,
                badgeCount = 0,
                onClick = { onTabSelected(AppTab.DASHBOARD) }
            )

            BottomNavItem(
                tab = AppTab.POS,
                selected = currentTab == AppTab.POS,
                label = "المبيعات",
                selectedIcon = Icons.Filled.PointOfSale,
                unselectedIcon = Icons.Outlined.PointOfSale,
                badgeCount = cartItemCount.toInt(),
                badgeColor = EmeraldPrimary,
                onClick = { onTabSelected(AppTab.POS) }
            )

            BottomNavItem(
                tab = AppTab.PRODUCTS,
                selected = currentTab == AppTab.PRODUCTS,
                label = "المنتجات",
                selectedIcon = Icons.Filled.Inventory2,
                unselectedIcon = Icons.Outlined.Inventory2,
                badgeCount = lowStockCount,
                badgeColor = DebtRed,
                onClick = { onTabSelected(AppTab.PRODUCTS) }
            )

            BottomNavItem(
                tab = AppTab.CUSTOMERS,
                selected = currentTab == AppTab.CUSTOMERS,
                label = "العملاء",
                selectedIcon = Icons.Filled.People,
                unselectedIcon = Icons.Outlined.People,
                badgeCount = 0,
                onClick = { onTabSelected(AppTab.CUSTOMERS) }
            )

            BottomNavItem(
                tab = AppTab.SAFE,
                selected = currentTab == AppTab.SAFE,
                label = "الخزنة",
                selectedIcon = Icons.Filled.AccountBalanceWallet,
                unselectedIcon = Icons.Outlined.AccountBalanceWallet,
                badgeCount = 0,
                onClick = { onTabSelected(AppTab.SAFE) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    tab: AppTab,
    selected: Boolean,
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    badgeCount: Int = 0,
    badgeColor: Color = EmeraldPrimary,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)

    val animatedIconColor by animateColorAsState(
        targetValue = if (selected) activeColor else inactiveColor,
        label = "nav_icon_color"
    )

    Column(
        modifier = Modifier
            .testTag("nav_tab_${tab.name.lowercase()}")
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, radius = 28.dp),
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = 32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge(
                            containerColor = badgeColor,
                            contentColor = Color.White
                        ) {
                            Text(
                                text = "$badgeCount",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (selected) selectedIcon else unselectedIcon,
                    contentDescription = label,
                    tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else inactiveColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = animatedIconColor
        )
    }
}
