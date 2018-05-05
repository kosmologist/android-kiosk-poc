package io.github.kosmologist.kioskappsample

import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothClass
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast

open abstract class BaseActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var devicePolicyManager:DevicePolicyManager
    private lateinit var adminComponentName:ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        packageManager.setComponentEnabledSetting(ComponentName(this.applicationContext,
                getActivityClass()),PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP)


    }

    override fun setContentView(layoutResID: Int) {
        val view = layoutInflater.inflate(layoutResID, null)
        configureToolbar(view)
        super.setContentView(view)

    }

    protected fun useToolbar():Boolean = true
    protected abstract fun getActivityClass():Class<*>

    private fun configureToolbar(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        if (useToolbar()){
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(true)
        }else{
            toolbar.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.base_menu,menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        when (Prefs.isKioskModeEnabled(this)){
            true -> menu?.findItem(R.id.menu_toggle_kioskmode)?.setTitle("Disable Kiosk Mode")
            false -> menu?.findItem(R.id.menu_toggle_kioskmode)?.setTitle("Enable Kiosk Mode")
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId){
            R.id.menu_toggle_kioskmode -> {
                Prefs.toggleKioskMode(this)
                Toast.makeText(this,"Toggled Kiosk Mode", Toast.LENGTH_LONG).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

}