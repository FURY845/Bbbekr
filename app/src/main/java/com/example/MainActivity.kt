package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.components.AppBottomBar
import com.example.ui.components.AppTopBar
import com.example.ui.dialogs.AddEditCustomerDialog
import com.example.ui.dialogs.AddEditProductDialog
import com.example.ui.dialogs.AddExpenseDialog
import com.example.ui.dialogs.AddStockDialog
import com.example.ui.dialogs.BackupExportDialog
import com.example.ui.dialogs.BackupImportDialog
import com.example.ui.dialogs.BarcodeScannerDialog
import com.example.ui.dialogs.ChangePasscodeDialog
import com.example.ui.dialogs.CustomerPaymentDialog
import com.example.ui.dialogs.DailyInventoryDialog
import com.example.ui.dialogs.ReceiptDialog
import com.example.ui.dialogs.SafeDepositDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LockScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.SafeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AccountingViewModel
import com.example.ui.viewmodel.AppTab

class MainActivity : ComponentActivity() {
    private val viewModel: AccountingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val storeSettings by viewModel.storeSettings.collectAsState()
            val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()

            MyApplicationTheme(darkTheme = storeSettings.isDarkMode) {
                // Ensure RTL layout for Arabic accounting app
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    if (storeSettings.isAppLockEnabled && !isAppUnlocked) {
                        LockScreen(
                            storeSettings = storeSettings,
                            onUnlocked = { viewModel.unlockApp() }
                        )
                    } else {
                        AccountingApp(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun AccountingApp(viewModel: AccountingViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    val cartState by viewModel.cartState.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()

    // Dialog state collectors
    val showProductDialog by viewModel.showProductDialog.collectAsState()
    val editingProduct by viewModel.editingProduct.collectAsState()

    val showAddStockDialog by viewModel.showAddStockDialog.collectAsState()
    val selectedProductForStock by viewModel.selectedProductForStock.collectAsState()

    val showCustomerDialog by viewModel.showCustomerDialog.collectAsState()
    val editingCustomer by viewModel.editingCustomer.collectAsState()

    val showCustomerPaymentDialog by viewModel.showCustomerPaymentDialog.collectAsState()
    val selectedCustomerForProfile by viewModel.selectedCustomerForProfile.collectAsState()

    val showSafeDialog by viewModel.showSafeDialog.collectAsState()
    val isSafeDepositMode by viewModel.isSafeDepositMode.collectAsState()

    val showDailyInventoryDialog by viewModel.showDailyInventoryDialog.collectAsState()
    val showBarcodeScannerDialog by viewModel.showBarcodeScannerDialog.collectAsState()
    val showSettingsDialog by viewModel.showSettingsDialog.collectAsState()
    val showChangePasscodeDialog by viewModel.showChangePasscodeDialog.collectAsState()
    val showBackupExportDialog by viewModel.showBackupExportDialog.collectAsState()
    val showBackupImportDialog by viewModel.showBackupImportDialog.collectAsState()

    val showThermalReceiptDialog by viewModel.showThermalReceiptDialog.collectAsState()
    val activeReceiptInvoice by viewModel.activeReceiptInvoice.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                storeSettings = storeSettings,
                isDarkMode = storeSettings.isDarkMode,
                onToggleDarkMode = { viewModel.toggleDarkMode() },
                onOpenSettings = { viewModel.openSettingsDialog() },
                onOpenDailyInventory = { viewModel.openDailyInventoryDialog() }
            )
        },
        bottomBar = {
            AppBottomBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.setTab(it) },
                cartItemCount = cartState.totalItemsCount,
                lowStockCount = lowStockProducts.size
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Crossfade(
                targetState = currentTab,
                label = "screen_transition"
            ) { tab ->
                when (tab) {
                    AppTab.DASHBOARD -> DashboardScreen(viewModel = viewModel)
                    AppTab.POS -> PosScreen(viewModel = viewModel)
                    AppTab.PRODUCTS -> ProductsScreen(viewModel = viewModel)
                    AppTab.CUSTOMERS -> CustomersScreen(viewModel = viewModel)
                    AppTab.SAFE -> SafeScreen(viewModel = viewModel)
                }
            }
        }
    }

    // =========================================================
    // Modals & Dialogs
    // =========================================================

