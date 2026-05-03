package com.pwm.personalwealthmanager.presentation.liabilities

import android.content.Intent
import android.os.Bundle
import androidx.core.view.GravityCompat
import android.widget.ImageView
import com.pwm.personalwealthmanager.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LiabilitiesActivity : com.pwm.personalwealthmanager.presentation.base.BaseDrawerActivity() {

    override fun getActiveNavItem() = BottomNavItem.NETWORTH


    override fun getSelfButtonId() = R.id.btnLiabilities

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_liabilities)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        findViewById<FloatingActionButton>(R.id.fabAddLiability).setOnClickListener {
            startActivity(Intent(this, AddEditLiabilityActivity::class.java))
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.liabilities_fragment_container, LiabilitiesFragment())
                .commit()
        }

        setupDrawerMenu()
        setupBottomNav()
    }
}
