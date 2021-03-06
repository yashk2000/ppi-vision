package org.mifos.visionppi.ui.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import com.google.android.material.navigation.NavigationView
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.nav_header_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.mifos.visionppi.R
import org.mifos.visionppi.adapters.ClientSearchAdapter
import org.mifos.visionppi.api.local.PreferencesHelper
import org.mifos.visionppi.databinding.ActivityMainBinding
import org.mifos.visionppi.objects.Client
import org.mifos.visionppi.ui.activities.LoginActivity
import org.mifos.visionppi.ui.client_profile.ClientProfileActivity
import org.mifos.visionppi.ui.user_profile.UserProfileActivity
import org.mifos.visionppi.utils.PrefManager


/**
 * Created by Apoorva M K on 25/06/19.
 */

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    MainMVPView {

    lateinit var binding: ActivityMainBinding
    lateinit var clientList: List<Client>
    var mMainPresenter: MainPresenter = MainPresenter()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(toolbar)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        client_search_list.layoutManager = LinearLayoutManager(this)

        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.title = getString(R.string.app_name)

        search_query.setOnEditorActionListener(TextView.OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                return@OnEditorActionListener true
            }
            false
        })

        search_query.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0.toString().trim().isEmpty()) {
                    search_btn.isEnabled = false
                    search_btn.alpha = 0.5F
                } else {
                    search_btn.isEnabled = true
                    search_btn.alpha = 1.0F
                }
            }
        })

        search_btn.setOnClickListener {
            performSearch()
        }

        val toggle: ActionBarDrawerToggle = object :
            ActionBarDrawerToggle(
                this,
                drawer_layout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
            ) {
            override
            fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                val inputMethodManager: InputMethodManager =
                    getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)

                profile_section.setOnClickListener {
                    val intent = Intent(applicationContext, UserProfileActivity::class.java)
                    startActivity(intent)
                }
            }
        }
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
    }

    private fun performSearch() {
        if (networkAvailable(this)) {
            search_query.onEditorAction(EditorInfo.IME_ACTION_DONE)
            if (search_query.text.toString().length == 0)
                searchError()
            else {
                val inputMethodManager =
                    getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(search_btn.windowToken, 0)
                search(search_query.text.toString())
            }
        } else {
            showToastMessage("Internet connection not available. Please check network settings")
        }
    }

    private fun networkAvailable(activity: AppCompatActivity): Boolean {
        val connectivityManager =
            activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    /*override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> return true
            else -> return super.onOptionsItemSelected(item)
        }
    }*/

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_profile -> {
                val intent = Intent(applicationContext, UserProfileActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_home -> {
                val intent = Intent(applicationContext, MainActivity::class.java)
                startActivity(intent)
            }
            R.id.share -> {
                val sendIntent = Intent()
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.putExtra(
                    Intent.EXTRA_TEXT,
                    getString(R.string.install_vision).toString() + "\n" + getString(R.string.download_link)
                )
                sendIntent.type = "text/plain"
                startActivity(sendIntent)
            }
            R.id.logout -> {
                val preferencesHelper = PreferencesHelper(baseContext)
                preferencesHelper.clear()
                val prefManager = PrefManager()
                prefManager.clear(this, baseContext)
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun searchError() {
        showToastMessage(getString(R.string.empty_query))
    }

    override fun showToastMessage(string: String) {
        Toast.makeText(this, string, Toast.LENGTH_SHORT).show()
    }

    override fun searchUnsuccessful() {
        showToastMessage(getString(R.string.search_unsuccessful))
    }

    override fun search(string: String) {
        val future = doAsync {

            clientList = doSearch(string)

            uiThread {
                makelist()
            }
        }
    }

    private fun onClick(item: Client) {

        val intent = Intent(applicationContext, ClientProfileActivity::class.java)
        intent.putExtra("client", item)
        startActivity(intent)

    }

    fun doSearch(string: String): List<Client> {
        return mMainPresenter.searchClients(string, applicationContext, this)
    }

    fun makelist() {
        if (clientList.size == 0)
            searchUnsuccessful()

        client_search_list.adapter =
                ClientSearchAdapter(clientList, this, { item -> onClick(item) })
    }
}
