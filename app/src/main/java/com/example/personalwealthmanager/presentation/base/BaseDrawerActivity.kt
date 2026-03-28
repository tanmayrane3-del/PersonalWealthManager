package com.example.personalwealthmanager.presentation.base

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

    enum class BottomNavItem { DASHBOARD, TRANSACTIONS, NETWORTH, NONE }

    /** Override to highlight the correct bottom nav tab for this screen. */
    protected open fun getActiveNavItem(): BottomNavItem = BottomNavItem.NONE

    /**
     * Return the button ID (e.g. R.id.btnDashboard) that corresponds to the current screen.
     * That button will close the drawer without navigating.
     */
    protected open fun getSelfButtonId(): Int = -1

    protected fun setupBottomNav() {
        val active = getActiveNavItem()
        val mutedColor = ContextCompat.getColor(this, R.color.text_muted)

        fun styleItem(layout: LinearLayout?, isActive: Boolean) {
            layout ?: return
            if (isActive) {
                layout.setBackgroundResource(R.drawable.bg_bottom_nav_active)
            } else {
                layout.setBackgroundColor(Color.TRANSPARENT)
                layout.background = null
            }
            for (i in 0 until layout.childCount) {
                when (val child = layout.getChildAt(i)) {
                    is ImageView -> if (isActive) child.setColorFilter(Color.WHITE)
                                    else child.setColorFilter(mutedColor)
                    is TextView  -> child.setTextColor(if (isActive) Color.WHITE else mutedColor)
                }
            }
        }

        val navDashboard    = findViewById<LinearLayout>(R.id.navItemDashboard)
        val navTransactions = findViewById<LinearLayout>(R.id.navItemTransactions)
        val navNetworth     = findViewById<LinearLayout>(R.id.navItemNetworth)
        val navMenu         = findViewById<LinearLayout>(R.id.navItemMenu)

        styleItem(navDashboard,    active == BottomNavItem.DASHBOARD)
        styleItem(navTransactions, active == BottomNavItem.TRANSACTIONS)
        styleItem(navNetworth,     active == BottomNavItem.NETWORTH)
        styleItem(navMenu,         false)

        navDashboard?.setOnClickListener {
            if (active != BottomNavItem.DASHBOARD)
                startActivity(Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
        navTransactions?.setOnClickListener {
            if (active != BottomNavItem.TRANSACTIONS)
                startActivity(Intent(this, TransactionsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
        navNetworth?.setOnClickListener {
            if (active != BottomNavItem.NETWORTH)
                startActivity(Intent(this, NetWorthActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
        }
        navMenu?.setOnClickListener {
            if (::drawerLayout.isInitialized) drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    protected fun setupDrawerMenu() {
        drawerLayout = findViewById(R.id.drawerLayout)
        setupGlassEffect()

        findViewById<ImageView>(R.id.btnMenu)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        val navigationView = findViewById<NavigationView>(R.id.navigationView)
        // Force semi-transparent glass background — Material may override android:background in XML
        navigationView.background = ColorDrawable(Color.argb(204, 255, 255, 255)) // 80% white
        val headerView = navigationView.getHeaderView(0)

        // Push header below the status bar
        val statusBarResId = resources.getIdentifier("status_bar_height", "dimen", "android")
        val statusBarPx = if (statusBarResId > 0) resources.getDimensionPixelSize(statusBarResId) else 0
        val density = resources.displayMetrics.density
        headerView.setPadding(0, statusBarPx, 0, 0)

        // Populate profile
        val email = getUserEmail() ?: ""
        headerView.findViewById<TextView>(R.id.tvUserEmail)?.text = email
        val name = getUserName() ?: email.substringBefore("@").replaceFirstChar { it.uppercase() }
        headerView.findViewById<TextView>(R.id.tvUserName)?.text = name
        headerView.findViewById<TextView>(R.id.tvUserInitials)?.text =
            name.firstOrNull()?.uppercase() ?: "P"

        // nav: close drawer and navigate unless this IS the current screen
        fun nav(id: Int, block: () -> Unit) {
            headerView.findViewById<View>(id)?.setOnClickListener {
                drawerLayout.closeDrawer(GravityCompat.END)
                if (id != getSelfButtonId()) block()
            }
        }

        // expandable: toggle child container visibility
        fun expandable(rowId: Int, arrowId: Int, containerId: Int) {
            val row = headerView.findViewById<View>(rowId)
            val arrow = headerView.findViewById<ImageView>(arrowId)
            val container = headerView.findViewById<LinearLayout>(containerId)
            row?.setOnClickListener {
                if (container?.visibility == View.VISIBLE) {
                    container.visibility = View.GONE
                    arrow?.setImageResource(R.drawable.ic_expand_more)
                } else {
                    container?.visibility = View.VISIBLE
                    arrow?.setImageResource(R.drawable.ic_expand_less)
                }
            }
        }

        nav(R.id.btnDashboard)          { startActivity(Intent(this, MainActivity::class.java)) }
        nav(R.id.btnTransactions)       { startActivity(Intent(this, TransactionsActivity::class.java)) }
        nav(R.id.btnCategoryManagement) { startActivity(Intent(this, CategoryManagementActivity::class.java)) }
        nav(R.id.btnSourceManagement)   { startActivity(Intent(this, SourceManagementActivity::class.java)) }
        nav(R.id.btnRecipientManagement){ startActivity(Intent(this, RecipientManagementActivity::class.java)) }

        nav(R.id.btnNetWorth) { startActivity(Intent(this, NetWorthActivity::class.java)) }

        expandable(R.id.btnAssets, R.id.ivAssetsExpand, R.id.assetsChildItems)
        nav(R.id.btnStocks)      { startActivity(Intent(this, StocksActivity::class.java)) }
        nav(R.id.btnMetals)      { startActivity(Intent(this, MetalsActivity::class.java)) }
        nav(R.id.btnMutualFunds) { startActivity(Intent(this, MutualFundsActivity::class.java)) }
        nav(R.id.btnOtherAssets) { startActivity(Intent(this, OtherAssetsActivity::class.java)) }

        nav(R.id.btnLiabilities)    { startActivity(Intent(this, LiabilitiesActivity::class.java)) }
        nav(R.id.btnConnectZerodha) { startActivity(Intent(this, SetupZerodhaActivity::class.java)) }
        nav(R.id.btnSettings)       { startActivity(Intent(this, SettingsActivity::class.java)) }

        headerView.findViewById<View>(R.id.btnLogout)?.setOnClickListener { logout() }

        // Highlight active drawer item
        highlightActiveDrawerItem(headerView)
    }

    private fun setupGlassEffect() {
        // Lighter scrim so background content shows through the semi-transparent drawer
        drawerLayout.setScrimColor(Color.argb(100, 0, 0, 0))

        // API 31+: blur the main content as the drawer slides in for a frosted-glass look
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val mainContent = drawerLayout.getChildAt(0) ?: return
            drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                    val radius = slideOffset * 20f
                    mainContent.setRenderEffect(
                        if (radius > 0.5f)
                            RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
                        else null
                    )
                }
                override fun onDrawerClosed(drawerView: View) {
                    mainContent.setRenderEffect(null)
                }
                override fun onDrawerOpened(drawerView: View) {}
                override fun onDrawerStateChanged(newState: Int) {}
            })
        }
    }

    private fun highlightActiveDrawerItem(headerView: View) {
        val activeId = getSelfButtonId()
        if (activeId < 0) return

        val assetChildIds = setOf(
            R.id.btnStocks, R.id.btnMetals, R.id.btnMutualFunds, R.id.btnOtherAssets
        )

        // If active is inside assets, expand that group
        if (activeId in assetChildIds) {
            headerView.findViewById<LinearLayout>(R.id.assetsChildItems)?.visibility = View.VISIBLE
            headerView.findViewById<ImageView>(R.id.ivAssetsExpand)
                ?.setImageResource(R.drawable.ic_expand_less)
        }

        val row = headerView.findViewById<View>(activeId) ?: return
        row.setBackgroundResource(R.drawable.drawer_item_active_bg)
        if (row is LinearLayout) {
            for (i in 0 until row.childCount) {
                when (val child = row.getChildAt(i)) {
                    is ImageView -> child.setColorFilter(Color.WHITE)
                    is TextView  -> child.setTextColor(Color.WHITE)
                }
            }
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

    protected fun getUserName(): String? =
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("user_name", null)

    protected fun getSessionToken(): String? =
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).getString("session_token", null)

    protected fun clearSession() {
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit {
            remove("session_token")
            remove("user_email")
            remove("user_name")
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
