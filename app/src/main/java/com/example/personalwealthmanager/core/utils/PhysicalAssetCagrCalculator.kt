package com.example.personalwealthmanager.core.utils

import com.example.personalwealthmanager.domain.model.PhysicalAsset
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.pow

object PhysicalAssetCagrCalculator {

    fun getCarCurrentValue(purchasePrice: Double, purchaseDateStr: String): Double {
        return try {
            val purchaseDate = LocalDate.parse(purchaseDateStr)
            val yearsHeld = ChronoUnit.DAYS.between(purchaseDate, LocalDate.now()) / 365.0
            if (yearsHeld <= 0) {
                purchasePrice
            } else if (yearsHeld <= 1) {
                purchasePrice * (1 - 0.15 * yearsHeld)
            } else {
                purchasePrice * 0.85 * (0.90.pow(yearsHeld - 1))
            }
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

    fun getAssetCagr(asset: PhysicalAsset): Double {
        return if (asset.assetType == "vehicle") {
            -((asset.depreciationRatePct ?: 10.0) / 100.0)
        } else {
            val currentValue = getHomeCurrentValue(asset)
            return try {
                val purchaseDate = LocalDate.parse(asset.purchaseDate)
                val yearsHeld = ChronoUnit.DAYS.between(purchaseDate, LocalDate.now()) / 365.0
                if (yearsHeld <= 0 || asset.purchasePrice <= 0) return 0.0
                (currentValue / asset.purchasePrice).pow(1.0 / yearsHeld) - 1.0
            } catch (e: Exception) {
                0.0
            }
        }
    }

    fun getProjectedValue(asset: PhysicalAsset, years: Int): Double {
        val currentValue = getAssetCurrentValue(asset)
        val cagr = getAssetCagr(asset)
        return currentValue * (1 + cagr).pow(years.toDouble())
    }

    fun getOverallCagr(assets: List<PhysicalAsset>, years: Int): Double {
        if (assets.isEmpty()) return 0.0
        val totalCurrent = assets.sumOf { getAssetCurrentValue(it) }
        if (totalCurrent <= 0) return 0.0
        val totalProjected = assets.sumOf { getProjectedValue(it, years) }
        return (totalProjected / totalCurrent).pow(1.0 / years) - 1.0
    }
}
