package com.example.personalwealthmanager.presentation.metals

import android.os.Bundle
import androidx.core.view.GravityCompat
import android.widget.ImageView
import com.example.personalwealthmanager.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MetalsActivity : com.example.personalwealthmanager.presentation.base.BaseDrawerActivity() {

    override fun getSelfButtonId() = R.id.btnMetals

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metals)

        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        // Load MetalsFragment
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.metals_fragment_container, MetalsFragment())
                .commit()
        }

        setupDrawerMenu()
    }
}
