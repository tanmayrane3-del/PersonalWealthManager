package com.example.personalwealthmanager.presentation.settings

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.personalwealthmanager.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<CardView>(R.id.cardSmsLogs).setOnClickListener {
            startActivity(Intent(this, SmsLogActivity::class.java))
        }
    }
}
