package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.data.model.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptGenerator {

    fun generateMeasurementReceiptBitmap(
        context: Context,
        shopName: String,
        shopPhone: String,
        shopAddress: String,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        sheetTitle: String,
        templateLabel: String,
        fields: List<MeasurementField>,
        values: List<MeasurementValue>,
        lastEditedDate: Long
    ): Bitmap {
        val mFields = fields.filter { !it.isDemand }
        val dFields = fields.filter { it.isDemand }
        
        val leftHeight = 35f + (mFields.size * 32f)
        val rightHeight = 35f + (dFields.size * 32f)
        val gridHeight = maxOf(leftHeight, rightHeight) + 20f
        
        val width = 800
        val colWidth = (width - 60) / 2f
        
        val headerHeight = 180f
        val metaHeight = 160f
        val footerHeight = 110f
        
        val globalNote = values.find { it.fieldId == "global_notes_field" }?.value ?: ""
        val noteBlockHeight = if (globalNote.isNotBlank()) 90f else 0f
        
        val height = (headerHeight + metaHeight + gridHeight + noteBlockHeight + footerHeight).toInt().coerceAtLeast(500)
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        canvas.drawColor(Color.parseColor("#FCFAF2"))
        
        val borderPaint = Paint().apply {
            color = Color.parseColor("#E5DECE")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(8f, 8f, width - 8f, height - 8f, borderPaint)
        
        var y = 40f
        
        val shopTitlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#8B6B3F")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(shopName.ifEmpty { "Tailor Atelier" }.uppercase(Locale.getDefault()), width / 2f, y, shopTitlePaint)
        y += 26f
        
        val tagPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#5E584E")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("SIZING MASTER RECORD", width / 2f, y, tagPaint)
        y += 24f
        
        val miniTagPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#726B5F")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        if (shopAddress.isNotEmpty()) {
            canvas.drawText(shopAddress, width / 2f, y, miniTagPaint)
            y += 22f
        }
        val phoneText = "Phone: ${shopPhone.ifEmpty { "+00 000 000" }}"
        canvas.drawText(phoneText, width / 2f, y, miniTagPaint)
        y += 24f
        
        val thickDividerPaint = Paint().apply {
            color = Color.parseColor("#8B6B3F")
            strokeWidth = 4f
        }
        canvas.drawLine(20f, y, width - 20f, y, thickDividerPaint)
        y += 25f
        
        val labelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#8B6B3F")
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valuePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#1B1A18")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaintLight = Paint().apply {
            color = Color.parseColor("#E5DECE")
            strokeWidth = 1.5f
        }
        
        // Row 1: Name and Address
        canvas.drawText("Name:", 30f, y + 25f, labelPaint)
        val nameToDraw = customerName.ifEmpty { "GENERAL CUSTOMER" }.uppercase(Locale.getDefault())
        canvas.drawText(nameToDraw, 80f, y + 23f, valuePaint)
        canvas.drawLine(78f, y + 28f, width / 2f - 30f, y + 28f, linePaintLight)

        canvas.drawText("Address:", width / 2f + 10f, y + 25f, labelPaint)
        val addrStr = customerAddress.ifEmpty { "N/A" }
        canvas.drawText(addrStr.uppercase(Locale.getDefault()), width / 2f + 85f, y + 23f, valuePaint)
        canvas.drawLine(width / 2f + 83f, y + 28f, width - 30f, y + 28f, linePaintLight)

        y += 42f

        // Row 2: Number
        canvas.drawText("Number:", 30f, y + 25f, labelPaint)
        val numToDraw = customerPhone.ifEmpty { "N/A" }
        canvas.drawText(numToDraw, 100f, y + 23f, valuePaint)
        canvas.drawLine(98f, y + 28f, width - 30f, y + 28f, linePaintLight)

        y += 42f

        // Row 3: Card Info and Date
        canvas.drawText("Card Info:", 30f, y + 25f, labelPaint)
        val recordToDraw = sheetTitle.ifEmpty { "PRIMARY" }.uppercase(Locale.getDefault())
        val recordPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#8B6B3F")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(recordToDraw, 115f, y + 23f, recordPaint)
        canvas.drawLine(113f, y + 28f, width / 2f - 30f, y + 28f, linePaintLight)

        canvas.drawText("Date:", width / 2f + 10f, y + 25f, labelPaint)
        val lastEditedStr = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(Date(lastEditedDate))
        canvas.drawText(lastEditedStr, width / 2f + 55f, y + 23f, valuePaint)
        canvas.drawLine(width / 2f + 53f, y + 28f, width - 30f, y + 28f, linePaintLight)

        y += 42f

        // Row 4: Template Name
        canvas.drawText("Template Name:", 30f, y + 25f, labelPaint)
        val tempToDraw = templateLabel.ifEmpty { "Default" }.uppercase(Locale.getDefault())
        canvas.drawText(tempToDraw, 145f, y + 23f, valuePaint)
        canvas.drawLine(143f, y + 28f, width - 30f, y + 28f, linePaintLight)

        y += 50f
        
        val linePaint = Paint().apply {
            color = Color.parseColor("#E5DECE")
            strokeWidth = 2f
        }
        canvas.drawLine(20f, y, width - 20f, y, linePaint)
        y += 20f
        
        val leftBgPaint = Paint().apply {
            color = Color.parseColor("#F4F0E4")
            style = Paint.Style.FILL
        }
        val leftRect = android.graphics.RectF(25f, y, 25f + colWidth, y + gridHeight)
        canvas.drawRoundRect(leftRect, 10f, 10f, leftBgPaint)
        
        val colHeaderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#8B6B3F")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("▼ MEASUREMENTS", 35f, y + 25f, colHeaderPaint)
        canvas.drawLine(35f, y + 35f, 25f + colWidth - 10f, y + 35f, linePaint)
        
        val valMap = values.associateBy { it.fieldId }
        var leftY = y + 55f
        if (mFields.isEmpty()) {
            canvas.drawText("None added", 35f, leftY, Paint().apply { isAntiAlias = true; color = Color.GRAY; textSize = 12f })
        } else {
            mFields.forEach { f ->
                val v = valMap[f.id]
                val valText = if (v != null) {
                    if (v.value.isNotBlank() && v.subValue.isNotBlank()) "${v.value} (${v.subValue})"
                    else v.value.ifBlank { v.subValue }.ifBlank { "—" }
                } else "—"
                canvas.drawText(f.label, 35f, leftY, Paint().apply { isAntiAlias = true; color = Color.parseColor("#3D3A37"); textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
                val labelWidth = Paint().apply { textSize = 12f }.measureText(f.label)
                val valueWidth = Paint().apply { textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }.measureText(valText)
                val dotStart = 35f + labelWidth + 5f
                val dotEnd = 25f + colWidth - 15f - valueWidth - 5f
                if (dotEnd > dotStart) {
                    val dotText = ".".repeat(((dotEnd - dotStart) / 4).toInt().coerceAtLeast(1))
                    canvas.drawText(dotText, dotStart, leftY, Paint().apply { isAntiAlias = true; color = Color.parseColor("#C7BEB0"); textSize = 11f })
                }
                canvas.drawText(valText, 25f + colWidth - 15f, leftY, Paint().apply { isAntiAlias = true; color = Color.parseColor("#8B6B3F"); textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.RIGHT })
                leftY += 32f
            }
        }
        
        val rightBgPaint = Paint().apply {
            color = Color.parseColor("#EEEAE0")
            style = Paint.Style.FILL
        }
        val rightRect = android.graphics.RectF(width - 25f - colWidth, y, width - 25f, y + gridHeight)
        canvas.drawRoundRect(rightRect, 10f, 10f, rightBgPaint)
        
        canvas.drawText("✕ STYLE DEMANDS", width - colWidth - 15f, y + 25f, Paint(colHeaderPaint).apply { color = Color.parseColor("#715F47") })
        canvas.drawLine(width - colWidth - 15f, y + 35f, width - 35f, y + 35f, linePaint)
        
        var rightY = y + 55f
        if (dFields.isEmpty()) {
            canvas.drawText("None added", width - colWidth - 15f, rightY, Paint().apply { isAntiAlias = true; color = Color.GRAY; textSize = 12f })
        } else {
            dFields.forEach { f ->
                val v = valMap[f.id]
                val valText = if (v != null) {
                    if (v.value.isNotBlank() && v.subValue.isNotBlank()) "${v.value} (${v.subValue})"
                    else v.value.ifBlank { v.subValue }.ifBlank { "—" }
                } else "—"
                canvas.drawText(f.label, width - colWidth - 15f, rightY, Paint().apply { isAntiAlias = true; color = Color.parseColor("#3D3A37"); textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
                val labelWidth = Paint().apply { textSize = 12f }.measureText(f.label)
                val valueWidth = Paint().apply { textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }.measureText(valText)
                val dotStart = width - colWidth - 15f + labelWidth + 5f
                val dotEnd = width - 35f - valueWidth - 5f
                if (dotEnd > dotStart) {
                    val dotText = ".".repeat(((dotEnd - dotStart) / 4).toInt().coerceAtLeast(1))
                    canvas.drawText(dotText, dotStart, rightY, Paint().apply { isAntiAlias = true; color = Color.parseColor("#C7BEB0"); textSize = 11f })
                }
                canvas.drawText(valText, width - 35f, rightY, Paint().apply { isAntiAlias = true; color = Color.parseColor("#715F47"); textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.RIGHT })
                rightY += 32f
            }
        }
        
        y += gridHeight + 10f
        
        if (globalNote.isNotBlank()) {
            val noteRect = android.graphics.RectF(25f, y, width - 25f, y + 75f)
            val noteBgPaint = Paint().apply {
                color = Color.parseColor("#FCFAF2")
                style = Paint.Style.FILL
            }
            val noteBorderPaint = Paint().apply {
                color = Color.parseColor("#E5DECE")
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
            }
            canvas.drawRoundRect(noteRect, 8f, 8f, noteBgPaint)
            canvas.drawRoundRect(noteRect, 8f, 8f, noteBorderPaint)
            
            val noteTitlePaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#8B6B3F")
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("ADDITIONAL NOTES:", 35f, y + 22f, noteTitlePaint)
            
            val noteContentPaint = Paint().apply {
                isAntiAlias = true
                color = Color.parseColor("#3D3A37")
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            
            val maxCharLine = 75
            if (globalNote.length > maxCharLine) {
                val firstLine = globalNote.substring(0, maxCharLine)
                val secondLine = if (globalNote.length > maxCharLine * 2) {
                    globalNote.substring(maxCharLine, maxCharLine * 2) + "..."
                } else {
                    globalNote.substring(maxCharLine)
                }
                canvas.drawText(firstLine, 35f, y + 42f, noteContentPaint)
                canvas.drawText(secondLine, 35f, y + 58f, noteContentPaint)
            } else {
                canvas.drawText(globalNote, 35f, y + 45f, noteContentPaint)
            }
            y += 85f
        } else {
            y += 5f
        }
        
        canvas.drawLine(20f, y, width - 20f, y, linePaint)
        y += 20f
        
        val footerLabelPaint = Paint().apply {
            isAntiAlias = true; color = Color.parseColor("#5E584E"); textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val footerTextPaint = Paint().apply {
            isAntiAlias = true; color = Color.parseColor("#726B5F"); textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        
        canvas.drawText("TICKET POLICY & DISCLAIMER", 25f, y, footerLabelPaint)
        canvas.drawText("These master specs are registered under tailor custody.", 25f, y + 15f, footerTextPaint)
        canvas.drawText("For subsequent altering, refer to the original copy.", 25f, y + 28f, footerTextPaint)
        
        val sigX = width - 120f
        canvas.drawLine(sigX - 40f, y + 20f, sigX + 60f, y + 20f, Paint().apply { color = Color.parseColor("#726B5F"); strokeWidth = 1.5f })
        canvas.drawText("Master Signature", sigX + 10f, y + 34f, Paint(footerLabelPaint).apply { color = Color.parseColor("#726B5F"); textAlign = Paint.Align.CENTER })
        
        return bitmap
    }

    fun generateLedgerReceiptBitmap(
        context: Context,
        shopName: String,
        shopPhone: String,
        shopAddress: String,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        filteredOrders: List<OrderRecord>,
        filteredPayments: List<PaymentRecord>,
        ledger: LedgerRecord?,
        filterType: String,
        customStartDate: Long?,
        customEndDate: Long?
    ): Bitmap {
        val rowCount = filteredOrders.size + filteredPayments.size
        val tableHeight = 40f + (rowCount * 30f) + 10f
        val periodSummaryHeight = 70f
        
        val headerHeight = 180f
        val metaHeight = 110f
        val footerHeight = 100f
        
        val width = 800
        val height = (headerHeight + metaHeight + tableHeight + periodSummaryHeight + footerHeight).toInt().coerceAtLeast(550)
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        canvas.drawColor(Color.WHITE)
        
        val borderPaint = Paint().apply {
            color = Color.parseColor("#E5E7EB")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRect(8f, 8f, width - 8f, height - 8f, borderPaint)
        
        var y = 40f
        
        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#1F2937")
            textSize = 21f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(shopName.ifEmpty { "Tailor Atelier" }.uppercase(Locale.getDefault()), 25f, y, titlePaint)
        
        val subTitlePaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#D97706")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("PREMIUM CUSTOM TAILOR SHOP", 25f, y + 16f, subTitlePaint)
        
        val shopMetaPaint = Paint().apply {
            isAntiAlias = true
            color = Color.GRAY
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(shopAddress.ifEmpty { "Main Atelier District" }, 25f, y + 36f, shopMetaPaint)
        canvas.drawText("Phone: ${shopPhone.ifEmpty { "+00 000 000" }}", 25f, y + 52f, shopMetaPaint)
        
        canvas.drawText("No: TX-${System.currentTimeMillis().toString().takeLast(6)}", width - 150f, y, Paint(shopMetaPaint).apply { textSize = 11f; typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL) })
        
        y += 75f
        
        val ribbonPaint = Paint().apply {
            color = Color.parseColor("#D97706")
            style = Paint.Style.FILL
        }
        canvas.drawRect(20f, y, width - 20f, y + 6f, ribbonPaint)
        y += 25f
        
        val metaLabelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#4B5563")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metaTextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#111827")
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val metaSubtextPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#374151")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        
        canvas.drawText("BILLED TO:", 25f, y, metaLabelPaint)
        canvas.drawText(customerName.uppercase(Locale.getDefault()), 25f, y + 20f, metaTextPaint)
        canvas.drawText("Phone: $customerPhone", 25f, y + 36f, metaSubtextPaint)
        if (customerAddress.isNotEmpty()) {
            canvas.drawText("Address: $customerAddress", 25f, y + 52f, metaSubtextPaint)
        }
        
        val rightColX = width - 250f
        canvas.drawText("STATEMENT DATE:", rightColX, y, metaLabelPaint)
        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        canvas.drawText(dateStr, rightColX, y + 20f, Paint(metaSubtextPaint).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
        
        val filterStr = if (filterType == "Custom") {
            val startStr = customStartDate?.let { SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(it)) } ?: "Beginning"
            val endStr = customEndDate?.let { SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(it)) } ?: "End"
            "Period: $startStr - $endStr"
        } else {
            "Filter: $filterType"
        }
        canvas.drawText(filterStr, rightColX, y + 36f, Paint(metaSubtextPaint).apply { textSize = 11f; color = Color.GRAY })
        
        val pendingDue = ledger?.pendingDebt ?: 0.0
        val isDebt = pendingDue > 0.0
        val statusText = if (isDebt) "DEBT OUTSTANDING" else "ALL PAID"
        val statusBg = if (isDebt) Color.parseColor("#FEEBEE") else Color.parseColor("#E8F5E9")
        val statusColor = if (isDebt) Color.parseColor("#C62828") else Color.parseColor("#2E7D32")
        
        val stampBackgroundPaint = Paint().apply {
            color = statusBg
            style = Paint.Style.FILL
        }
        val stampRect = android.graphics.RectF(rightColX, y + 45f, rightColX + 220f, y + 68f)
        canvas.drawRoundRect(stampRect, 6f, 6f, stampBackgroundPaint)
        canvas.drawRoundRect(stampRect, 6f, 6f, Paint().apply { color = statusColor; style = Paint.Style.STROKE; strokeWidth = 1f })
        
        canvas.drawText(
            statusText, 
            rightColX + 110f, 
            y + 61f, 
            Paint().apply { 
                isAntiAlias = true; color = statusColor; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER 
            }
        )
        
        y += 95f
        
        val headerBgPaint = Paint().apply {
            color = Color.parseColor("#F3F4F6")
            style = Paint.Style.FILL
        }
        canvas.drawRect(20f, y, width - 20f, y + 26f, headerBgPaint)
        
        val gridHeaderPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#374151")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("DESCRIPTION", 30f, y + 17f, gridHeaderPaint)
        canvas.drawText("QTY", 400f, y + 17f, Paint(gridHeaderPaint).apply { textAlign = Paint.Align.CENTER })
        canvas.drawText("PRICE", 560f, y + 17f, Paint(gridHeaderPaint).apply { textAlign = Paint.Align.RIGHT })
        canvas.drawText("TOTAL", width - 30f, y + 17f, Paint(gridHeaderPaint).apply { textAlign = Paint.Align.RIGHT })
        
        y += 28f
        
        val gridItemPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#1F2937")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        
        if (filteredOrders.isEmpty() && filteredPayments.isEmpty()) {
            canvas.drawText("No ledger records in selected range.", width / 2f, y + 25f, Paint(gridItemPaint).apply { textAlign = Paint.Align.CENTER; color = Color.GRAY })
            y += 40f
        } else {
            filteredOrders.forEach { ord ->
                canvas.drawText(ord.itemName, 30f, y + 18f, gridItemPaint)
                canvas.drawText(ord.quantity.toString(), 400f, y + 18f, Paint(gridItemPaint).apply { textAlign = Paint.Align.CENTER })
                canvas.drawText(String.format(Locale.getDefault(), "%.2f", ord.price), 560f, y + 18f, Paint(gridItemPaint).apply { textAlign = Paint.Align.RIGHT })
                canvas.drawText(String.format(Locale.getDefault(), "%.2f", ord.price * ord.quantity), width - 30f, y + 18f, Paint(gridItemPaint).apply { textAlign = Paint.Align.RIGHT; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
                y += 28f
            }
            
            filteredPayments.forEach { pay ->
                val memo = if (pay.note.isNotEmpty()) "Credit: ${pay.note}" else "Payment Credit"
                val payPaint = Paint(gridItemPaint).apply { color = Color.parseColor("#15803D") }
                canvas.drawText(memo, 30f, y + 18f, payPaint)
                canvas.drawText("1", 400f, y + 18f, Paint(payPaint).apply { textAlign = Paint.Align.CENTER })
                canvas.drawText(String.format(Locale.getDefault(), "-%.2f", pay.amountPaid), 560f, y + 18f, Paint(payPaint).apply { textAlign = Paint.Align.RIGHT })
                canvas.drawText(String.format(Locale.getDefault(), "-%.2f", pay.amountPaid), width - 30f, y + 18f, Paint(payPaint).apply { textAlign = Paint.Align.RIGHT; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
                y += 28f
            }
        }
        
        canvas.drawLine(20f, y, width - 20f, y, Paint().apply { color = Color.parseColor("#E5E7EB"); strokeWidth = 1.5f })
        y += 18f
        
        val totalOrdersSelected = filteredOrders.sumOf { it.price * it.quantity }
        val totalPaidSelected = filteredPayments.sumOf { it.amountPaid }
        val netSelected = totalOrdersSelected - totalPaidSelected
        
        val summaryLabelPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#4B5563")
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        
        canvas.drawText("Selected Period Charges Total:", 30f, y, summaryLabelPaint)
        canvas.drawText(String.format(Locale.getDefault(), "%.2f", totalOrdersSelected), width - 30f, y, Paint(summaryLabelPaint).apply { textAlign = Paint.Align.RIGHT })
        y += 20f
        
        canvas.drawText("Selected Period Credits Applied:", 30f, y, Paint(summaryLabelPaint).apply { color = Color.parseColor("#15803D") })
        canvas.drawText(String.format(Locale.getDefault(), "-%.2f", totalPaidSelected), width - 30f, y, Paint(summaryLabelPaint).apply { color = Color.parseColor("#15803D"); textAlign = Paint.Align.RIGHT })
        y += 20f
        
        canvas.drawText("Selected Period Net Balance due:", 30f, y, Paint(summaryLabelPaint).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.BLACK })
        canvas.drawText(String.format(Locale.getDefault(), "%.2f", netSelected), width - 30f, y, Paint(summaryLabelPaint).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); color = Color.BLACK; textAlign = Paint.Align.RIGHT })
        
        y += 35f
        
        val footerBgPaint = Paint().apply {
            color = Color.parseColor("#FEF3C7")
            style = Paint.Style.FILL
        }
        val footerRect = android.graphics.RectF(20f, y, width - 20f, y + 54f)
        canvas.drawRoundRect(footerRect, 8f, 8f, footerBgPaint)
        
        val tbl1 = Paint().apply { isAntiAlias = true; color = Color.parseColor("#92400E"); textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val tbl2 = Paint().apply { isAntiAlias = true; color = Color.parseColor("#B45309"); textSize = 9f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
        canvas.drawText("LEDGER SUMMARY TERMS", 35f, y + 20f, tbl1)
        canvas.drawText("Official transaction balance record ledger statement.", 35f, y + 36f, tbl2)
        
        canvas.drawText("TOTAL DUE", width - 35f, y + 18f, Paint(tbl1).apply { textAlign = Paint.Align.RIGHT })
        val globalDue = ledger?.pendingDebt ?: 0.0
        canvas.drawText(String.format(Locale.getDefault(), "%.2f", globalDue), width - 35f, y + 42f, Paint(tbl1).apply { textSize = 19f; textAlign = Paint.Align.RIGHT })
        
        return bitmap
    }

    fun saveAndShareBespoke(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        customerName: String,
        customerPhone: String,
        shopName: String,
        isWhatsAppDirect: Boolean
    ) {
        val imagesFolder = File(context.cacheDir, "receipts")
        if (!imagesFolder.exists()) {
            imagesFolder.mkdirs()
        }
        val file = File(imagesFolder, fileName)
        try {
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            val contentUri = FileProvider.getUriForFile(context, getSharedFileProviderAuthority(context), file)
            if (contentUri != null) {
                if (isWhatsAppDirect && customerPhone.isNotBlank()) {
                    val cleanPhone = customerPhone.replace(Regex("[^0-9]"), "")
                    val prefilledText = "Hello $customerName, here is your bespoke invoice/statement from $shopName."
                    
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        putExtra(Intent.EXTRA_TEXT, prefilledText)
                        type = "image/png"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra("jid", "$cleanPhone@s.whatsapp.net")
                        `package` = "com.whatsapp"
                    }
                    try {
                        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(shareIntent)
                    } catch (e: Exception) {
                        val fallbackIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, contentUri)
                            putExtra(Intent.EXTRA_TEXT, prefilledText)
                            type = "image/png"
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooser = Intent.createChooser(fallbackIntent, "Share Statement")
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(chooser)
                    }
                } else {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        type = "image/png"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooserIntent = Intent.createChooser(shareIntent, "Share Bespoke Receipt")
                    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooserIntent)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareBespokeMeasurementReceipt(
        context: Context,
        shopName: String,
        shopPhone: String,
        shopAddress: String,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        sheetTitle: String,
        templateLabel: String,
        fields: List<MeasurementField>,
        values: List<MeasurementValue>,
        lastEditedDate: Long
    ) {
        val bitmap = generateMeasurementReceiptBitmap(
            context, shopName, shopPhone, shopAddress,
            customerName, customerPhone, customerAddress,
            sheetTitle, templateLabel, fields, values, lastEditedDate
        )
        saveAndShareBespoke(context, bitmap, "measurement_card_${customerName.replace(" ", "_").lowercase()}.png", customerName, customerPhone, shopName, false)
    }

    fun shareBespokeLedgerReceipt(
        context: Context,
        shopName: String,
        shopPhone: String,
        shopAddress: String,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        filteredOrders: List<OrderRecord>,
        filteredPayments: List<PaymentRecord>,
        ledger: LedgerRecord?,
        filterType: String,
        customStartDate: Long?,
        customEndDate: Long?,
        isWhatsAppDirect: Boolean
    ) {
        val bitmap = generateLedgerReceiptBitmap(
            context, shopName, shopPhone, shopAddress,
            customerName, customerPhone, customerAddress,
            filteredOrders, filteredPayments, ledger,
            filterType, customStartDate, customEndDate
        )
        saveAndShareBespoke(context, bitmap, "ledger_statement_${customerName.replace(" ", "_").lowercase()}.png", customerName, customerPhone, shopName, isWhatsAppDirect)
    }

    private fun getSharedFileProviderAuthority(context: Context): String {
        return "${context.packageName}.fileprovider"
    }

    fun shareMeasurementReceipt(
        context: Context,
        customer: Customer,
        fields: List<MeasurementField>,
        values: List<MeasurementValue>
    ) {
        val width = 600
        val height = 250 + (fields.size * 35) + 120
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw thermal ticket background
        canvas.drawColor(Color.parseColor("#FFFDF9")) // Off-white warm paper
        
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        val centerPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 28f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val borderPaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        // Draw outer borders
        canvas.drawRect(10f, 10f, width - 10f, height - 10f, borderPaint)
        
        // Draw decorative zig-zag / ticket dotted lines at top & bottom
        var x = 20f
        val dottedPaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 3f
            style = Paint.Style.STROKE
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }
        canvas.drawLine(20f, 50f, width - 20f, 50f, dottedPaint)

        // Header Title
        canvas.drawText("TAILOR SHOP MASTER", width / 2f, 90f, centerPaint)
        
        paint.textSize = 20f
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        canvas.drawText("DATE: ${dateFormat.format(Date())}", 30f, 130f, paint)
        canvas.drawText("----------------------------------------", 30f, 155f, paint)
        
        // Customer Info
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas.drawText("CUSTOMER: ${customer.name}", 30f, 185f, paint)
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        canvas.drawText("PHONE   : ${customer.phone}", 30f, 215f, paint)
        canvas.drawText("ADDRESS : ${customer.address}", 30f, 245f, paint)
        canvas.drawText("----------------------------------------", 30f, 270f, paint)

        // Title Section
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas.drawText("MEASUREMENTS (SIZES)", 30f, 300f, paint)
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        canvas.drawText("----------------------------------------", 30f, 320f, paint)

        // Custom Size Fields
        var y = 350f
        val valMap = values.associateBy { it.fieldId }
        for (field in fields) {
            val entry = valMap[field.id]
            val valueText = entry?.value ?: "Not Set"
            canvas.drawText("${field.label}:", 40f, y, paint)
            
            val valPaint = Paint(paint).apply {
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                color = Color.parseColor("#1B5E20") // Rich Green accent for measurements
            }
            canvas.drawText(valueText, 350f, y, valPaint)
            y += 35f
        }

        canvas.drawText("----------------------------------------", 30f, y, paint)
        y += 35f
        canvas.drawText("THANK YOU FOR YOUR CHOICE!", width / 2f, y, centerPaint)
        y += 35f
        canvas.drawText("Powered by Tailor Workspace API", width / 2f, y, Paint(centerPaint).apply { textSize = 16f; color = Color.GRAY })

        saveAndShare(context, bitmap, "measurement_card_${customer.name}.png")
    }

    fun shareLedgerReceipt(
        context: Context,
        customer: Customer,
        ledger: LedgerRecord
    ) {
        val width = 600
        val height = 550
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw warm off-white ticket background
        canvas.drawColor(Color.parseColor("#FFFDF9"))
        
        val paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 24f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        val centerPaint = Paint().apply {
            isAntiAlias = true
            color = Color.BLACK
            textSize = 28f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val borderPaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        // Outer Border
        canvas.drawRect(10f, 10f, width - 10f, height - 10f, borderPaint)

        // Dotted divider
        val dottedPaint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 3f
            style = Paint.Style.STROKE
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 10f), 0f)
        }
        canvas.drawLine(20f, 50f, width - 20f, 50f, dottedPaint)

        // Title
        canvas.drawText("TAILOR LEDGER & BILLING", width / 2f, 90f, centerPaint)

        paint.textSize = 20f
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        canvas.drawText("DATE: ${dateFormat.format(Date())}", 30f, 130f, paint)
        canvas.drawText("----------------------------------------", 30f, 155f, paint)

        // Customer
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        canvas.drawText("CUSTOMER: ${customer.name}", 30f, 195f, paint)
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        canvas.drawText("PHONE   : ${customer.phone}", 30f, 225f, paint)
        canvas.drawText("----------------------------------------", 30f, 255f, paint)

        // Invoice Bill Breakdown
        canvas.drawText("BILL PARTICULARS", 30f, 290f, Paint(paint).apply { typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) })
        canvas.drawText("----------------------------------------", 30f, 310f, paint)

        canvas.drawText("TOTAL BILL VALUE: ", 30f, 345f, paint)
        canvas.drawText("${String.format(Locale.getDefault(), "%.2f", ledger.totalValue)}", 350f, 345f, Paint(paint).apply { typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD) })

        canvas.drawText("TOTAL PAID AMOUNT:", 30f, 380f, paint)
        canvas.drawText("${String.format(Locale.getDefault(), "%.2f", ledger.amountPaid)}", 350f, 380f, Paint(paint).apply { typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD); color = Color.parseColor("#2E7D32") })

        canvas.drawText("----------------------------------------", 30f, 410f, paint)

        canvas.drawText("PENDING DEBT BALANCE:", 30f, 445f, Paint(paint).apply { color = Color.parseColor("#B71C1C") })
        canvas.drawText(
            "${String.format(Locale.getDefault(), "%.2f", ledger.pendingDebt)}", 
            350f, 445f, 
            Paint(paint).apply { 
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                color = Color.parseColor("#B71C1C")
            }
        )

        canvas.drawText("----------------------------------------", 30f, 475f, paint)

        // Footer status stamp
        val stampText = if (ledger.pendingDebt <= 0.0) "PAID IN FULL" else "OUTSTANDING DEBT"
        val stampBackground = if (ledger.pendingDebt <= 0.0) Color.parseColor("#E8F5E9") else Color.parseColor("#FFEBEE")
        val stampColor = if (ledger.pendingDebt <= 0.0) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")

        val stampPaint = Paint().apply {
            color = stampColor
            textSize = 18f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        
        // Draw a neat filled rectangle for the status stamp
        val stampBoxPaint = Paint().apply {
            color = stampBackground
            style = Paint.Style.FILL
        }
        canvas.drawRect(width / 2f - 150f, 490f, width / 2f + 150f, 530f, stampBoxPaint)
        canvas.drawRect(width / 2f - 150f, 490f, width / 2f + 150f, 530f, Paint().apply { color = stampColor; style = Paint.Style.STROKE; strokeWidth = 2f })
        canvas.drawText(stampText, width / 2f, 518f, stampPaint)

        saveAndShare(context, bitmap, "ledger_receipt_${customer.name}.png")
    }

    fun shareStatementImage(
        context: Context,
        receiptText: String,
        customerName: String,
        customerPhone: String,
        shopName: String,
        isWhatsAppDirect: Boolean
    ) {
        val lines = receiptText.split("\n")
        
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#1F2937") // Charcoal dark text
            textSize = 20f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }
        
        var maxLineWidth = 0f
        for (line in lines) {
            val w = textPaint.measureText(line)
            if (w > maxLineWidth) {
                maxLineWidth = w
            }
        }
        
        val paddingLeftRight = 45f
        val paddingTopBottom = 50f
        val lineHeight = 30f
        
        val width = (maxLineWidth + paddingLeftRight * 2).toInt().coerceAtLeast(600)
        val height = (lines.size * lineHeight + paddingTopBottom * 2).toInt()
        
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Render beautiful thermal paper background
        canvas.drawColor(Color.parseColor("#FFFDF9"))
        
        val borderPaint = Paint().apply {
            color = Color.parseColor("#374151")
            strokeWidth = 3f
            style = Paint.Style.STROKE
        }
        canvas.drawRect(15f, 15f, width - 15f, height - 15f, borderPaint)
        
        val dottedPaint = Paint().apply {
            color = Color.parseColor("#9CA3AF")
            strokeWidth = 2f
            style = Paint.Style.STROKE
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(5f, 10f), 0f)
        }
        canvas.drawLine(25f, 40f, width - 25f, 40f, dottedPaint)
        
        var y = paddingTopBottom + 15f
        for (line in lines) {
            canvas.drawText(line, paddingLeftRight, y, textPaint)
            y += lineHeight
        }
        
        val imagesFolder = File(context.cacheDir, "receipts")
        if (!imagesFolder.exists()) {
            imagesFolder.mkdirs()
        }
        
        val fileName = "invoice_${customerName.replace(" ", "_").lowercase()}_${System.currentTimeMillis()}.png"
        val file = File(imagesFolder, fileName)
        
        try {
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
            
            val contentUri = FileProvider.getUriForFile(context, getSharedFileProviderAuthority(context), file)
            if (contentUri != null) {
                if (isWhatsAppDirect && customerPhone.isNotBlank()) {
                    val cleanPhone = customerPhone.replace(Regex("[^0-9]"), "")
                    val prefilledText = "Hello $customerName, here is your invoice / ledger statement of account from $shopName."
                    
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        putExtra(Intent.EXTRA_TEXT, prefilledText)
                        type = "image/png"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        putExtra("jid", "$cleanPhone@s.whatsapp.net")
                        `package` = "com.whatsapp"
                    }
                    try {
                        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(shareIntent)
                    } catch (e: Exception) {
                        val fallbackIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_STREAM, contentUri)
                            putExtra(Intent.EXTRA_TEXT, prefilledText)
                            type = "image/png"
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        val chooser = Intent.createChooser(fallbackIntent, "Share Ledger Statement")
                        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(chooser)
                    }
                } else {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        type = "image/png"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val chooser = Intent.createChooser(shareIntent, "Share Invoice Image")
                    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(chooser)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveAndShare(context: Context, bitmap: Bitmap, fileName: String) {
        val imagesFolder = File(context.cacheDir, "receipts")
        imagesFolder.mkdirs()
        val file = File(imagesFolder, fileName)
        try {
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()

            val contentUri = FileProvider.getUriForFile(context, getSharedFileProviderAuthority(context), file)
            if (contentUri != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    type = "image/png"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooserIntent = Intent.createChooser(shareIntent, "Share Receipt")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooserIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
