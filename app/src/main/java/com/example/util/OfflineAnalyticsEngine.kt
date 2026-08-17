package com.example.util

import com.example.data.local.InvoiceWithItems
import com.example.data.model.Customer
import com.example.data.model.InventoryLog
import com.example.data.model.Invoice
import com.example.data.model.MovementType
import com.example.data.model.Product
import com.example.data.model.SafeTransaction
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class StoreInsightItem(
    val title: String,
    val description: String,
    val badge: String,
    val type: InsightType
)

enum class InsightType {
    SUCCESS, WARNING, INFO, ALERT
}

data class LocalSmartAnalysis(
    val summaryText: String,
    val topSellingProducts: List<Pair<String, Double>>,
    val lowStockAlerts: List<Product>,
    val profitMarginPercentage: Double,
    val averageInvoiceValue: Double,
    val highDebtCustomers: List<Customer>,
    val recommendations: List<String>
)

object OfflineAnalyticsEngine {

    fun formatNumber(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale.ENGLISH)
        return if (amount % 1.0 == 0.0) {
            DecimalFormat("#,##0", symbols).format(amount)
        } else {
            DecimalFormat("#,##0.##", symbols).format(amount)
        }
    }

    /**
     * Generates a 100% offline smart accounting analysis from local Room data.
     */
    fun analyzeStoreData(
        products: List<Product>,
        invoicesWithItems: List<InvoiceWithItems>,
        customers: List<Customer>,
        safeTransactions: List<SafeTransaction>,
        currency: String
    ): LocalSmartAnalysis {
        val allItems = invoicesWithItems.flatMap { it.items }
        val totalSales = invoicesWithItems.sumOf { it.invoice.netAmount }
        val totalProfit = invoicesWithItems.sumOf { it.invoice.totalProfit }
        val invoiceCount = invoicesWithItems.size

        // 1. Top Selling Products
        val productSalesMap = mutableMapOf<String, Double>()
        allItems.forEach { item ->
            productSalesMap[item.productName] = (productSalesMap[item.productName] ?: 0.0) + item.quantity
        }
        val topSelling = productSalesMap.toList().sortedByDescending { it.second }.take(5)

        // 2. Low Stock Alerts
        val lowStock = products.filter { it.stockQuantity <= it.minStockAlert }

        // 3. Profit Margins
        val profitMargin = if (totalSales > 0) (totalProfit / totalSales) * 100.0 else 0.0
        val avgInvoice = if (invoiceCount > 0) totalSales / invoiceCount else 0.0

        // 4. High Debt Customers
        val highDebt = customers.filter { it.balance > 0 }.sortedByDescending { it.balance }.take(5)

        // 5. Recommendations
        val recs = mutableListOf<String>()
        if (lowStock.isNotEmpty()) {
            recs.add("يوجد ${lowStock.size} أصناف وصلت لحد إعادة الطلب، يُوصى بتوريد مخزون جديد فوراً.")
        }
        if (highDebt.isNotEmpty()) {
            val totalDebt = customers.sumOf { it.balance }
            recs.add("إجمالي الديون المستحقة ${formatNumber(totalDebt)} $currency، يُنصح بتفعيل إرسال تذكيرات الواتساب للعملاء لتسريع التحصيل.")
        }
        if (profitMargin in 1.0..15.0) {
            recs.add("هامش الربح الحالي ${String.format(Locale.ENGLISH, "%.1f", profitMargin)}%، يمكن مراجعة أسعار بيع بعض الأصناف لمواكبة تكاليف الشراء.")
        } else if (profitMargin > 15.0) {
            recs.add("هامش الربح ممتاز ويبلغ ${String.format(Locale.ENGLISH, "%.1f", profitMargin)}%، مما يعكس كفاءة إدارة المخزون والتسعير.")
        }
        if (topSelling.isNotEmpty()) {
            val best = topSelling.first()
            recs.add("الصنف الأكثر طلباً هو '${best.first}' بمبيعات ${formatNumber(best.second)} قطعة، يُفضل الحفاظ على وفرة دائمة له بالمستودع.")
        }

        val summary = """
📊 تقرير المساعد المحاسبي الذكي (محلي 100%):
• إجمالي المبيعات: ${formatNumber(totalSales)} $currency
• صافي الأرباح: ${formatNumber(totalProfit)} $currency (هامش ربح ${String.format(Locale.ENGLISH, "%.1f", profitMargin)}%)
• متوسط قيمة الفاتورة: ${formatNumber(avgInvoice)} $currency
• عدد الأصناف بالمخزن: ${products.size} صنف (منها ${lowStock.size} ناقص)
• إجمالي الديون المستحقة: ${formatNumber(customers.sumOf { it.balance })} $currency
        """.trimIndent()

        return LocalSmartAnalysis(
            summaryText = summary,
            topSellingProducts = topSelling,
            lowStockAlerts = lowStock,
            profitMarginPercentage = profitMargin,
            averageInvoiceValue = avgInvoice,
            highDebtCustomers = highDebt,
            recommendations = recs
        )
    }

    /**
     * Smart local algorithm to suggest pricing based on purchase cost and target profit margins.
     */
    fun suggestPrice(costPrice: Double, category: String): Pair<Double, Double> {
        val standardMargin = when (category) {
            "مواد غذائية", "مخبوزات" -> 0.20 // 20%
            "مشروبات" -> 0.25 // 25%
            "حلويات" -> 0.30 // 30%
            else -> 0.22
        }
        val suggestedSell = if (costPrice > 0) Math.ceil(costPrice * (1.0 + standardMargin)) else 0.0
        val minSell = if (costPrice > 0) Math.ceil(costPrice * (1.0 + (standardMargin * 0.5))) else 0.0
        return Pair(suggestedSell, minSell)
    }

    /**
     * Generates a rich Arabic product description locally.
     */
    fun generateProductDescription(name: String, category: String, unit: String, sellPrice: Double, currency: String): String {
        val priceStr = formatNumber(sellPrice)
        return "منتج $name عالي الجودة من قسم $category، يُباع بالـ$unit بسعر $priceStr $currency. متوفر بالمخزن بجودة مضمونة وتعبئة ممتازة."
    }
}
