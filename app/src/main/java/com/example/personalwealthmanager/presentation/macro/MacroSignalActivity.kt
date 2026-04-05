package com.example.personalwealthmanager.presentation.macro

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.data.remote.dto.MacroAccuracyDto
import com.example.personalwealthmanager.data.remote.dto.MacroHistoryItemDto
import com.example.personalwealthmanager.data.remote.dto.MacroSignalDto
import com.example.personalwealthmanager.databinding.ActivityMacroSignalBinding
import com.example.personalwealthmanager.presentation.base.BaseDrawerActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

@AndroidEntryPoint
class MacroSignalActivity : BaseDrawerActivity() {

    private lateinit var binding: ActivityMacroSignalBinding
    private val viewModel: MacroSignalViewModel by viewModels()

    private val BULL  = Color.parseColor("#639922")
    private val BEAR  = Color.parseColor("#E24B4A")
    private val nf    = NumberFormat.getNumberInstance(Locale("en", "IN"))

    override fun getSelfButtonId() = R.id.btnMarketSignal

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMacroSignalBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.END) }
        binding.btnRetry.setOnClickListener { viewModel.load() }

        setupDrawerMenu()
        setupBottomNav()

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is MacroUiState.Loading -> showLoading()
                    is MacroUiState.Success -> renderSuccess(state)
                    is MacroUiState.Error   -> showError(state.message)
                }
            }
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private fun showLoading() {
        binding.layoutLoading.visibility = View.VISIBLE
        binding.layoutError.visibility   = View.GONE
        binding.layoutContent.visibility = View.GONE
    }

    private fun showError(msg: String) {
        binding.layoutLoading.visibility = View.GONE
        binding.layoutError.visibility   = View.VISIBLE
        binding.layoutContent.visibility = View.GONE
        binding.tvError.text = msg
    }

    private fun renderSuccess(state: MacroUiState.Success) {
        binding.layoutLoading.visibility = View.GONE
        binding.layoutError.visibility   = View.GONE
        binding.layoutContent.visibility = View.VISIBLE

        val s = state.signal
        if (s != null) {
            renderHeader(s)
            renderHeroCard(s)
            renderFactorTable(s)
            renderMacroGrid(s)
        }
        renderBacktest(state.history, state.accuracy)
    }

    // ── Header ────────────────────────────────────────────────────────────────

    private fun renderHeader(s: MacroSignalDto) {
        val monthLabel = try {
            val d = LocalDate.parse(s.month.substring(0, 10))
            "NIFTY 50 · ${d.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)).uppercase()}"
        } catch (e: Exception) { "NIFTY 50" }
        binding.tvHeaderLabel.text = monthLabel
        binding.tvHeaderNiftyPrice.text = "₹${nf.format(s.niftyClose.toLong())}"
    }

    // ── Hero Card ─────────────────────────────────────────────────────────────

    private fun renderHeroCard(s: MacroSignalDto) {
        val score        = if (s.isFinal && s.finalScore != null) s.finalScore else s.totalScore
        val displaySig   = if (s.isFinal && s.finalSignal != null) s.finalSignal else s.signal
        val isBull       = displaySig.contains("bull")
        val accentColor  = if (isBull) BULL else if (displaySig == "neutral") Color.parseColor("#757575") else BEAR

        // Left accent bar
        binding.signalAccentBar.setBackgroundColor(accentColor)

        // Date label
        val targetDate = try {
            val d = LocalDate.parse(s.month.substring(0, 10))
            val lastDay = d.withDayOfMonth(d.lengthOfMonth())
            lastDay.format(DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)).uppercase() + " TARGET"
        } catch (e: Exception) { "TARGET" }
        binding.tvHeroDateLabel.text = targetDate

        // Target price + return from prev
        binding.tvHeroTargetPrice.text = "₹${nf.format(s.targetNifty)}"
        val ret = s.predictedReturnPct
        val retStr = "${if (ret >= 0) "+" else ""}${"%.1f".format(ret)}% from ${prevMonthLabel(s.month)} close"
        binding.tvHeroReturnPct.text = retStr
        binding.tvHeroReturnPct.setTextColor(accentColor)
        val prevClose = s.prevNifty
        binding.tvHeroPrevRef.text = if (prevClose != null)
            "${prevMonthLabel(s.month)} closed at ₹${nf.format(prevClose.toLong())}"
        else ""

        // Score + signal tag
        binding.tvHeroScore.text = formatScore(score)
        binding.tvHeroScore.setTextColor(accentColor)
        binding.tvHeroSignalTag.text = signalLabel(displaySig)
        applyTagStyle(binding.tvHeroSignalTag, score)

        // Range bar
        binding.tvRangeWorst.text = "Worst ₹${nf.format(s.targetNiftyLow)}"
        binding.tvRangeBest.text  = "Best ₹${nf.format(s.targetNiftyHigh)}"

        // Bar fill: where current Nifty sits in low→high range (clamped 5-95%)
        val range = (s.targetNiftyHigh - s.targetNiftyLow).toFloat()
        val pos   = if (range > 0) ((s.niftyClose - s.targetNiftyLow) / range * 100f).toFloat() else 50f
        val fillPct  = pos.coerceIn(5f, 95f)
        val emptyPct = 100f - fillPct
        (binding.rangeBarFill.layoutParams  as LinearLayout.LayoutParams).weight = fillPct
        (binding.rangeBarEmpty.layoutParams as LinearLayout.LayoutParams).weight = emptyPct
        binding.rangeBarFill.setBackgroundColor(accentColor)
        binding.rangeBarTrack.requestLayout()

        // Chips
        val (chip1text, chip1ok) = freshnessChip(s.tradingDayN)
        val (chip2text, chip2ok) = accuracyChip(s.accuracyAtScore, s.historicalMonths)
        val (chip3text, chip3ok) = directionChip(s.pctPositive, s.predictedDirection)
        applyChip(binding.tvChip1, chip1text, chip1ok)
        applyChip(binding.tvChip2, chip2text, chip2ok)
        applyChip(binding.tvChip3, chip3text, chip3ok)
    }

    // ── Factor Table ──────────────────────────────────────────────────────────

    private fun renderFactorTable(s: MacroSignalDto) {
        val score = if (s.isFinal && s.finalScore != null) s.finalScore else s.totalScore
        val displaySig = if (s.isFinal && s.finalSignal != null) s.finalSignal else s.signal

        binding.tvFactorSectionTitle.text = "Why score ${formatScore(score)}?"

        // Net Flow reading
        val netFlow = s.netFlowMtd
        val netFlowStr = if (netFlow != null) {
            val sign = if (netFlow >= 0) "+" else ""
            "${sign}₹${nf.format(abs(netFlow.toLong()))} cr"
        } else "—"
        binding.tvReadingNetFlow.text = netFlowStr
        binding.tvReadingNetFlow.setTextColor(scoreReadingColor(s.scoreNetFlow))
        applyPill(binding.tvPillNetFlow, s.scoreNetFlow)

        // VIX
        binding.tvReadingVix.text = s.indiaVixHigh?.let { "%.2f".format(it) } ?: "—"
        binding.tvReadingVix.setTextColor(Color.parseColor("#212121"))
        applyPill(binding.tvPillVix, s.scoreVix)

        // NASDAQ momentum (computed from prev)
        val nasdaqMom = if (s.nasdaqClose != null && s.prevNasdaq != null && s.prevNasdaq > 0)
            (s.nasdaqClose - s.prevNasdaq) / s.prevNasdaq * 100
        else null
        binding.tvReadingNasdaq.text = nasdaqMom?.let {
            "${if (it >= 0) "+" else ""}${"%.2f".format(it)}%"
        } ?: (s.nasdaqClose?.let { nf.format(it.toLong()) } ?: "—")
        binding.tvReadingNasdaq.setTextColor(scoreReadingColor(s.scoreNasdaq))
        applyPill(binding.tvPillNasdaq, s.scoreNasdaq)

        // USD/INR
        binding.tvReadingInr.text = s.usdInr?.let { "₹${"%.2f".format(it)}" } ?: "—"
        binding.tvReadingInr.setTextColor(Color.parseColor("#212121"))
        applyPill(binding.tvPillInr, s.scoreInr)

        // Oil
        binding.tvReadingOil.text = s.oilBrent?.let { "$$${"%.2f".format(it)}" } ?: "—"
        binding.tvReadingOil.setTextColor(Color.parseColor("#212121"))
        applyPill(binding.tvPillOil, s.scoreOil)

        // Nifty trend (computed from prev)
        val niftyTrend = if (s.prevNifty != null && s.prevNifty > 0)
            (s.niftyClose - s.prevNifty) / s.prevNifty * 100
        else null
        binding.tvReadingTrend.text = niftyTrend?.let {
            "${if (it >= 0) "+" else ""}${"%.2f".format(it)}%"
        } ?: "—"
        binding.tvReadingTrend.setTextColor(Color.parseColor("#212121"))
        applyPill(binding.tvPillTrend, s.scoreTrend)

        // Total
        applyPill(binding.tvPillTotal, score, large = true)
    }

    // ── Macro Grid ────────────────────────────────────────────────────────────

    private fun renderMacroGrid(s: MacroSignalDto) {
        fun fmtCr(v: Double?): String {
            if (v == null) return "—"
            val abs = abs(v)
            val sign = if (v >= 0) "+" else "-"
            return "${sign}₹${nf.format(abs.toLong())} cr"
        }
        fun fmtPct(v: Double?) = v?.let { "${"%.2f".format(it)}%" } ?: "—"
        fun fmtNum(v: Double?) = v?.let { nf.format(it.toLong()) } ?: "—"

        binding.tvKvFiiSold.text   = fmtCr(s.fiiNetMtd)
        binding.tvKvFiiSold.setTextColor(if ((s.fiiNetMtd ?: 0.0) >= 0) BULL else BEAR)

        binding.tvKvDiiBought.text = fmtCr(s.diiNetMtd)
        binding.tvKvDiiBought.setTextColor(if ((s.diiNetMtd ?: 0.0) >= 0) BULL else BEAR)

        binding.tvKvFedRate.text   = fmtPct(s.fedRate)
        binding.tvKvRbiRate.text   = fmtPct(s.rbiRate)
        binding.tvKvDxy.text       = s.dxy?.let { "%.2f".format(it) } ?: "—"
        binding.tvKvHangSeng.text  = fmtNum(s.hsiClose)
        binding.tvKvNasdaqNow.text = fmtNum(s.nasdaqClose)
        binding.tvKvNasdaqPrev.text = fmtNum(s.prevNasdaq)
    }

    // ── Backtest ──────────────────────────────────────────────────────────────

    private fun renderBacktest(history: List<MacroHistoryItemDto>, accuracy: List<MacroAccuracyDto>) {
        // Aggregate stats from history (is_correct field per month)
        val totalDecided = history.count { it.isCorrect != null }
        val correctCalls = history.count { it.isCorrect == true }
        val wrongCalls   = history.count { it.isCorrect == false }
        val overallPct   = if (totalDecided > 0) correctCalls.toFloat() / totalDecided * 100f else 0f

        binding.tvAccPct.text    = if (totalDecided > 0) "${"%.1f".format(overallPct)}%" else "N/A"
        binding.tvAccMonths.text = "$totalDecided months"
        binding.tvCorrect.text   = "$correctCalls"
        binding.tvWrong.text     = "$wrongCalls"

        // Backtest title
        val historyCount = history.size
        binding.tvBacktestTitle.text = "Was the model right? — $historyCount months"

        // Footer
        if (history.isNotEmpty()) {
            val oldest = history.lastOrNull()?.month?.let { formatMonthShort(it) } ?: ""
            val newest = history.firstOrNull()?.month?.let { formatMonthShort(it) } ?: ""
            binding.tvBacktestFooter.text = "$oldest – $newest · not a trading signal"
        }

        // Monthly rows
        binding.backtestContainer.removeAllViews()
        if (history.isEmpty()) {
            binding.backtestContainer.addView(makeTextView("No history available yet.", Color.GRAY, 13f))
            return
        }

        val maxRetAbs = history.mapNotNull { it.actualRet1m ?: it.predictedReturnPct }.maxOfOrNull { abs(it) }?.toFloat() ?: 5f

        history.forEach { item ->
            val isLive   = item.actualRet1m == null && item.isFinal != true
            val retValue = item.actualRet1m ?: (if (item.isFinal == true) item.predictedReturnPct else null)
            val scoreVal = if (item.isFinal == true && item.finalScore != null) item.finalScore else item.totalScore

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = Gravity.CENTER_VERTICAL
                setPadding(0, 5, 0, 5)
            }

            // Month (44dp)
            row.addView(makeTextView(formatMonthShort(item.month), Color.parseColor("#616161"), 11f).apply {
                layoutParams = LinearLayout.LayoutParams(dp(44), LinearLayout.LayoutParams.WRAP_CONTENT)
            })

            // Score (28dp)
            row.addView(makeTextView(formatScore(scoreVal), scoreColor(scoreVal), 10f, bold = true).apply {
                layoutParams = LinearLayout.LayoutParams(dp(28), LinearLayout.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER }
                gravity = Gravity.CENTER
            })

            // Return bar (flex)
            val barFrame = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(12), 1f).apply {
                    marginStart = dp(4); marginEnd = dp(4)
                }
                setBackgroundColor(Color.parseColor("#F0F0F0"))
            }
            if (retValue != null) {
                val barPct   = min(abs(retValue.toFloat()) / maxRetAbs, 1f)
                val barColor = if (retValue >= 0) BULL else BEAR
                val barView  = View(this).apply {
                    setBackgroundColor(barColor)
                }
                val fullW    = resources.displayMetrics.widthPixels // will be set in post
                barFrame.post {
                    val halfW  = (barFrame.width / 2f).toInt()
                    val barW   = (halfW * barPct).toInt().coerceAtLeast(2)
                    val params = FrameLayout.LayoutParams(barW, FrameLayout.LayoutParams.MATCH_PARENT)
                    if (retValue >= 0) params.leftMargin  = halfW
                    else               params.leftMargin  = halfW - barW
                    barView.layoutParams = params
                }
                barFrame.addView(barView)
            }
            row.addView(barFrame)

            // Actual return (50dp)
            val retText  = when {
                isLive   -> "in progress"
                retValue != null -> "${if (retValue >= 0) "+" else ""}${"%.1f".format(retValue)}%"
                else     -> "—"
            }
            val retColor = when {
                isLive   -> Color.GRAY
                retValue != null && retValue >= 0 -> BULL
                retValue != null -> BEAR
                else     -> Color.GRAY
            }
            row.addView(makeTextView(retText, retColor, 10f).apply {
                layoutParams = LinearLayout.LayoutParams(dp(50), LinearLayout.LayoutParams.WRAP_CONTENT)
                gravity = Gravity.END
            })

            // ✓/✗ (16dp)
            val tick     = when {
                isLive           -> "·"
                item.isCorrect == true  -> "✓"
                item.isCorrect == false -> "✗"
                else             -> "·"
            }
            val tickColor = when {
                item.isCorrect == true  -> BULL
                item.isCorrect == false -> BEAR
                else -> Color.GRAY
            }
            row.addView(makeTextView(tick, tickColor, 11f).apply {
                layoutParams = LinearLayout.LayoutParams(dp(16), LinearLayout.LayoutParams.WRAP_CONTENT)
                gravity = Gravity.CENTER
            })

            binding.backtestContainer.addView(row)

            // Thin divider
            binding.backtestContainer.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                setBackgroundColor(Color.parseColor("#F0F0F0"))
            })
        }
    }

    // ── Chip helpers ──────────────────────────────────────────────────────────

    private fun freshnessChip(day: Int): Pair<String, Boolean> = when {
        day <= 3  -> "Day $day of 22 · Early signal · updates daily" to false
        day <= 9  -> "Day $day of 22 · Building · updates daily" to false
        day <= 21 -> "Day $day of 22 · High confidence · full accuracy reached" to true
        else      -> "Day $day of 22 · Final · month closed" to true
    }

    private fun accuracyChip(acc: Double?, months: Int?): Pair<String, Boolean> {
        if (acc == null || months == null) return "Not enough history" to false
        return if (months < 5)
            "Rare signal · $months past cases · ${acc.toInt()}% correct" to false
        else
            "${acc.toInt()}% accurate across $months similar months" to (acc >= 65)
    }

    private fun directionChip(pctPositive: Double?, direction: String): Pair<String, Boolean> {
        if (pctPositive == null) return "Direction data unavailable" to false
        return if (direction == "bear") {
            val fell = (100 - pctPositive).toInt()
            "Nifty fell in $fell% of similar months" to (fell >= 60)
        } else {
            val rose = pctPositive.toInt()
            "Nifty rose in $rose% of similar months" to (rose >= 60)
        }
    }

    private fun applyChip(tv: TextView, text: String, ok: Boolean) {
        tv.text = "● $text"
        val (bg, fg) = when {
            ok    -> Color.parseColor("#EAF3DE") to Color.parseColor("#3B6D11")
            text.contains("Rare") || text.contains("Early") || text.contains("Building") ->
                Color.parseColor("#FAEEDA") to Color.parseColor("#854F0B")
            else  -> Color.parseColor("#F0F0F0") to Color.parseColor("#616161")
        }
        tv.setTextColor(fg)
        val gd = GradientDrawable().apply { setColor(bg); cornerRadius = dp(20).toFloat() }
        tv.background = gd
    }

    // ── Pill helpers ──────────────────────────────────────────────────────────

    private fun applyPill(tv: TextView, score: Int?, large: Boolean = false) {
        val s = score ?: 0
        tv.text = formatScore(s)
        if (!large) tv.textSize = 11f else tv.textSize = 13f
        val (bg, fg) = when {
            s <= -2 -> Color.parseColor("#FCEBEB") to Color.parseColor("#A32D2D")
            s == -1 -> Color.parseColor("#FAEEDA") to Color.parseColor("#854F0B")
            s == 0  -> Color.parseColor("#F0F0F0") to Color.parseColor("#757575")
            s == 1  -> Color.parseColor("#EAF3DE") to Color.parseColor("#3B6D11")
            else    -> Color.parseColor("#EAF3DE") to Color.parseColor("#27500A")
        }
        tv.setTextColor(fg)
        val gd = GradientDrawable().apply { setColor(bg); cornerRadius = dp(4).toFloat() }
        tv.background = gd
    }

    private fun applyTagStyle(tv: TextView, score: Int) {
        val (bg, fg) = if (score >= 0)
            Color.parseColor("#EAF3DE") to Color.parseColor("#3B6D11")
        else
            Color.parseColor("#FCEBEB") to Color.parseColor("#A32D2D")
        tv.setTextColor(fg)
        val gd = GradientDrawable().apply { setColor(bg); cornerRadius = dp(4).toFloat() }
        tv.background = gd
    }

    // ── Format helpers ────────────────────────────────────────────────────────

    private fun formatScore(score: Int?) = when {
        score == null -> "—"
        score > 0     -> "+$score"
        else          -> "$score"
    }

    private fun signalLabel(signal: String) = when (signal) {
        "strong_bull"   -> "Strong Bull"
        "mild_bull"     -> "Mild Bull"
        "cautious_bull" -> "Cautious Bull"
        "neutral"       -> "Neutral"
        "cautious_bear" -> "Cautious Bear"
        "mild_bear"     -> "Mild Bear"
        "strong_bear"   -> "Strong Bear"
        else            -> signal.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    private fun scoreColor(score: Int) = when {
        score > 0 -> BULL
        score < 0 -> BEAR
        else      -> Color.parseColor("#616161")
    }

    private fun scoreReadingColor(score: Int?): Int = when {
        (score ?: 0) > 0 -> BULL
        (score ?: 0) < 0 -> BEAR
        else              -> Color.parseColor("#212121")
    }

    private fun formatMonthShort(dateStr: String): String = try {
        val d = LocalDate.parse(dateStr.substring(0, 10))
        d.format(DateTimeFormatter.ofPattern("MMM ''yy", Locale.ENGLISH))
    } catch (e: Exception) { dateStr }

    private fun prevMonthLabel(monthStr: String): String = try {
        val d = LocalDate.parse(monthStr.substring(0, 10)).minusMonths(1)
        d.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH))
    } catch (e: Exception) { "Prev" }

    private fun makeTextView(text: String, color: Int, size: Float, bold: Boolean = false) =
        TextView(this).apply {
            this.text = text
            textSize  = size
            setTextColor(color)
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
        }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
