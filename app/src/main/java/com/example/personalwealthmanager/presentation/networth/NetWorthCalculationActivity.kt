package com.example.personalwealthmanager.presentation.networth

import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.data.remote.dto.NetWorthCurrentDto
import com.google.gson.Gson
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

class NetWorthCalculationActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DTO = "extra_net_worth_dto"
    }

    private val currencyFmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_net_worth_calculation)

        val json = intent.getStringExtra(EXTRA_DTO)
        if (json == null) { finish(); return }
        val dto = Gson().fromJson(json, NetWorthCurrentDto::class.java)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { onBackPressed() }

        populateGrowthRateTable(dto)
        populateValuationTable(dto)
        updateInsights(dto)
    }

    // ── Growth Rate Table ──────────────────────────────────────────────────────

    private fun populateGrowthRateTable(dto: NetWorthCurrentDto) {
        setCagrCell(R.id.tvGrStocks1y, cagrPct(dto.stocksValue, dto.stocksProj1y, 1))
        setCagrCell(R.id.tvGrStocks3y, cagrPct(dto.stocksValue, dto.stocksProj3y, 3))
        setCagrCell(R.id.tvGrStocks5y, cagrPct(dto.stocksValue, dto.stocksProj5y, 5))

        setCagrCell(R.id.tvGrMf1y,     cagrPct(dto.mfValue,     dto.mfProj1y,     1))
        setCagrCell(R.id.tvGrMf3y,     cagrPct(dto.mfValue,     dto.mfProj3y,     3))
        setCagrCell(R.id.tvGrMf5y,     cagrPct(dto.mfValue,     dto.mfProj5y,     5))

        setCagrCell(R.id.tvGrMetals1y, cagrPct(dto.metalsValue, dto.metalsProj1y, 1))
        setCagrCell(R.id.tvGrMetals3y, cagrPct(dto.metalsValue, dto.metalsProj3y, 3))
        setCagrCell(R.id.tvGrMetals5y, cagrPct(dto.metalsValue, dto.metalsProj5y, 5))
    }

    private fun setCagrCell(id: Int, pct: Double) {
        val tv = findViewById<TextView>(id)
        tv.text = formatCagrPct(pct)
        tv.setTextColor(if (pct >= 0) Color.parseColor("#004F45") else Color.parseColor("#B71C1C"))
    }

    // ── Valuation Breakdown Table ──────────────────────────────────────────────

    private fun populateValuationTable(dto: NetWorthCurrentDto) {
        setCell(R.id.tvVbStocks1y, dto.stocksProj1y)
        setCell(R.id.tvVbStocks3y, dto.stocksProj3y)
        setCell(R.id.tvVbStocks5y, dto.stocksProj5y)

        setCell(R.id.tvVbMf1y, dto.mfProj1y)
        setCell(R.id.tvVbMf3y, dto.mfProj3y)
        setCell(R.id.tvVbMf5y, dto.mfProj5y)

        setCell(R.id.tvVbMetals1y, dto.metalsProj1y)
        setCell(R.id.tvVbMetals3y, dto.metalsProj3y)
        setCell(R.id.tvVbMetals5y, dto.metalsProj5y)

        setCell(R.id.tvVbOther1y, dto.otherProj1y)
        setCell(R.id.tvVbOther3y, dto.otherProj3y)
        setCell(R.id.tvVbOther5y, dto.otherProj5y)

        val ta1y = dto.stocksProj1y + dto.mfProj1y + dto.metalsProj1y + dto.otherProj1y
        val ta3y = dto.stocksProj3y + dto.mfProj3y + dto.metalsProj3y + dto.otherProj3y
        val ta5y = dto.stocksProj5y + dto.mfProj5y + dto.metalsProj5y + dto.otherProj5y
        setCell(R.id.tvVbTotalAssets1y, ta1y)
        setCell(R.id.tvVbTotalAssets3y, ta3y)
        setCell(R.id.tvVbTotalAssets5y, ta5y)

        setCell(R.id.tvVbLiab1y, dto.liabProj1y)
        setCell(R.id.tvVbLiab3y, dto.liabProj3y)
        setCell(R.id.tvVbLiab5y, dto.liabProj5y)

        setCell(R.id.tvVbNw1y, dto.projected1y)
        setCell(R.id.tvVbNw3y, dto.projected3y)
        setCell(R.id.tvVbNw5y, dto.projected5y)
    }

    private fun setCell(id: Int, value: Double) {
        findViewById<TextView>(id).text = formatCompact(value)
    }

    // ── Insights card ──────────────────────────────────────────────────────────

    private fun updateInsights(dto: NetWorthCurrentDto) {
        val lines = mutableListOf<String>()

        // Liability-free year
        val liabFree = when {
            dto.liabProj1y == 0.0 && dto.totalLiabilities > 0 -> "1 year"
            dto.liabProj3y == 0.0 && dto.totalLiabilities > 0 -> "3 years"
            dto.liabProj5y == 0.0 && dto.totalLiabilities > 0 -> "5 years"
            else -> null
        }
        if (liabFree != null) {
            lines += "By $liabFree, your liabilities are projected to reach ₹0, significantly increasing your financial freedom and investment capacity."
        }

        // 5Y net worth growth
        if (dto.netWorth > 0 && dto.projected5y > dto.netWorth) {
            val growthPct = ((dto.projected5y / dto.netWorth - 1) * 100).toInt()
            lines += "Your net worth is projected to grow $growthPct% over the next 5 years based on historical CAGR performance."
        }

        // Debt-to-asset ratio at year 5
        val assets5y = dto.stocksProj5y + dto.mfProj5y + dto.metalsProj5y + dto.otherProj5y
        if (assets5y > 0 && dto.liabProj5y >= 0) {
            val dtar = (dto.liabProj5y / assets5y * 100).toInt()
            if (dtar == 0) {
                lines += "By Year 5, your debt-to-asset ratio is projected to reach 0%, fully eliminating financial leverage from your portfolio."
            } else {
                lines += "By Year 5, your projected debt-to-asset ratio is $dtar%."
            }
        }

        val body = lines.joinToString("\n\n").ifBlank {
            "Your projections are based on historical asset-class CAGR rates and compound growth models."
        }
        findViewById<TextView>(R.id.tvInsightsBody).text = body
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun cagrPct(currentValue: Double, projectedValue: Double, years: Int): Double {
        if (currentValue <= 0 || projectedValue <= 0) return 0.0
        return ((projectedValue / currentValue).pow(1.0 / years) - 1) * 100
    }

    private fun formatCagrPct(pct: Double): String {
        val sign = if (pct >= 0) "+" else ""
        return "$sign${String.format("%.1f", pct)}%"
    }

    private fun formatCompact(amount: Double): String = when {
        abs(amount) >= 1_00_00_000 -> "₹${String.format("%.2f", amount / 1_00_00_000)}Cr"
        abs(amount) >= 1_00_000    -> "₹${String.format("%.2f", amount / 1_00_000)}L"
        else                       -> currencyFmt.format(amount)
    }
}
