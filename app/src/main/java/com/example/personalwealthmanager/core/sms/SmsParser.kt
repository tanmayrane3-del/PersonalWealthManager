package com.example.personalwealthmanager.core.sms

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ParsedSmsTransaction(
    val amount: String,
    val paymentIdentifier: String,
    val transactionReference: String?,
    val date: String,         // "yyyy-MM-dd"
    val time: String,         // "HH:mm"
    val paymentMethod: String,
    val bankSender: String
)

object SmsParser {

    fun parseSms(
        sender: String,
        body: String,
        receivedTimestampMs: Long
    ): ParsedSmsTransaction? {
        val senderUpper = sender.uppercase()
        return when {
            senderUpper.contains("BOBSMS") -> parseBob(body, receivedTimestampMs)
            senderUpper.contains("ICICIT") -> parseIcici(body, receivedTimestampMs)
            senderUpper.contains("KOTAKB") -> parseKotak(body, receivedTimestampMs)
            else -> null
        }
    }

    // BOB (Bank of Baroda)
    // Sample: Rs.80.00 Dr. from A/C XXXXXX1110 and Cr. to paytmqr15zd5dln17@paytm. Ref:602764013675. AvlBal:Rs2470.54(2026:01:27 10:04:54).
    private fun parseBob(body: String, timestampMs: Long): ParsedSmsTransaction? {
        val amountRegex = Regex("""Rs\.(\d+(?:\.\d+)?)\s+Dr\.""")
        val amount = amountRegex.find(body)?.groupValues?.get(1) ?: return null

        val identifierRegex = Regex("""Cr\.\s+to\s+(\S+?)(?=\.\s)""")
        val paymentIdentifier = identifierRegex.find(body)?.groupValues?.get(1) ?: return null

        val refRegex = Regex("""Ref:(\d+)""")
        val transactionReference = refRegex.find(body)?.groupValues?.get(1)

        // Date/time from SMS body: (yyyy:MM:dd HH:mm:ss)
        val dateTimeRegex = Regex("""\((\d{4}):(\d{2}):(\d{2})\s+(\d{2}):(\d{2}):\d{2}\)""")
        val dtMatch = dateTimeRegex.find(body)
        val (date, time) = if (dtMatch != null) {
            val year = dtMatch.groupValues[1]
            val month = dtMatch.groupValues[2]
            val day = dtMatch.groupValues[3]
            val hour = dtMatch.groupValues[4]
            val min = dtMatch.groupValues[5]
            "$year-$month-$day" to "$hour:$min"
        } else {
            timestampToDateAndTime(timestampMs)
        }

        return ParsedSmsTransaction(
            amount = amount,
            paymentIdentifier = paymentIdentifier,
            transactionReference = transactionReference,
            date = date,
            time = time,
            paymentMethod = "upi",
            bankSender = "BOB"
        )
    }

    // ICICI Bank
    // Sample: INR 2,780.00 spent using ICICI Bank Card XX3009 on 29-Jun-25 on IND*Amazon.in -. Avl Limit: INR 4,29,066.89.
    private fun parseIcici(body: String, timestampMs: Long): ParsedSmsTransaction? {
        val amountRegex = Regex("""INR\s+([\d,]+(?:\.\d+)?)\s+spent""")
        val rawAmount = amountRegex.find(body)?.groupValues?.get(1) ?: return null
        val amount = rawAmount.replace(",", "")

        val identifierRegex = Regex("""on\s+\d{2}-\w{3}-\d{2,4}\s+on\s+(.+?)(?=\s+-\.|\.\s)""")
        val paymentIdentifier = identifierRegex.find(body)?.groupValues?.get(1)?.trim() ?: return null

        val txDateRegex = Regex("""on\s+(\d{2}-\w{3}-\d{2,4})\s+on""")
        val txDateStr = txDateRegex.find(body)?.groupValues?.get(1)

        val (date, time) = timestampToDateAndTime(timestampMs)
        val transactionReference = if (txDateStr != null) {
            "${txDateStr}_${paymentIdentifier.replace(" ", "_")}"
        } else {
            null
        }

        return ParsedSmsTransaction(
            amount = amount,
            paymentIdentifier = paymentIdentifier,
            transactionReference = transactionReference,
            date = date,
            time = time,
            paymentMethod = "credit_card",
            bankSender = "ICICI"
        )
    }

    // Kotak Bank
    // Sample: INR 120 spent on Kotak Credit Card x3434 on 18-FEB-2026 at UPI-604914257596-07906. Avl limit INR 49007.46
    private fun parseKotak(body: String, timestampMs: Long): ParsedSmsTransaction? {
        val amountRegex = Regex("""INR\s+([\d,]+(?:\.\d+)?)\s+spent""")
        val rawAmount = amountRegex.find(body)?.groupValues?.get(1) ?: return null
        val amount = rawAmount.replace(",", "")

        val identifierRegex = Regex("""at\s+([^\s.]+)""")
        val paymentIdentifier = identifierRegex.find(body)?.groupValues?.get(1) ?: return null

        val txDateRegex = Regex("""on\s+(\d{2}-[A-Za-z]{3}-\d{4})\s+at""")
        val txDateStr = txDateRegex.find(body)?.groupValues?.get(1)

        val (date, time) = timestampToDateAndTime(timestampMs)
        val transactionReference = if (txDateStr != null) {
            "${txDateStr}_${paymentIdentifier}"
        } else {
            null
        }

        return ParsedSmsTransaction(
            amount = amount,
            paymentIdentifier = paymentIdentifier,
            transactionReference = transactionReference,
            date = date,
            time = time,
            paymentMethod = "credit_card",
            bankSender = "KOTAK"
        )
    }

    private fun timestampToDateAndTime(timestampMs: Long): Pair<String, String> {
        val cal = Calendar.getInstance().apply { timeInMillis = timestampMs }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        return dateFormat.format(cal.time) to timeFormat.format(cal.time)
    }
}
