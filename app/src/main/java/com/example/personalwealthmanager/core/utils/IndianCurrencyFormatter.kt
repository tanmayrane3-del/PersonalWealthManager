package com.pwm.personalwealthmanager.core.utils

import java.text.NumberFormat
import java.util.Locale

object IndianCurrencyFormatter {
    private val format = NumberFormat.getInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 0
    }
    private val formatWithDecimal = NumberFormat.getInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    fun format(amount: Double, decimals: Boolean = false): String {
        val f = if (decimals) formatWithDecimal else format
        return "₹${f.format(amount)}"
    }

    fun compact(amount: Double): String = when {
        amount >= 1_00_00_000 -> "₹${"%.2f".format(amount / 1_00_00_000)}Cr"
        amount >= 1_00_000    -> "₹${"%.2f".format(amount / 1_00_000)}L"
        amount >= 1_000       -> "₹${"%.1f".format(amount / 1_000)}K"
        else                  -> format(amount)
    }
}
