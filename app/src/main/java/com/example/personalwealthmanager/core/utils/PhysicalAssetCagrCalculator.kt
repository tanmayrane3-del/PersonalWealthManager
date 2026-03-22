package com.example.personalwealthmanager.core.utils

import com.example.personalwealthmanager.domain.model.PhysicalAsset
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.pow

object PhysicalAssetCagrCalculator {

    private const val VEHICLE_DEPRECIATION_RATE = 0.15  // 15% WDV per year

    fun getCarCurrentValue(purchasePrice: Double, purchaseDateStr: String): Double {
        return try {
            val purchaseDate = LocalDate.parse(purchaseDateStr.take(10))
            val daysHeld = ChronoUnit.DAYS.between(purchaseDate, LocalDate.now())
            if (daysHeld <= 0) purchasePrice
            else purchasePrice * (1.0 - VEHICLE_DEPRECIATION_RATE).pow(daysHeld / 365.25)
        } catch (e: Exception) {
            purchasePrice
        }
    }

    fun getHomeCurrentValue(asset: PhysicalAsset): Double =
        asset.currentMarketValue ?: asset.purchasePrice

    fun getAssetCurrentValue(asset: PhysicalAsset): Double =
        if (asset.assetType == "vehicle") {
            getCarCurrentValue(asset.purchasePrice, asset.purchaseDate)
        } else {
            getHomeCurrentValue(asset)
        }

    fun getAssetCagr(asset: PhysicalAsset): Double =
        if (asset.assetType == "vehicle") {
            -VEHICLE_DEPRECIATION_RATE
        } else {
            try {
                val purchaseDate = LocalDate.parse(asset.purchaseDate.take(10))
                val yearsHeld = ChronoUnit.DAYS.between(purchaseDate, LocalDate.now()) / 365.25
                if (yearsHeld <= 0 || asset.purchasePrice <= 0) 0.0
                else {
                    val currentValue = getHomeCurrentValue(asset)
                    (currentValue / asset.purchasePrice).pow(1.0 / yearsHeld) - 1.0
                }
            } catch (e: Exception) {
                0.0
            }
        }

    fun getProjectedValue(asset: PhysicalAsset, years: Int): Double {
        val currentValue = getAssetCurrentValue(asset)
        val cagr = getAssetCagr(asset)
        return currentValue * (1.0 + cagr).pow(years.toDouble())
    }

    fun getOverallCagr(assets: List<PhysicalAsset>, years: Int): Double {
        if (assets.isEmpty()) return 0.0
        val totalCurrent = assets.sumOf { getAssetCurrentValue(it) }
        if (totalCurrent <= 0) return 0.0
        val totalProjected = assets.sumOf { getProjectedValue(it, years) }
        return (totalProjected / totalCurrent).pow(1.0 / years) - 1.0
    }
}
