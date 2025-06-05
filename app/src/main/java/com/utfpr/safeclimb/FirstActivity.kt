package com.utfpr.safeclimb


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.utfpr.safeclimb.databinding.ActivityFirstBinding
import com.google.android.material.bottomnavigation.BottomNavigationView


class FirstActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFirstBinding

    companion object {
        private var loggedIn = false
        private var name = ""
        private var email = ""

        fun isLoggedIn(): Boolean {
            return loggedIn
        }

        fun getName(): String {
            return name
        }

        fun getEmail(): String {
            return email
        }

        fun setName(value: String) {
            name = value
        }

        fun setEmail(value: String) {
            email = value
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFirstBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        setSupportActionBar(findViewById(R.id.action_bar))

        getCred()

        val navController = findNavController(R.id.nav_host_fragment_activity_first)

        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_home -> {
                    navController.popBackStack(R.id.navigation_home, false);
                    navController.navigate(R.id.navigation_home, null)
                    true
                }
                R.id.action_start_climb -> {
                    navController.popBackStack(R.id.navigation_home, false);
                    navController.navigate(R.id.navigation_start_climb, null)
                    true
                }
                R.id.action_map -> {
                    navController.popBackStack(R.id.navigation_home, false);
                    navController.navigate(R.id.navigation_map, null)
                    true
                }
                R.id.action_faq -> {
                    navController.popBackStack(R.id.navigation_home, false);
                    navController.navigate(R.id.navigation_faq, null)
                    true
                }
                else -> {
                    super.onOptionsItemSelected(item)
                }
            }
        }

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_start_climb, R.id.navigation_map, R.id.navigation_faq
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        //navView.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_activity_first)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.action_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_user -> {
            showPopup()
            true
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun configMenu(popupMenu: Menu) {
        val username = popupMenu.findItem(R.id.action_username)
        val login = popupMenu.findItem(R.id.action_login)
        val logout = popupMenu.findItem(R.id.action_logout)
        val account = popupMenu.findItem(R.id.action_account)
        val history = popupMenu.findItem(R.id.mountain_history)
        val gallery = popupMenu.findItem(R.id.action_gallery)

        if(isLoggedIn())
        {
            username.title = name
            username.isVisible = true
            login.isVisible = false
            logout.isVisible = true
            account.isVisible = true
            history.isVisible = true
            gallery.isVisible = true
        }
        else
        {
            username.isVisible = false
            login.isVisible = true
            logout.isVisible = false
            account.isVisible = false
            history.isVisible = false
            gallery.isVisible = false
        }

    }

    private fun showPopup() {
        val popup = PopupMenu(this, findViewById<View>(R.id.action_user))
        val inflater = popup.getMenuInflater()
        inflater.inflate(R.menu.action_bar_options, popup.menu)
        configMenu(popup.menu)
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.mountain_history -> {
                    val navController = findNavController(R.id.nav_host_fragment_activity_first)
                    navController.navigate(R.id.navigation_mountain_history, null)
                    true
                }
                R.id.action_account -> {
                    val navController = findNavController(R.id.nav_host_fragment_activity_first)
                    navController.navigate(R.id.navigation_user_edit, null)
                    true
                }
                R.id.action_gallery -> {
                    val navController = findNavController(R.id.nav_host_fragment_activity_first)
                    navController.navigate(R.id.navigation_gallery, null)
                    true
                }
                R.id.action_login -> {
                    val intent = Intent(
                        this,
                        MainActivity::class.java
                    )
                    intent.putExtra("screen","auth");
                    startActivity(intent)
                    true
                }
                R.id.action_logout-> {
                    loggedIn = false
                    name = ""
                    email = ""
                    cleanCred()
                    val intent = Intent(
                        this,
                        MainActivity::class.java
                    )
                    intent.putExtra("screen","auth");
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    fun getCred() {
        try {
            val sharedPref = this.getSharedPreferences("preferences", Context.MODE_PRIVATE) ?: return
            loggedIn = sharedPref.getBoolean("user_logged", false)
            name = sharedPref.getString("user_name", "").toString()
            email = sharedPref.getString("user_email", "").toString()
        }catch (e: Exception){
            loggedIn = false
            name = ""
            email = ""
        }
    }

    fun cleanCred() {
        try {
            val sharedPref = this.getSharedPreferences("preferences", Context.MODE_PRIVATE) ?: return
            with (sharedPref.edit()) {
                putBoolean("user_logged", false)
                putString("user_name", "")
                putString("user_email", "")
                apply()
            }
        }
        catch (e: Exception)
        {
            return
        }
    }
}

