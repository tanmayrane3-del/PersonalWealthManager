package com.example.personalwealthmanager.presentation.otherassets

import android.os.Bundle
import androidx.core.view.GravityCompat
import android.widget.ImageView
import com.example.personalwealthmanager.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class OtherAssetsActivity : com.example.personalwealthmanager.presentation.base.BaseDrawerActivity() {

    override fun getActiveNavItem() = BottomNavItem.NETWORTH


    override fun getSelfButtonId() = R.id.btnOtherAssets

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other_assets)

        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.other_assets_fragment_container, OtherAssetsFragment())
                .commit()
        }

        setupDrawerMenu()
        setupBottomNav()
    }
}
