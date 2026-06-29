package com.example.util

import android.content.Context
import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject

object JsonBackupHelper {
    // Export all components including shop info, customers, templates, fields, measurements, ledger, inventory
    fun exportEverything(
        shopName: String,
        shopPhone: String,
        shopAddress: String,
        customers: List<Customer>,
        templates: List<SizingTemplate>,
        fields: List<MeasurementField>,
        customerMeasurements: List<CustomerMeasurement>,
        measurementValues: List<MeasurementValue>,
        inventoryItems: List<InventoryItem>,
        ledgerRecords: List<LedgerRecord>,
        paymentRecords: List<PaymentRecord>,
        orderRecords: List<OrderRecord>
    ): String {
        try {
            val root = JSONObject()

            // Shop setup info
            val shopObj = JSONObject()
            shopObj.put("name", shopName)
            shopObj.put("phone", shopPhone)
            shopObj.put("address", shopAddress)
            root.put("shop_profile", shopObj)

            // Customers
            val custArr = JSONArray()
            for (c in customers) {
                val obj = JSONObject()
                obj.put("id", c.id)
                obj.put("name", c.name)
                obj.put("phone", c.phone)
                obj.put("address", c.address)
                obj.put("updatedAt", c.updatedAt)
                custArr.put(obj)
            }
            root.put("customers", custArr)

            // Templates
            val tempArr = JSONArray()
            for (t in templates) {
                val obj = JSONObject()
                obj.put("id", t.id)
                obj.put("name", t.name)
                obj.put("updatedAt", t.updatedAt)
                tempArr.put(obj)
            }
            root.put("sizing_templates", tempArr)

            // Fields
            val fieldArr = JSONArray()
            for (f in fields) {
                val obj = JSONObject()
                obj.put("id", f.id)
                obj.put("templateId", f.templateId)
                obj.put("label", f.label)
                obj.put("type", f.type)
                obj.put("options", f.options)
                obj.put("displayOrder", f.displayOrder)
                obj.put("updatedAt", f.updatedAt)
                fieldArr.put(obj)
            }
            root.put("measurement_fields", fieldArr)

            // Customer Measurements
            val custMeasArr = JSONArray()
            for (cm in customerMeasurements) {
                val obj = JSONObject()
                obj.put("id", cm.id)
                obj.put("customerId", cm.customerId)
                obj.put("templateId", cm.templateId)
                obj.put("title", cm.title)
                obj.put("date", cm.date)
                obj.put("updatedAt", cm.updatedAt)
                custMeasArr.put(obj)
            }
            root.put("customer_measurements", custMeasArr)

            // Measurement Values
            val valArr = JSONArray()
            for (v in measurementValues) {
                val obj = JSONObject()
                obj.put("id", v.id)
                obj.put("customerId", v.customerId)
                obj.put("measurementId", v.measurementId)
                obj.put("fieldId", v.fieldId)
                obj.put("value", v.value)
                obj.put("updatedAt", v.updatedAt)
                valArr.put(obj)
            }
            root.put("measurement_values", valArr)

            // Inventory Items
            val invArr = JSONArray()
            for (item in inventoryItems) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("name", item.name)
                obj.put("unitType", item.unitType)
                obj.put("purchasePrice", item.purchasePrice)
                obj.put("sellingPrice", item.sellingPrice)
                obj.put("stockQuantity", item.stockQuantity)
                obj.put("updatedAt", item.updatedAt)
                invArr.put(obj)
            }
            root.put("inventory_items", invArr)

            // Ledger Records
            val ledgerArr = JSONArray()
            for (l in ledgerRecords) {
                val obj = JSONObject()
                obj.put("id", l.id)
                obj.put("customerId", l.customerId)
                obj.put("totalValue", l.totalValue)
                obj.put("amountPaid", l.amountPaid)
                obj.put("pendingDebt", l.pendingDebt)
                obj.put("updatedAt", l.updatedAt)
                ledgerArr.put(obj)
            }
            root.put("ledger_records", ledgerArr)

            // Payment Records
            val payArr = JSONArray()
            for (p in paymentRecords) {
                val obj = JSONObject()
                obj.put("id", p.id)
                obj.put("customerId", p.customerId)
                obj.put("ledgerId", p.ledgerId)
                obj.put("amountPaid", p.amountPaid)
                obj.put("paymentDate", p.paymentDate)
                obj.put("note", p.note)
                obj.put("updatedAt", p.updatedAt)
                payArr.put(obj)
            }
            root.put("payment_records", payArr)

            // Order Records
            val orderArr = JSONArray()
            for (o in orderRecords) {
                val obj = JSONObject()
                obj.put("id", o.id)
                obj.put("customerId", o.customerId)
                obj.put("ledgerId", o.ledgerId)
                obj.put("itemName", o.itemName)
                obj.put("price", o.price)
                obj.put("quantity", o.quantity)
                obj.put("orderDate", o.orderDate)
                obj.put("isCompleted", o.isCompleted)
                obj.put("updatedAt", o.updatedAt)
                orderArr.put(obj)
            }
            root.put("order_records", orderArr)

            return root.toString(2)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }

    // Holds parsed records for a user to bulk save
    data class BackupPayload(
        val shopName: String,
        val shopPhone: String,
        val shopAddress: String,
        val customers: List<Customer>,
        val templates: List<SizingTemplate>,
        val fields: List<MeasurementField>,
        val customerMeasurements: List<CustomerMeasurement>,
        val measurementValues: List<MeasurementValue>,
        val inventoryItems: List<InventoryItem>,
        val ledgerRecords: List<LedgerRecord>,
        val paymentRecords: List<PaymentRecord>,
        val orderRecords: List<OrderRecord>
    )

