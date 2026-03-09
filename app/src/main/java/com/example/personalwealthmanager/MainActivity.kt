package com.example.personalwealthmanager

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.personalwealthmanager.presentation.stocks.StocksActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.personalwealthmanager.ApiConfig
import com.example.personalwealthmanager.presentation.categories.CategoryManagementActivity
import com.example.personalwealthmanager.presentation.sources.SourceManagementActivity
import com.example.personalwealthmanager.presentation.recipients.RecipientManagementActivity
import com.example.personalwealthmanager.presentation.zerodha.SetupZerodhaActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import com.example.personalwealthmanager.presentation.transactions.TransactionsActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_SMS_PERMISSION = 1001
        private const val REQUEST_CODE_NOTIFICATION_PERMISSION = 1002
    }

    private lateinit var tvFromDate: TextView
    private lateinit var tvToDate: TextView
    private lateinit var tvIncomeAmount: TextView
    private lateinit var tvExpensesAmount: TextView
    private lateinit var ivRefreshIncome: ImageView
    private lateinit var ivRefreshExpenses: ImageView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var tvStockValue: TextView
    private lateinit var tvStockDayPnl: TextView
    private lateinit var ivRefreshStocks: ImageView

    private val fromCalendar = Calendar.getInstance()
    private val toCalendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        tvFromDate = findViewById(R.id.tvFromDate)
        tvToDate = findViewById(R.id.tvToDate)
        tvIncomeAmount = findViewById(R.id.tvIncomeAmount)
        tvExpensesAmount = findViewById(R.id.tvExpensesAmount)
        ivRefreshIncome = findViewById(R.id.ivRefreshIncome)
        ivRefreshExpenses = findViewById(R.id.ivRefreshExpenses)
        drawerLayout = findViewById(R.id.drawerLayout)
        tvStockValue = findViewById(R.id.tvStockValue)
        tvStockDayPnl = findViewById(R.id.tvStockDayPnl)
        ivRefreshStocks = findViewById(R.id.ivRefreshStocks)

        val fromDateContainer = findViewById<View>(R.id.fromDateContainer)
        val toDateContainer = findViewById<View>(R.id.toDateContainer)
        val btnMenu = findViewById<View>(R.id.btnMenu)

        // Set default dates (today - 30 days to today)
        toCalendar.time = Date() // Today
        fromCalendar.time = Date()
        fromCalendar.add(Calendar.DAY_OF_MONTH, -30)

        // Display initial dates
        updateDateDisplay()

        // Load initial data
        refreshData()

        // Set click listeners for From Date
        fromDateContainer.setOnClickListener {
            showDatePickerDialog(true)
        }

        // Set click listeners for To Date
        toDateContainer.setOnClickListener {
            showDatePickerDialog(false)
        }

        // Burger menu click
        btnMenu.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        // Refresh button click listeners
        ivRefreshIncome.setOnClickListener {
            refreshIncome()
        }

        ivRefreshExpenses.setOnClickListener {
            refreshExpenses()
        }

        ivRefreshStocks.setOnClickListener {
            refreshStocks()
        }

        findViewById<View>(R.id.stocksCard).setOnClickListener {
            startActivity(Intent(this, StocksActivity::class.java))
        }

        // Setup drawer menu
        setupDrawerMenu()

        // Request SMS permissions at runtime
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
                REQUEST_CODE_SMS_PERMISSION
            )
        }

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATION_PERMISSION
                )
            }
        }
    }

    private fun refreshData() {
        refreshIncome()
        refreshExpenses()
        refreshStocks()
    }

    private fun refreshIncome() {
        val sessionToken = getSessionToken()
        if (sessionToken == null) {
            Toast.makeText(this, getString(R.string.session_expired), Toast.LENGTH_SHORT).show()
            navigateToLogin()
            return
        }

        // Show loading state
        tvIncomeAmount.text = getString(R.string.loading)
        ivRefreshIncome.isEnabled = false
        ivRefreshIncome.alpha = 0.5f

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val startDate = apiDateFormat.format(fromCalendar.time)
                val endDate = apiDateFormat.format(toCalendar.time)

                val urlString = "${ApiConfig.BASE_URL}${ApiConfig.Endpoints.GET_INCOME_SUMMARY}?start_date=$startDate&end_date=$endDate"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty(ApiConfig.HEADER_SESSION_TOKEN, sessionToken)
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    connection.errorStream?.bufferedReader()?.readText() ?: ""
                }

                withContext(Dispatchers.Main) {
                    ivRefreshIncome.isEnabled = true
                    ivRefreshIncome.alpha = 1.0f

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val jsonResponse = JSONObject(response)
                        if (jsonResponse.getString("status") == "success") {
                            val dataArray = jsonResponse.getJSONArray("data")

                            if (dataArray.length() > 0) {
                                val dataObject = dataArray.getJSONObject(0)
                                val totalIncome = dataObject.getString("total_income").toDoubleOrNull() ?: 0.0
                                tvIncomeAmount.text = formatCurrency(totalIncome)
                            } else {
                                tvIncomeAmount.text = formatCurrency(0.0)
                            }
                        } else {
                            tvIncomeAmount.text = getString(R.string.no_data)
                        }
                    } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        Toast.makeText(this@MainActivity, getString(R.string.session_expired), Toast.LENGTH_SHORT).show()
                        navigateToLogin()
                    } else {
                        tvIncomeAmount.text = getString(R.string.default_income)
                        Toast.makeText(this@MainActivity, getString(R.string.refresh_failed), Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ivRefreshIncome.isEnabled = true
                    ivRefreshIncome.alpha = 1.0f
                    tvIncomeAmount.text = getString(R.string.default_income)
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.network_error, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun refreshExpenses() {
        val sessionToken = getSessionToken()
        if (sessionToken == null) {
            Toast.makeText(this, getString(R.string.session_expired), Toast.LENGTH_SHORT).show()
            navigateToLogin()
            return
        }

        // Show loading state
        tvExpensesAmount.text = getString(R.string.loading)
        ivRefreshExpenses.isEnabled = false
        ivRefreshExpenses.alpha = 0.5f

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val startDate = apiDateFormat.format(fromCalendar.time)
                val endDate = apiDateFormat.format(toCalendar.time)

                val urlString = "${ApiConfig.BASE_URL}${ApiConfig.Endpoints.GET_EXPENSE_SUMMARY}?start_date=$startDate&end_date=$endDate"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty(ApiConfig.HEADER_SESSION_TOKEN, sessionToken)
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                val response = if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream.bufferedReader().readText()
                } else {
                    connection.errorStream?.bufferedReader()?.readText() ?: ""
                }

                withContext(Dispatchers.Main) {
                    ivRefreshExpenses.isEnabled = true
                    ivRefreshExpenses.alpha = 1.0f

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val jsonResponse = JSONObject(response)
                        if (jsonResponse.getString("status") == "success") {
                            val dataArray = jsonResponse.getJSONArray("data")

                            if (dataArray.length() > 0) {
                                val dataObject = dataArray.getJSONObject(0)
                                val totalExpenses = dataObject.getString("total_expenses").toDoubleOrNull() ?: 0.0
                                tvExpensesAmount.text = formatCurrency(totalExpenses)
                            } else {
                                tvExpensesAmount.text = formatCurrency(0.0)
                            }
                        } else {
                            tvExpensesAmount.text = getString(R.string.no_data)
                        }
                    } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        Toast.makeText(this@MainActivity, getString(R.string.session_expired), Toast.LENGTH_SHORT).show()
                        navigateToLogin()
                    } else {
                        tvExpensesAmount.text = getString(R.string.default_expenses)
                        Toast.makeText(this@MainActivity, getString(R.string.refresh_failed), Toast.LENGTH_SHORT).show()
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ivRefreshExpenses.isEnabled = true
                    ivRefreshExpenses.alpha = 1.0f
                    tvExpensesAmount.text = getString(R.string.default_expenses)
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.network_error, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun refreshStocks() {
        val sessionToken = getSessionToken() ?: return
        tvStockValue.text = getString(R.string.loading)
        tvStockDayPnl.text = getString(R.string.loading)
        ivRefreshStocks.isEnabled = false
        ivRefreshStocks.alpha = 0.5f

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("${ApiConfig.BASE_URL}/api/holdings/summary")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty(ApiConfig.HEADER_SESSION_TOKEN, sessionToken)
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                val response = if (responseCode == HttpURLConnection.HTTP_OK)
                    connection.inputStream.bufferedReader().readText()
                else
                    connection.errorStream?.bufferedReader()?.readText() ?: ""

                withContext(Dispatchers.Main) {
                    ivRefreshStocks.isEnabled = true
                    ivRefreshStocks.alpha = 1.0f

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val json = JSONObject(response)
                        if (json.getString("status") == "success") {
                            val data = json.getJSONObject("data")
                            val totalValue = data.getDouble("total_portfolio_value")
                            val todayPnl = data.getDouble("today_pnl")

                            tvStockValue.text = formatCurrency(totalValue)
                            val prefix = if (todayPnl >= 0) "+" else ""
                            tvStockDayPnl.text = "$prefix${formatCurrency(todayPnl)}"
                            tvStockDayPnl.setTextColor(ContextCompat.getColor(
                                this@MainActivity,
                                if (todayPnl >= 0) R.color.income_green else R.color.expense_red
                            ))
                        } else {
                            tvStockValue.text = getString(R.string.stocks_not_connected)
                            tvStockDayPnl.text = getString(R.string.stocks_not_connected)
                        }
                    } else {
                        tvStockValue.text = getString(R.string.stocks_not_connected)
                        tvStockDayPnl.text = getString(R.string.stocks_not_connected)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ivRefreshStocks.isEnabled = true
                    ivRefreshStocks.alpha = 1.0f
                    tvStockValue.text = getString(R.string.stocks_not_connected)
                    tvStockDayPnl.text = getString(R.string.stocks_not_connected)
                }
            }
        }
    }

    private fun formatCurrency(amount: Double): String {
        return currencyFormat.format(amount)
    }

    private fun setupDrawerMenu() {
        val navigationView = findViewById<com.google.android.material.navigation.NavigationView>(R.id.navigationView)
        val headerView = navigationView.getHeaderView(0)

        // Display user email
        val tvUserEmail = headerView.findViewById<TextView>(R.id.tvUserEmail)
        val userEmail = getUserEmail()
        tvUserEmail.text = userEmail ?: "User"

        // Dashboard button - already on this screen
        val btnDashboard = headerView.findViewById<Button>(R.id.btnDashboard)
        btnDashboard?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            // Already on dashboard screen
        }

        // Transactions button
        val btnTransactions = headerView.findViewById<Button>(R.id.btnTransactions)
        btnTransactions.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, TransactionsActivity::class.java))
        }

        // Management expandable menu
        val btnManagement = headerView.findViewById<Button>(R.id.btnManagement)
        val ivManagementExpand = headerView.findViewById<ImageView>(R.id.ivManagementExpand)
        val managementChildItems = headerView.findViewById<LinearLayout>(R.id.managementChildItems)

        btnManagement?.setOnClickListener {
            if (managementChildItems?.visibility == View.VISIBLE) {
                managementChildItems.visibility = View.GONE
                ivManagementExpand?.setImageResource(R.drawable.ic_expand_more)
            } else {
                managementChildItems?.visibility = View.VISIBLE
                ivManagementExpand?.setImageResource(R.drawable.ic_expand_less)
            }
        }

        ivManagementExpand?.setOnClickListener {
            btnManagement?.performClick()
        }

        // Category management button
        headerView.findViewById<Button>(R.id.btnCategoryManagement)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, CategoryManagementActivity::class.java))
        }

        // Source management button
        headerView.findViewById<Button>(R.id.btnSourceManagement)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, SourceManagementActivity::class.java))
        }

        // Recipient management button
        headerView.findViewById<Button>(R.id.btnRecipientManagement)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, RecipientManagementActivity::class.java))
        }

        // Assets expandable menu
        val btnAssets = headerView.findViewById<Button>(R.id.btnAssets)
        val ivAssetsExpand = headerView.findViewById<ImageView>(R.id.ivAssetsExpand)
        val assetsChildItems = headerView.findViewById<LinearLayout>(R.id.assetsChildItems)

        btnAssets?.setOnClickListener {
            if (assetsChildItems?.visibility == View.VISIBLE) {
                assetsChildItems.visibility = View.GONE
                ivAssetsExpand?.setImageResource(R.drawable.ic_expand_more)
            } else {
                assetsChildItems?.visibility = View.VISIBLE
                ivAssetsExpand?.setImageResource(R.drawable.ic_expand_less)
            }
        }
        ivAssetsExpand?.setOnClickListener { btnAssets?.performClick() }

        headerView.findViewById<Button>(R.id.btnStocks)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, StocksActivity::class.java))
        }

        // Setup Demat expandable menu
        val btnSetupDemat = headerView.findViewById<Button>(R.id.btnSetupDemat)
        val ivSetupDematExpand = headerView.findViewById<ImageView>(R.id.ivSetupDematExpand)
        val setupDematChildItems = headerView.findViewById<LinearLayout>(R.id.setupDematChildItems)

        btnSetupDemat?.setOnClickListener {
            if (setupDematChildItems?.visibility == View.VISIBLE) {
                setupDematChildItems.visibility = View.GONE
                ivSetupDematExpand?.setImageResource(R.drawable.ic_expand_more)
            } else {
                setupDematChildItems?.visibility = View.VISIBLE
                ivSetupDematExpand?.setImageResource(R.drawable.ic_expand_less)
            }
        }
        ivSetupDematExpand?.setOnClickListener { btnSetupDemat?.performClick() }

        headerView.findViewById<Button>(R.id.btnConnectZerodha)?.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            startActivity(Intent(this, SetupZerodhaActivity::class.java))
        }

        // Logout button
        val btnLogout = headerView.findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            logout()
        }
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

                val responseCode = connection.responseCode

                withContext(Dispatchers.Main) {
                    clearSession()
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.logout_success),
                        Toast.LENGTH_SHORT
                    ).show()
                    navigateToLogin()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Even if logout API fails, clear local session
                    clearSession()
                    navigateToLogin()
                }
            }
        }
    }

    private fun clearSession() {
        getSharedPreferences("AppPrefs", Context.MODE_PRIVATE).edit {
            remove("session_token")
            remove("user_email")
        }
    }

    private fun getSessionToken(): String? {
        return getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("session_token", null)
    }

    private fun getUserEmail(): String? {
        return getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            .getString("user_email", null)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showDatePickerDialog(isFromDate: Boolean) {
        val calendar = if (isFromDate) fromCalendar else toCalendar

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                if (isFromDate) {
                    fromCalendar.set(selectedYear, selectedMonth, selectedDay)

                    // If From date is now after To date, update To date to match From date
                    if (fromCalendar.after(toCalendar)) {
                        toCalendar.time = fromCalendar.time
                    }
                } else {
                    toCalendar.set(selectedYear, selectedMonth, selectedDay)

                    // If To date is now before From date, update From date to match To date
                    if (toCalendar.before(fromCalendar)) {
                        fromCalendar.time = toCalendar.time
                    }
                }

                updateDateDisplay()
                // Refresh data when date changes
                refreshData()
            },
            year,
            month,
            day
        )

        // Set date constraints
        if (isFromDate) {
            datePickerDialog.datePicker.maxDate = toCalendar.timeInMillis
        } else {
            datePickerDialog.datePicker.minDate = fromCalendar.timeInMillis
        }

        datePickerDialog.show()
    }

    private fun updateDateDisplay() {
        tvFromDate.text = dateFormat.format(fromCalendar.time)
        tvToDate.text = dateFormat.format(toCalendar.time)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}