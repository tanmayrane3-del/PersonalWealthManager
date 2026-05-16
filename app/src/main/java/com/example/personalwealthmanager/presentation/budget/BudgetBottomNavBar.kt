package com.pwm.personalwealthmanager.presentation.budget

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwm.personalwealthmanager.MainActivity
import com.pwm.personalwealthmanager.R
import com.pwm.personalwealthmanager.presentation.networth.NetWorthActivity
import com.pwm.personalwealthmanager.presentation.transactions.TransactionsActivity

private val TealGreen  = Color(0xFF1A7C5C)
private val MutedColor = Color(0xFF9CA3AF)

enum class BudgetNavTab { DASHBOARD, TRANSACTIONS, NETWORTH, BUDGET }

@Composable
fun BudgetBottomNavBar(activeTab: BudgetNavTab = BudgetNavTab.BUDGET) {
    val context = LocalContext.current

    Column {
        HorizontalDivider(color = Color(0xFFE5E7EB))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .navigationBarsPadding()
                .height(60.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NavItem(
                iconRes = R.drawable.ic_nav_dashboard,
                label = "Dashboard",
                active = activeTab == BudgetNavTab.DASHBOARD,
                onClick = {
                    context.startActivity(
                        Intent(context, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    )
                }
            )
            NavItem(
                iconRes = R.drawable.ic_nav_transactions,
                label = "Transactions",
                active = activeTab == BudgetNavTab.TRANSACTIONS,
                onClick = {
                    context.startActivity(
                        Intent(context, TransactionsActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    )
                }
            )
            NavItem(
                iconRes = R.drawable.ic_nav_networth,
                label = "Net Worth",
                active = activeTab == BudgetNavTab.NETWORTH,
                onClick = {
                    context.startActivity(
                        Intent(context, NetWorthActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    )
                }
            )
            NavItem(
                iconRes = R.drawable.ic_nav_menu,
                label = "Budget",
                active = activeTab == BudgetNavTab.BUDGET,
                onClick = { /* already on budget */ }
            )
        }
    }
}

@Composable
private fun NavItem(iconRes: Int, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = if (active) TealGreen else MutedColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (active) TealGreen else MutedColor
        )
    }
}
