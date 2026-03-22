package com.example.personalwealthmanager.presentation.mutualfunds

import android.os.Bundle
import androidx.core.view.GravityCompat
import android.widget.ImageView
import com.example.personalwealthmanager.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MutualFundsActivity : com.example.personalwealthmanager.presentation.base.BaseDrawerActivity() {

    override fun getSelfButtonId() = R.id.btnMutualFunds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mutual_funds)

        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.mf_fragment_container, MutualFundsFragment())
                .commit()
        }

        setupDrawerMenu()
    }

    override fun onResume() {
        super.onResume()
        // Fragment observes ViewModel; refresh happens via Fragment's fetchAll on first load.
        // On returning from CasImportActivity we need to re-fetch.
        supportFragmentManager.findFragmentById(R.id.mf_fragment_container)?.let {
            if (it is MutualFundsFragment) {
                // ViewModel is shared via activityViewModels — refresh holdings
                val vm = androidx.lifecycle.ViewModelProvider(this)[MutualFundsViewModel::class.java]
                vm.fetchAll()
            }
        }
    }
}
