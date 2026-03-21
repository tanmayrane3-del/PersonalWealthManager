package com.example.personalwealthmanager.presentation.otherassets

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.personalwealthmanager.MainActivity
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.presentation.categories.CategoryManagementActivity
import com.example.personalwealthmanager.presentation.liabilities.LiabilitiesActivity
import com.example.personalwealthmanager.presentation.metals.MetalsActivity
import com.example.personalwealthmanager.presentation.mutualfunds.MutualFundsActivity
import com.example.personalwealthmanager.presentation.recipients.RecipientManagementActivity
import com.example.personalwealthmanager.presentation.settings.SettingsActivity
import com.example.personalwealthmanager.presentation.sources.SourceManagementActivity
import com.example.personalwealthmanager.presentation.stocks.StocksActivity
import com.example.personalwealthmanager.presentation.transactions.TransactionsActivity
import com.example.personalwealthmanager.presentation.zerodha.SetupZerodhaActivity
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OtherAssetsActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other_assets)

        drawerLayout = findViewById(R.id.drawerLayout)

        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.other_assets_fragment_container, OtherAssetsFragment())
                .commit()
        }

        setupNavigationDrawer()
    }

    private fun setupNavigationDrawer() {
        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val headerView = navigationView.getHeaderView(0)

        headerView.findViewById<TextView>(R.id.tvUserEmail)?.text =
            getSharedPreferences("AppPrefs", MODE_PRIVATE).getString("user_email", "user@example.com")

        headerView.findViewById<Button>(R.id.btnDashboard)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        headerView.findViewById<Button>(R.id.btnTransactions)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, TransactionsActivity::class.java))
            finish()
        }

        setupExpandable(headerView, R.id.btnManagement, R.id.ivManagementExpand, R.id.managementChildItems)

        headerView.findViewById<Button>(R.id.btnCategoryManagement)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, CategoryManagementActivity::class.java))
        }

        headerView.findViewById<Button>(R.id.btnSourceManagement)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, SourceManagementActivity::class.java))
        }

        headerView.findViewById<Button>(R.id.btnRecipientManagement)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, RecipientManagementActivity::class.java))
        }

        setupExpandable(headerView, R.id.btnAssets, R.id.ivAssetsExpand, R.id.assetsChildItems)

        headerView.findViewById<Button>(R.id.btnStocks)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, StocksActivity::class.java))
            finish()
        }

        headerView.findViewById<Button>(R.id.btnMetals)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, MetalsActivity::class.java))
            finish()
        }

        headerView.findViewById<Button>(R.id.btnMutualFunds)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, MutualFundsActivity::class.java))
            finish()
        }

        headerView.findViewById<Button>(R.id.btnOtherAssets)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            // Already on this screen
        }

        headerView.findViewById<Button>(R.id.btnLiabilities)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, LiabilitiesActivity::class.java))
            finish()
        }

        setupExpandable(headerView, R.id.btnSetupDemat, R.id.ivSetupDematExpand, R.id.setupDematChildItems)

        headerView.findViewById<Button>(R.id.btnConnectZerodha)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, SetupZerodhaActivity::class.java))
        }

        headerView.findViewById<Button>(R.id.btnSettings)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        headerView.findViewById<Button>(R.id.btnLogout)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            getSharedPreferences("AppPrefs", MODE_PRIVATE).edit().clear().apply()
            val intent = Intent(this, com.example.personalwealthmanager.presentation.auth.login.LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupExpandable(headerView: View, btnId: Int, iconId: Int, containerId: Int) {
        val btn = headerView.findViewById<Button>(btnId)
        val icon = headerView.findViewById<ImageView>(iconId)
        val container = headerView.findViewById<LinearLayout>(containerId)

        btn?.setOnClickListener {
            if (container?.visibility == View.VISIBLE) {
                container.visibility = View.GONE
                icon?.setImageResource(R.drawable.ic_expand_more)
            } else {
                container?.visibility = View.VISIBLE
                icon?.setImageResource(R.drawable.ic_expand_less)
            }
        }
        icon?.setOnClickListener { btn?.performClick() }
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}
