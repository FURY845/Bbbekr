package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.widget.Toast
import com.example.data.local.InvoiceWithItems
import com.example.data.model.StoreSettings
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PrinterHelper {

    fun printInvoiceA5(
        context: Context,
        invoiceWithItems: InvoiceWithItems,
        settings: StoreSettings,
        includeDebt: Boolean = true,
        template: String = settings.invoiceTemplate
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
        if (printManager == null) {
            Toast.makeText(context, "خدمة الطباعة غير متاحة في هذا الجهاز", Toast.LENGTH_SHORT).show()
            return
        }

        val jobName = "فاتورة_${invoiceWithItems.invoice.invoiceNumber}"
        val printAdapter = InvoiceA5PrintAdapter(context, invoiceWithItems, settings, includeDebt, template)

        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A5)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(jobName, printAdapter, printAttributes)
    }

    private class InvoiceA5PrintAdapter(
        private val context: Context,
        private val invoiceWithItems: InvoiceWithItems,
        private val settings: StoreSettings,
        private val includeDebt: Boolean,
        private val template: String
    ) : PrintDocumentAdapter() {

        private var pdfDocument: PrintedPdfDocument? = null

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }

            pdfDocument = PrintedPdfDocument(context, newAttributes)
            val info = PrintDocumentInfo.Builder("فاتورة_${invoiceWithItems.invoice.invoiceNumber}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(1)
                .build()

            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            val doc = pdfDocument ?: return
            val page = doc.startPage(0)

            if (cancellationSignal?.isCanceled == true) {
                callback.onWriteCancelled()
                doc.close()
                return
            }

            val canvas = page.canvas
            drawInvoiceCanvas(canvas, page.info.pageWidth, page.info.pageHeight)

            doc.finishPage(page)

            try {
                doc.writeTo(FileOutputStream(destination.fileDescriptor))
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            } finally {
                doc.close()
                pdfDocument = null
            }
        }

        private fun drawInvoiceCanvas(canvas: Canvas, width: Int, height: Int) {
            val inv = invoiceWithItems.invoice
            val items = invoiceWithItems.items

            // Background
            canvas.drawColor(Color.WHITE)

            val margin = 28f
            val rightX = width - margin
            var currentY = 40f

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 12f
                textAlign = Paint.Align.RIGHT
            }

            val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }

            val leftAlignPaint = Paint(paint).apply { textAlign = Paint.Align.LEFT }
            val leftBoldPaint = Paint(boldPaint).apply { textAlign = Paint.Align.LEFT }

            when (template) {
                "ROYAL" -> {
                    // ROYAL TEMPLATE: Elegant Navy & Gold styling
                    val headerPaint = Paint().apply { color = Color.rgb(30, 58, 138) } // Navy
                    canvas.drawRect(margin, 25f, rightX, 90f, headerPaint)

                    val goldBorder = Paint().apply {
                        color = Color.rgb(217, 119, 6)
                        style = Paint.Style.STROKE
                        strokeWidth = 2f
                    }
                    canvas.drawRect(margin, 25f, rightX, 90f, goldBorder)

                    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.WHITE
                        textSize = 20f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(settings.storeName, width / 2f, 55f, titlePaint)

                    val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(254, 240, 138) // Soft Gold
                        textSize = 11f
                        textAlign = Paint.Align.CENTER
                    }
                    val subText = "${if (settings.phone.isNotBlank()) "هاتف: ${settings.phone}  •  " else ""}${settings.address}"
                    canvas.drawText(subText, width / 2f, 78f, subPaint)
                    currentY = 115f
                }
                "MINIMAL" -> {
                    // MINIMAL TEMPLATE: Clean Monochromatic
                    val minimalTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.BLACK
                        textSize = 20f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.RIGHT
                    }
                    canvas.drawText(settings.storeName, rightX, currentY, minimalTitlePaint)
                    currentY += 20f

                    if (settings.phone.isNotBlank() || settings.address.isNotBlank()) {
                        paint.textSize = 11f
                        paint.color = Color.DKGRAY
                        canvas.drawText("${if (settings.phone.isNotBlank()) "هاتف: ${settings.phone} | " else ""}${settings.address}", rightX, currentY, paint)
                        currentY += 18f
                    }

                    val linePaint = Paint().apply { color = Color.BLACK; strokeWidth = 1.5f }
                    canvas.drawLine(margin, currentY, rightX, currentY, linePaint)
                    currentY += 20f
                }
                else -> {
                    // MODERN TEMPLATE (Default): Emerald Accents
                    val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.rgb(16, 185, 129) // Emerald
                        textSize = 22f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        textAlign = Paint.Align.RIGHT
                    }
                    canvas.drawText(settings.storeName, rightX, currentY, primaryPaint)
                    currentY += 22f

                    if (settings.phone.isNotBlank() || settings.address.isNotBlank()) {
                        val subText = "${if (settings.phone.isNotBlank()) "هاتف: ${settings.phone} • " else ""}${settings.address}"
                        paint.textSize = 11f
                        paint.color = Color.rgb(100, 116, 139)
                        canvas.drawText(subText, rightX, currentY, paint)
                        currentY += 18f
                    }

                    val linePaint = Paint().apply { color = Color.rgb(226, 232, 240); strokeWidth = 2f }
                    canvas.drawLine(margin, currentY, rightX, currentY, linePaint)
                    currentY += 20f
                }
            }

            // Invoice Info Bar
            val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.ENGLISH)
            val dateStr = dateFormat.format(Date(inv.timestamp))

            boldPaint.textSize = 12f
            canvas.drawText("فاتورة رقم: #${inv.invoiceNumber}", rightX, currentY, boldPaint)
            canvas.drawText("التاريخ: $dateStr", margin, currentY, leftAlignPaint)
            currentY += 18f

            canvas.drawText("العميل: ${inv.customerName}", rightX, currentY, boldPaint)
            canvas.drawText("طريقة السداد: ${inv.paymentType.titleAr}", margin, currentY, leftAlignPaint)
            currentY += 22f

            // Table Header Background
            val tableBgPaint = Paint().apply {
                color = if (template == "ROYAL") Color.rgb(238, 242, 255) else Color.rgb(241, 245, 249)
            }
            canvas.drawRect(margin, currentY - 12f, rightX, currentY + 10f, tableBgPaint)

            boldPaint.textSize = 11f
            boldPaint.color = if (template == "ROYAL") Color.rgb(30, 58, 138) else Color.rgb(51, 65, 85)
            canvas.drawText("الصنف", rightX - 8f, currentY, boldPaint)
            canvas.drawText("الكمية", rightX - 130f, currentY, boldPaint)
            canvas.drawText("السعر", rightX - 200f, currentY, boldPaint)
            canvas.drawText("الإجمالي", margin + 8f, currentY, Paint(boldPaint).apply { textAlign = Paint.Align.LEFT })
            currentY += 20f

            // Table Items
            paint.color = Color.rgb(30, 41, 59)
            paint.textSize = 11f
            val itemLinePaint = Paint().apply { color = Color.rgb(241, 245, 249); strokeWidth = 1f }

            items.forEach { item ->
                canvas.drawText(item.productName, rightX - 8f, currentY, paint)
                val qtyStr = if (item.quantity % 1.0 == 0.0) String.format(Locale.ENGLISH, "%.0f", item.quantity) else String.format(Locale.ENGLISH, "%.2f", item.quantity)
                canvas.drawText("$qtyStr ${item.unit}", rightX - 130f, currentY, paint)
                val priceStr = formatAmount(item.unitPrice)
                canvas.drawText(priceStr, rightX - 200f, currentY, paint)
                val totalStr = formatAmount(item.total)
                canvas.drawText(totalStr, margin + 8f, currentY, leftAlignPaint)

                currentY += 16f
                canvas.drawLine(margin, currentY - 4f, rightX, currentY - 4f, itemLinePaint)
            }

            currentY += 12f
            val dividerPaint = Paint().apply { color = Color.rgb(203, 213, 225); strokeWidth = 1.5f }
            canvas.drawLine(margin, currentY, rightX, currentY, dividerPaint)
            currentY += 20f

            // Financial Summary
            boldPaint.textSize = 12f
            boldPaint.color = Color.rgb(15, 23, 42)
            canvas.drawText("إجمالي الفاتورة:", rightX - 90f, currentY, boldPaint)
            canvas.drawText("${formatAmount(inv.netAmount)} ${settings.currency}", margin + 8f, currentY, leftBoldPaint)
            currentY += 18f

            if (includeDebt && inv.previousDebt > 0) {
                val redBold = Paint(boldPaint).apply { color = Color.rgb(220, 38, 38) }
                val redLeftBold = Paint(leftBoldPaint).apply { color = Color.rgb(220, 38, 38) }
                canvas.drawText("دين سابق مستحق:", rightX - 90f, currentY, redBold)
                canvas.drawText("${formatAmount(inv.previousDebt)} ${settings.currency}", margin + 8f, currentY, redLeftBold)
                currentY += 18f

                val grandTotal = inv.netAmount + inv.previousDebt
                canvas.drawText("المطلوب كلياً:", rightX - 90f, currentY, boldPaint)
                canvas.drawText("${formatAmount(grandTotal)} ${settings.currency}", margin + 8f, currentY, leftBoldPaint)
                currentY += 18f
            }

            canvas.drawText("المدفوع نقداً:", rightX - 90f, currentY, boldPaint)
            canvas.drawText("${formatAmount(inv.paidAmount)} ${settings.currency}", margin + 8f, currentY, leftBoldPaint)
            currentY += 18f

            if (inv.remainingAmount > 0) {
                val redBold = Paint(boldPaint).apply { color = Color.rgb(220, 38, 38) }
                val redLeftBold = Paint(leftBoldPaint).apply { color = Color.rgb(220, 38, 38) }
                canvas.drawText("المتبقي آجل:", rightX - 90f, currentY, redBold)
                canvas.drawText("${formatAmount(inv.remainingAmount)} ${settings.currency}", margin + 8f, currentY, redLeftBold)
                currentY += 18f
            }

            // Footer note
            if (settings.invoiceFooterNote.isNotBlank()) {
                currentY += 18f
                paint.textSize = 10f
                paint.color = Color.rgb(100, 116, 139)
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(settings.invoiceFooterNote, width / 2f, currentY, paint)
            }
        }

        private fun formatAmount(amount: Double): String {
            return if (amount % 1.0 == 0.0) {
                String.format(Locale.ENGLISH, "%,.0f", amount)
            } else {
                String.format(Locale.ENGLISH, "%,.2f", amount)
            }
        }
    }
}
