package com.example.personalwealthmanager.presentation.base

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.personalwealthmanager.ApiConfig
import com.example.personalwealthmanager.LoginActivity
import com.example.personalwealthmanager.MainActivity
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.presentation.categories.CategoryManagementActivity
import com.example.personalwealthmanager.presentation.liabilities.LiabilitiesActivity
import com.example.personalwealthmanager.presentation.metals.MetalsActivity
import com.example.personalwealthmanager.presentation.mutualfunds.MutualFundsActivity
import com.example.personalwealthmanager.presentation.networth.NetWorthActivity
import com.example.personalwealthmanager.presentation.otherassets.OtherAssetsActivity
import com.example.personalwealthmanager.presentation.recipients.RecipientManagementActivity
import com.example.personalwealthmanager.presentation.settings.SettingsActivity
import com.example.personalwealthmanager.presentation.sources.SourceManagementActivity
import com.example.personalwealthmanager.presentation.stocks.StocksActivity
import com.example.personalwealthmanager.presentation.transactions.TransactionsActivity
import com.example.personalwealthmanager.presentation.zerodha.SetupZerodhaActivity
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

abstract class BaseDrawerActivity : AppCompatActivity() {

    protected lateinit var drawerLayout: DrawerLayout

    /**
     * Return the button ID (e.g. R.id.btnDashboard) that corresponds to the current screen.
     * That button will close the drawer without navigating.
     */
    protected open fun getSelfButtonId(): Int = -1

    protected fun setupDrawerMenu() {
        drawerLayout = findViewById(R.id.drawerLayout)

        findViewById<ImageView>(R.id.btnMenu)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        val headerView = navigationView.getHeaderView(0)

        // Push header below the status bar
        val statusBarResId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarPx = if (statusBarResId > 0) resources.getDimensionPixelSize(statusBarResId) else 0
        val density = resources.displayMetrics.density
        headerView.setPadding(
            (10 * density).toInt(),
            statusBarPx + (28 * density).toInt(),
            (10 * density).toInt(),
            (12 * density).toInt()
        )

        headerView.findViewById<TextView>(R.id.tvUserEmail)?.text = getUserEmail() ?: "User"

        // nav: close drawer and navigate unless this IS the current screen
        fun nav(id: Int, block: () -> Unit) {
            headerView.findViewById<Button>(id)?.setOnClickListener {
                drawerLayout.closeDrawer(GravityCompat.END)
                if (id != getSelfButtonId()) block()
            }
        }

        // expandable: toggle child container visibility
        fun expandable(btnId: Int, arrowId: Int, containerId: Int) {
            val btn = headerView.findViewById<Button>(btnId)
            val arrow = headerView.findViewById<ImageView>(arrowId)
            val container = headerView.findViewById<LinearLayout>(containerId)
            btn?.setOnClickListener {
                if (container?.visibility == View.VISIBLE) {
                    container.visibility = View.GONE
                    arrow?.setImageResource(R.drawable.ic_expand_more)
                } else {
                    container?.visibility = View.VISIBLE
                    arrow?.setImageResource(R.drawable.ic_expand_less)
                }
            }
            arrow?.setOnClickListener { btn?.performClick() }
        }

        nav(R.id.btnDashboard) { startActivity(Intent(this, MainActivity::class.java)) }
        nav(R.id.btnTransactions) { startActivity(Intent(this, TransactionsActivity::class.java)) }

        expandable(R.id.btnManagement, R.id.ivManagementExpand, R.id.managementChildItems)
        nav(R.id.btnCategoryManagement) { startActivity(Intent(this, CategoryManagementActivity::class.java)) }
        nav(R.id.btnSourceManagement) { startActivity(Intent(this, SourceManagementActivity::class.java)) }
        nav(R.id.btnRecipientManagement) { startActivity(Intent(this, RecipientManagementActivity::class.java)) }

        nav(R.id.btnNetWorth) { startActivity(Intent(this, NetWorthActivity::class.java)) }

        expandable(R.id.btnAssets, R.id.ivAssetsExpand, R.id.assetsChildItems)
        nav(R.id.btnStocks) { startActivity(Intent(this, StocksActivity::class.java)) }
        nav(R.id.btnMetals) { startActivity(Intent(this, MetalsActivity::class.java)) }
        nav(R.id.btnMutualFunds) { startActivity(Intent(this, MutualFundsActivity::class.java)) }
        nav(R.id.btnOtherAssets) { startActivity(Intent(this, OtherAssetsActivity::class.java)) }

        nav(R.id.btnLiabilities) { startActivity(Intent(this, LiabilitiesActivity::class.java)) }

        expandable(R.id.btnSetupDemat, R.id.ivSetupDematExpand, R.id.setupDematChildItems)
        nav(R.id.btnConnectZerodha) { startActivity(Intent(this, SetupZerodhaActivity::class.java)) }

        nav(R.id.btnSettings) { startActivity(Intent(this, SettingsActivity::class.java)) }

        headerView.findViewById<Button>(R.id.btnLogout)?.setOnClickListener {
            logout()
        }
    }

    override fun onBackPressed() {
        if (::drawerLayout.isInitialized && drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }

    // ── Session helpers (available to all subclasses) ──────────────────────────

    protected fun getUserEmail(): String? =
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("user_email", null)

    protected fun getSessionToken(): String? =
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("session_token", null)

    protected fun clearSession() {
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit {
            remove("session_token")
            remove("user_email")
        }
    }

    protected fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun logout() {
        val sessionToken = getSessionToken()
        if (sessionToken == null) {
            navigateToLogin()
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(ApiConfig.BASE_URL + ApiConfig.Endpoints.LOGOUT)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("X-Session-Token", sessionToken)
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.responseCode // trigger the request
                withContext(Dispatchers.Main) {
                    clearSession()
                    Toast.makeText(this@BaseDrawerActivity, getString(R.string.logout_success), Toast.LENGTH_SHORT).show()
                    navigateToLogin()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    clearSession()
                    navigateToLogin()
                }
            }
        }
    }
}
