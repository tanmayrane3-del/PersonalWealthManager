package com.example.personalwealthmanager.presentation.settings

import android.content.Intent
import android.os.Bundle
import androidx.cardview.widget.CardView
import com.example.personalwealthmanager.R
import com.example.personalwealthmanager.presentation.base.BaseDrawerActivity

class SettingsActivity : BaseDrawerActivity() {

    override fun getSelfButtonId() = R.id.btnSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setupDrawerMenu()
        setupBottomNav()

        findViewById<CardView>(R.id.cardSmsLogs).setOnClickListener {
            startActivity(Intent(this, SmsLogActivity::class.java))
        }
    }
}
