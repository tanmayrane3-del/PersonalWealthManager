package com.example.personalwealthmanager.presentation.liabilities

import android.os.Bundle
import androidx.core.view.GravityCompat
import android.widget.ImageView
import com.example.personalwealthmanager.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LiabilitiesActivity : com.example.personalwealthmanager.presentation.base.BaseDrawerActivity() {

    override fun getActiveNavItem() = BottomNavItem.NETWORTH


    override fun getSelfButtonId() = R.id.btnLiabilities

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_liabilities)

        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
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
