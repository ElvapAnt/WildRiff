package com.rmas.wildriff

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationBarView.LABEL_VISIBILITY_LABELED
import com.rmas.wildriff.databinding.ActivityMainBinding
import com.rmas.wildriff.model.SharedViewModel

class MainActivity :AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var userId : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        userId = intent.getStringExtra("USER_ID")!!
        sharedViewModel.setUserID(userId)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setBackgroundColor(Color.TRANSPARENT)
        setSupportActionBar(binding.toolbar)

        supportActionBar?.apply {
            title = null
            subtitle = null
        }

        binding.appBarLayout.isVisible = false

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        binding.bottomNavigationView.setupWithNavController(navController)
        binding.bottomNavigationView.labelVisibilityMode = LABEL_VISIBILITY_LABELED

        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)


        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_home -> {
                    navigateToFragment(R.id.HomeFragment)
                    true
                }
                R.id.menu_play -> {
                    navigateToFragment(R.id.PlayFragment)
                    true
                }
                R.id.menu_profile -> {
                    navigateToFragment(R.id.ProfileFragment)
                    true
                }
                R.id.menu_leaderboard -> {
                    navigateToFragment(R.id.LeaderboardFragment)
                    true
                }
                else -> false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    private fun navigateToFragment(fragmentId: Int) {
        val bundle = Bundle()
        bundle.putString("USER_ID", userId)

        findNavController(R.id.nav_host_fragment_content_main)
            .navigate(fragmentId, bundle)
    }
}