    // Add / Edit Product
    if (showProductDialog) {
        AddEditProductDialog(
            product = editingProduct,
            currency = storeSettings.currency,
            onDismiss = { viewModel.closeProductDialog() },
            onSave = { viewModel.saveProduct(it) },
            onDelete = if (editingProduct != null) { { viewModel.deleteProduct(it) } } else null
        )
    }

    // Add Stock to Product
    if (showAddStockDialog && selectedProductForStock != null) {
        AddStockDialog(
            product = selectedProductForStock!!,
            currency = storeSettings.currency,
            onDismiss = { viewModel.closeAddStockDialog() },
            onConfirm = { addedQty, purchaseCost, payFromSafe, notes ->
                viewModel.addProductStock(addedQty, purchaseCost, payFromSafe, notes)
            }
        )
    }

    // Add / Edit Customer
    if (showCustomerDialog) {
        AddEditCustomerDialog(
            customer = editingCustomer,
            currency = storeSettings.currency,
            onDismiss = { viewModel.closeCustomerDialog() },
            onSave = { viewModel.saveCustomer(it) },
            onDeleteRequest = { viewModel.requestDeleteCustomer(it) }
        )
    }

    // Customer Payment
    if (showCustomerPaymentDialog && selectedCustomerForProfile != null) {
        CustomerPaymentDialog(
            customer = selectedCustomerForProfile!!,
            currency = storeSettings.currency,
            onDismiss = { viewModel.closeCustomerPaymentDialog() },
            onConfirm = { amount, depositToSafe, notes ->
                viewModel.recordCustomerPayment(amount, depositToSafe, notes)
            }
        )
    }

    // Safe Deposit or Expense
    if (showSafeDialog) {
        if (isSafeDepositMode) {
            SafeDepositDialog(
                currency = storeSettings.currency,
                onDismiss = { viewModel.closeSafeDialog() },
                onConfirm = { amount, category, notes ->
                    viewModel.recordSafeTransaction(
                        type = com.example.data.model.SafeTransactionType.INCOME_DEPOSIT,
                        amount = amount,
                        category = category,
                        notes = notes
                    )
                }
            )
        } else {
            AddExpenseDialog(
                currency = storeSettings.currency,
                onDismiss = { viewModel.closeSafeDialog() },
                onConfirm = { amount, category, notes ->
                    viewModel.recordSafeTransaction(
                        type = com.example.data.model.SafeTransactionType.EXPENSE_GENERAL,
                        amount = amount,
                        category = category,
                        notes = notes
                    )
                }
            )
        }
    }

    // Daily Inventory Dialog
    if (showDailyInventoryDialog) {
        DailyInventoryDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeDailyInventoryDialog() }
        )
    }

    // Barcode Scanner Dialog
    if (showBarcodeScannerDialog) {
        BarcodeScannerDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.closeBarcodeScanner() }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            settings = storeSettings,
            onDismiss = { viewModel.closeSettingsDialog() },
            onSave = { viewModel.saveSettings(it) },
            onOpenChangePasscode = { viewModel.openChangePasscodeDialog() },
            onOpenBackupExport = { viewModel.openBackupExportDialog() },
            onOpenBackupImport = { viewModel.openBackupImportDialog() },
            onLockAppNow = { viewModel.lockApp() }
        )
    }

    // Change Passcode & Security Dialog
    if (showChangePasscodeDialog) {
        ChangePasscodeDialog(
            settings = storeSettings,
            onDismiss = { viewModel.closeChangePasscodeDialog() },
            onSave = { viewModel.saveSettings(it) }
        )
    }

    // Encrypted Backup Export Dialog ("سحب ملف المعلومات")
    if (showBackupExportDialog) {
        BackupExportDialog(
            settings = storeSettings,
            viewModel = viewModel,
            onDismiss = { viewModel.closeBackupExportDialog() }
        )
    }

    // Encrypted Backup Import Dialog ("وضع ملف معلومات")
    if (showBackupImportDialog) {
        BackupImportDialog(
            settings = storeSettings,
            viewModel = viewModel,
            onDismiss = { viewModel.closeBackupImportDialog() },
            onSuccess = {
                // Refresh or handle post-import if needed
            }
        )
    }

    // Thermal / A4 Receipt & Invoice Dialog
    if (showThermalReceiptDialog && activeReceiptInvoice != null) {
        ReceiptDialog(
            invoiceWithItems = activeReceiptInvoice!!,
            viewModel = viewModel,
            onClose = { viewModel.closeReceiptDialog() }
        )
    }
}