    fun parseBackup(jsonStr: String, userId: String): BackupPayload? {
        try {
            val root = JSONObject(jsonStr)

            // Shop Profile
            val shopObj = root.optJSONObject("shop_profile")
            val shopName = shopObj?.optString("name") ?: "Tailor Shop Master"
            val shopPhone = shopObj?.optString("phone") ?: "Not Configured"
            val shopAddress = shopObj?.optString("address") ?: "Not Configured"

            // Customers
            val customers = mutableListOf<Customer>()
            root.optJSONArray("customers")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    customers.add(
                        Customer(
                            id = obj.getString("id"),
                            userId = userId,
                            name = obj.getString("name"),
                            phone = obj.getString("phone"),
                            address = obj.getString("address"),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            isSynced = false,
                            isDeleted = false
                        )
                    )
                }
            }

            // Templates
            val templates = mutableListOf<SizingTemplate>()
            root.optJSONArray("sizing_templates")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    templates.add(
                        SizingTemplate(
                            id = obj.getString("id"),
                            userId = userId,
                            name = obj.getString("name"),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            isSynced = false,
                            isDeleted = false
                        )
                    )
                }
            }

            // Fields
            val fields = mutableListOf<MeasurementField>()
            root.optJSONArray("measurement_fields")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    fields.add(
                        MeasurementField(
                            id = obj.getString("id"),
                            userId = userId,
                            templateId = obj.optString("templateId", ""),
                            label = obj.getString("label"),
                            type = obj.getString("type"),
                            options = obj.optString("options", ""),
                            displayOrder = obj.optInt("displayOrder", 0),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            isSynced = false,
                            isDeleted = false
                        )
                    )
                }
            }

            // Customer Measurements
            val customerMeasurements = mutableListOf<CustomerMeasurement>()
            root.optJSONArray("customer_measurements")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    customerMeasurements.add(
                        CustomerMeasurement(
                            id = obj.getString("id"),
                            userId = userId,
                            customerId = obj.getString("customerId"),
                            templateId = obj.getString("templateId"),
                            title = obj.optString("title", "Standard"),
                            date = obj.optLong("date", System.currentTimeMillis()),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            isSynced = false,
                            isDeleted = false
                        )
                    )
                }
            }

            // Measurement Values
            val measurementValues = mutableListOf<MeasurementValue>()
            root.optJSONArray("measurement_values")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    measurementValues.add(
                        MeasurementValue(
                            id = obj.getString("id"),
                            userId = userId,
                            customerId = obj.getString("customerId"),
                            measurementId = obj.optString("measurementId", "default_measurement"),
                            fieldId = obj.getString("fieldId"),
                            value = obj.getString("value"),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            isSynced = false,
                            isDeleted = false
                        )
                    )
                }
            }

            // Inventory Items
            val inventoryItems = mutableListOf<InventoryItem>()
            root.optJSONArray("inventory_items")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    inventoryItems.add(
                        InventoryItem(
                            id = obj.getString("id"),
                            userId = userId,
                            name = obj.getString("name"),
                            unitType = obj.getString("unitType"),
                            purchasePrice = obj.getDouble("purchasePrice"),
                            sellingPrice = obj.getDouble("sellingPrice"),
                            stockQuantity = obj.getInt("stockQuantity"),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            isSynced = false,
                            isDeleted = false
                        )
                    )
                }
            }

            // Ledger Records
            val ledgerRecords = mutableListOf<LedgerRecord>()
            root.optJSONArray("ledger_records")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    ledgerRecords.add(
                        LedgerRecord(
                            id = obj.getString("id"),
                            userId = userId,
                            customerId = obj.getString("customerId"),
                            totalValue = obj.getDouble("totalValue"),
                            amountPaid = obj.getDouble("amountPaid"),
                            pendingDebt = obj.optDouble("pendingDebt", obj.getDouble("totalValue") - obj.getDouble("amountPaid")),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            isSynced = false,
                            isDeleted = false
                        )
                    )
                }
            }

            // Payment Records
            val paymentRecords = mutableListOf<PaymentRecord>()
            root.optJSONArray("payment_records")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    paymentRecords.add(
                        PaymentRecord(
                            id = obj.getString("id"),
                            userId = userId,
                            customerId = obj.getString("customerId"),
                            ledgerId = obj.optString("ledgerId", ""),
                            amountPaid = obj.getDouble("amountPaid"),
                            paymentDate = obj.optLong("paymentDate", System.currentTimeMillis()),
                            note = obj.optString("note", ""),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            isSynced = false,
                            isDeleted = false
                        )
                    )
                }
            }

            // Order Records
            val orderRecords = mutableListOf<OrderRecord>()
            root.optJSONArray("order_records")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    orderRecords.add(
                        OrderRecord(
                            id = obj.getString("id"),
                            userId = userId,
                            customerId = obj.getString("customerId"),
                            ledgerId = obj.optString("ledgerId", ""),
                            itemName = obj.getString("itemName"),
                            price = obj.getDouble("price"),
                            quantity = obj.optInt("quantity", 1),
                            orderDate = obj.optLong("orderDate", System.currentTimeMillis()),
                            isCompleted = obj.optBoolean("isCompleted", false),
                            updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                            isSynced = false,
                            isDeleted = false
                        )
                    )
                }
            }

            return BackupPayload(
                shopName = shopName,
                shopPhone = shopPhone,
                shopAddress = shopAddress,
                customers = customers,
                templates = templates,
                fields = fields,
                customerMeasurements = customerMeasurements,
                measurementValues = measurementValues,
                inventoryItems = inventoryItems,
                ledgerRecords = ledgerRecords,
                paymentRecords = paymentRecords,
                orderRecords = orderRecords
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
