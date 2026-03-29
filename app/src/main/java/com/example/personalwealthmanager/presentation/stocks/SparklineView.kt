package com.example.personalwealthmanager.presentation.stocks

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/**
 * Minimal sparkline view that draws a smooth polyline from a list of float values.
 * Used in the Expected Projection cards to visualise compound-growth paths.
 */
class SparklineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF00695C.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val path = Path()
    private var points: FloatArray = FloatArray(0)

    fun setData(points: FloatArray) {
        this.points = points
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (points.size < 2) return

        val w = width.toFloat()
        val h = height.toFloat()
        val pad = 4f

        val min = points.min()
        val max = points.max()
        val range = if (max - min == 0f) 1f else max - min

        path.reset()
        points.forEachIndexed { i, value ->
            val x = pad + i / (points.size - 1).toFloat() * (w - 2 * pad)
            val y = pad + (1f - (value - min) / range) * (h - 2 * pad)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        canvas.drawPath(path, paint)
    }

    companion object {
        /**
         * Build monthly compound-growth points from [currentValue] to a 1-year projection.
         * Returns 13 values (month 0 … month 12).
         */
        fun monthly1Y(currentValue: Double, projected1y: Double): FloatArray {
            val cagr = projected1y / currentValue - 1
            return FloatArray(13) { i ->
                (currentValue * Math.pow(1 + cagr, i / 12.0)).toFloat()
            }
        }

        /**
         * Build quarterly compound-growth points for a 3-year projection.
         * Returns 13 values (quarter 0 … quarter 12 = 3 years).
         */
        fun quarterly3Y(currentValue: Double, projected3y: Double): FloatArray {
            val cagr = Math.pow(projected3y / currentValue, 1.0 / 3) - 1
            return FloatArray(13) { i ->
                (currentValue * Math.pow(1 + cagr, i * 3.0 / 12.0)).toFloat()
            }
        }

        /**
         * Build semi-annual compound-growth points for a 5-year projection.
         * Returns 11 values (half 0 … half 10 = 5 years).
         */
        fun semiAnnual5Y(currentValue: Double, projected5y: Double): FloatArray {
            val cagr = Math.pow(projected5y / currentValue, 1.0 / 5) - 1
            return FloatArray(11) { i ->
                (currentValue * Math.pow(1 + cagr, i * 0.5)).toFloat()
            }
        }
    }
}
