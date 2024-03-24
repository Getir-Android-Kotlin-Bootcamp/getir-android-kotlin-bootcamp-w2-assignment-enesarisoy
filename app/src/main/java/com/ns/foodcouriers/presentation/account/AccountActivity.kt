package com.ns.foodcouriers.presentation.account

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.ns.foodcouriers.presentation.account.adapter.PagerAdapter
import com.ns.foodcouriers.R

class AccountActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager2: ViewPager2
    private lateinit var pagerAdapter: PagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        tabLayout = findViewById(R.id.tabAccount)
        viewPager2 = findViewById(R.id.viewPagerAccount)
        pagerAdapter = PagerAdapter(supportFragmentManager,  lifecycle).apply {
            addFragment(CreateAccountFragment())
            addFragment(LoginFragment())
        }
        viewPager2.adapter = pagerAdapter

        TabLayoutMediator(tabLayout, viewPager2){ tab, position ->
            when (position) {
                0 -> tab.text = "Create Account"
                1 -> tab.text = "Login"
            }
        }.attach()
    }
}