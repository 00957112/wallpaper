package com.example.wallpaper
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout


class MainActivity : AppCompatActivity() {
    @SuppressLint("ClickableViewAccessibility")
    private lateinit var viewPager: ViewPager
    private lateinit var tabLayout: TabLayout
    private lateinit var adapter: TabAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Remove the code related to TabAdapter and ViewPager here
    }

    override fun onResume() {
        super.onResume()

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        adapter = TabAdapter(supportFragmentManager)
        viewPager.adapter = adapter

        // 添加选项卡到TabLayout
        for (i in 0 until adapter.count) {
            tabLayout.addTab(tabLayout.newTab().setText(adapter.getPageTitle(i)))
        }

        // 设置TabLayout与ViewPager关联
        tabLayout.setupWithViewPager(viewPager)
    }

    // 自定义PagerAdapter类
    class TabAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        private val fragmentList = listOf(HomeFragment(), ListViewFragment())
        private val fragmentTitles = listOf("首页", "列表视图")

        override fun getItem(position: Int): Fragment {
            return fragmentList[position]
        }

        override fun getCount(): Int {
            return fragmentList.size
        }

        override fun getPageTitle(position: Int): CharSequence {
            return fragmentTitles[position]
        }
    }
}
