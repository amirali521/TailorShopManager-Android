package com.example.util

import com.example.data.model.*
import java.io.BufferedReader
import java.io.StringReader
import java.util.UUID

object CsvHelper {

    // Helper to escape commas or quotes in CSV cells
    private fun escapeCsv(value: String): String {
        val cleanValue = value.replace("\"", "\"\"")
        return if (cleanValue.contains(",") || cleanValue.contains("\n") || cleanValue.contains("\"")) {
            "\"$cleanValue\""
        } else {
            cleanValue
        }
    }

    // Helper to parse a standard CSV line considering quotes and escaped commas
    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var currentToken = StringBuilder()
        var insideQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (insideQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    // Escaped quote: "" -> "
                    currentToken.append('"')
                    i++
                } else {
                    insideQuotes = !insideQuotes
                }
            } else if (c == ',' && !insideQuotes) {
                result.add(currentToken.toString().trim())
                currentToken = StringBuilder()
            } else {
                currentToken.append(c)
            }
            i++
        }
        result.add(currentToken.toString().trim())
        return result
    }

    fun exportCustomersToCsv(customers: List<Customer>): String {
        val writer = StringBuilder()
        writer.append("CustomerId,Name,Phone,Address,LastUpdated\n")
        for (c in customers) {
            writer.append("${escapeCsv(c.id)},")
                .append("${escapeCsv(c.name)},")
                .append("${escapeCsv(c.phone)},")
                .append("${escapeCsv(c.address)},")
                .append("${c.updatedAt}\n")
        }
        return writer.toString()
    }

    fun parseCustomersFromCsv(csvContent: String, userId: String): List<Customer> {
        val customers = mutableListOf<Customer>()
        val reader = BufferedReader(StringReader(csvContent))
        var line: String? = reader.readLine() // Skip Header
        while (reader.readLine().also { line = it } != null) {
            val tokens = parseCsvLine(line!!)
            if (tokens.size >= 4) {
                val csvId = tokens[0].ifBlank { UUID.randomUUID().toString() }
                val name = tokens[1]
                val phone = tokens[2]
                val address = tokens[3]
                customers.add(
                    Customer(
                        id = csvId,
                        userId = userId,
                        name = name,
                        phone = phone,
                        address = address,
                        updatedAt = System.currentTimeMillis(),
                        isSynced = false,
                        isDeleted = false
                    )
                )
            }
        }
        return customers
    }

    fun exportInventoryToCsv(items: List<InventoryItem>): String {
        val writer = StringBuilder()
        writer.append("ItemId,Name,UnitType,PurchasePrice,SellingPrice,StockQuantity\n")
        for (item in items) {
            writer.append("${escapeCsv(item.id)},")
                .append("${escapeCsv(item.name)},")
                .append("${escapeCsv(item.unitType)},")
                .append("${item.purchasePrice},")
                .append("${item.sellingPrice},")
                .append("${item.stockQuantity}\n")
        }
        return writer.toString()
    }

    fun parseInventoryFromCsv(csvContent: String, userId: String): List<InventoryItem> {
        val items = mutableListOf<InventoryItem>()
        val reader = BufferedReader(StringReader(csvContent))
        var line: String? = reader.readLine() // Skip Header
        while (reader.readLine().also { line = it } != null) {
            val tokens = parseCsvLine(line!!)
            if (tokens.size >= 6) {
                val csvId = tokens[0].ifBlank { UUID.randomUUID().toString() }
                val name = tokens[1]
                val unitType = tokens[2]
                val purchase = tokens[3].toDoubleOrNull() ?: 0.0
                val selling = tokens[4].toDoubleOrNull() ?: 0.0
                val stock = tokens[5].toIntOrNull() ?: 0
                items.add(
                    InventoryItem(
                        id = csvId,
                        userId = userId,
                        name = name,
                        unitType = unitType,
                        purchasePrice = purchase,
                        sellingPrice = selling,
                        stockQuantity = stock,
                        updatedAt = System.currentTimeMillis(),
                        isSynced = false,
                        isDeleted = false
                    )
                )
            }
        }
        return items
    }

    fun exportLedgerToCsv(records: List<LedgerRecord>): String {
        val writer = StringBuilder()
        writer.append("LedgerId,CustomerId,TotalBill,AmountPaid,PendingDebt\n")
        for (r in records) {
            writer.append("${escapeCsv(r.id)},")
                .append("${escapeCsv(r.customerId)},")
                .append("${r.totalValue},")
                .append("${r.amountPaid},")
                .append("${r.pendingDebt}\n")
        }
        return writer.toString()
    }

    fun parseLedgerFromCsv(csvContent: String, userId: String): List<LedgerRecord> {
        val records = mutableListOf<LedgerRecord>()
        val reader = BufferedReader(StringReader(csvContent))
        var line: String? = reader.readLine() // Skip Header
        while (reader.readLine().also { line = it } != null) {
            val tokens = parseCsvLine(line!!)
            if (tokens.size >= 5) {
                val csvId = tokens[0].ifBlank { UUID.randomUUID().toString() }
                val customerId = tokens[1]
                val total = tokens[2].toDoubleOrNull() ?: 0.0
                val paid = tokens[3].toDoubleOrNull() ?: 0.0
                val debt = tokens[4].toDoubleOrNull() ?: (total - paid)
                records.add(
                    LedgerRecord(
                        id = csvId,
                        userId = userId,
                        customerId = customerId,
                        totalValue = total,
                        amountPaid = paid,
                        pendingDebt = debt,
                        updatedAt = System.currentTimeMillis(),
                        isSynced = false,
                        isDeleted = false
                    )
                )
            }
        }
        return records
    }

    fun exportMeasurementsToCsv(values: List<MeasurementValue>, fieldsMap: Map<String, String>): String {
        val writer = StringBuilder()
        writer.append("RecordId,CustomerId,FieldId,FieldName,Value\n")
        for (v in values) {
            val fieldName = fieldsMap[v.fieldId] ?: "Unknown Field"
            writer.append("${escapeCsv(v.id)},")
                .append("${escapeCsv(v.customerId)},")
                .append("${escapeCsv(v.fieldId)},")
                .append("${escapeCsv(fieldName)},")
                .append("${escapeCsv(v.value)}\n")
        }
        return writer.toString()
    }
}
