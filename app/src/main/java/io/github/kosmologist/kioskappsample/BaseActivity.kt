package io.github.kosmologist.kioskappsample

import android.app.admin.DevicePolicyManager
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
import android.app.ActivityManager
import android.os.Build
import android.os.BatteryManager
import android.content.Intent
import android.content.IntentFilter
import android.app.admin.SystemUpdatePolicy
import android.os.UserManager
import android.provider.Settings
import android.util.Log


open abstract class BaseActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var adminComponentName: ComponentName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adminComponentName = DeviceAdministratorReceiver.getComponentName(this)
        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        packageManager.setComponentEnabledSetting(ComponentName(this.applicationContext,
                getActivityClass()), PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP)
    }

    override fun onStart() {
        super.onStart()
        if (Prefs.isKioskModeEnabled(this) &&
                devicePolicyManager.isDeviceOwnerApp(packageName)) {
            lockTask()
        }
    }

    override fun setContentView(layoutResID: Int) {
        val view = layoutInflater.inflate(layoutResID, null)
        configureToolbar(view)
        super.setContentView(view)

    }

    protected fun useToolbar(): Boolean = true
    protected abstract fun getActivityClass(): Class<*>

    private fun configureToolbar(view: View) {
        toolbar = view.findViewById(R.id.toolbar)
        if (useToolbar()) {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(true)
        } else {
            toolbar.visibility = View.GONE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.base_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        when (Prefs.isKioskModeEnabled(this)) {
            true -> menu?.findItem(R.id.menu_toggle_kioskmode)?.setTitle("Disable Kiosk Mode")
            false -> menu?.findItem(R.id.menu_toggle_kioskmode)?.setTitle("Enable Kiosk Mode")
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_toggle_kioskmode -> {
                onToggleKioskMode()
            }
            R.id.menu_remove_deviceOwnership -> {
                onRemoveDeviceOwnership()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun onToggleKioskMode() {
        try {
            if (devicePolicyManager.isDeviceOwnerApp(packageName)) {
                Prefs.toggleKioskMode(this)
                if (Prefs.isKioskModeEnabled(this)) {
                    setDefaultCosuPolicies(true)
                    lockTask()
                    Toast.makeText(this, "Kiosk Enabled", Toast.LENGTH_LONG).show()
                } else {
                    unlockTask()
                    setDefaultCosuPolicies(false)
                    Toast.makeText(this, "Kiosk Disabled", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Current App is not set to be device owner," +
                        "thus cannot enable/disable Kiosk Mode", Toast.LENGTH_SHORT).show()
            }
        } catch (exception: Exception) {
            Log.e("KIOSK", exception.toString())
            Toast.makeText(this, "Current App is not set to be device owner," +
                    "thus cannot enable/disable Kiosk Mode", Toast.LENGTH_SHORT).show()
        }
    }

    private fun lockTask() {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (!activityManager.isInLockTaskMode) {
                startLockTask()
                Toast.makeText(this, "Started lock Task", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (activityManager.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) {
                startLockTask()
                Log.d("KIOSK", "Started lock Task")
            }
        }
    }

    private fun unlockTask() {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (activityManager.isInLockTaskMode) {
                stopLockTask()
                Log.d("KIOSK", "Stopped lock Task")
            }
        } else {
            if (activityManager.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_LOCKED) {
                stopLockTask()
                Log.d("KIOSK", "Stopped lock Task")
            }
        }
    }

    private fun setDefaultCosuPolicies(active: Boolean) {
        setUserRestriction(UserManager.DISALLOW_SAFE_BOOT, active)
        setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, active)
        setUserRestriction(UserManager.DISALLOW_ADD_USER, active)
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, active)
        setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, active)

        devicePolicyManager.setKeyguardDisabled(adminComponentName, active)
        devicePolicyManager.setStatusBarDisabled(adminComponentName, active)

        enableStayOnWhilePluggedIn(active)

        if (active) {
            devicePolicyManager.setSystemUpdatePolicy(adminComponentName, SystemUpdatePolicy.createWindowedInstallPolicy(60, 120))
        } else {
            devicePolicyManager.setSystemUpdatePolicy(adminComponentName, null)
        }

        devicePolicyManager.setLockTaskPackages(adminComponentName, if (active) arrayOf(packageName) else arrayOf())

        val intentFilter = IntentFilter(Intent.ACTION_MAIN)
        intentFilter.addCategory(Intent.CATEGORY_HOME)
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT)

        if (active) {
            devicePolicyManager.addPersistentPreferredActivity(adminComponentName,
                    intentFilter,
                    ComponentName(packageName, MainActivity::class.java!!.getName()))
        } else {
            devicePolicyManager.clearPackagePersistentPreferredActivities(adminComponentName, packageName)
        }
        Log.d("KIOS", "Setup Default Policies " + active)
    }

    private fun setUserRestriction(restriction: String, disallow: Boolean) {
        if (disallow) {
            devicePolicyManager.addUserRestriction(adminComponentName, packageName)
        } else {
            devicePolicyManager.clearUserRestriction(adminComponentName, restriction)
        }
    }

    private fun enableStayOnWhilePluggedIn(enabled: Boolean) {
        if (enabled) {
            devicePolicyManager.setGlobalSetting(
                    adminComponentName,
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    Integer.toString(BatteryManager.BATTERY_PLUGGED_AC or
                            BatteryManager.BATTERY_PLUGGED_USB or
                            BatteryManager.BATTERY_PLUGGED_WIRELESS))
        } else {
            devicePolicyManager.setGlobalSetting(adminComponentName, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, "0")
        }
    }

    private fun onRemoveDeviceOwnership() {
        // Only device owner can remove itself and also only programatically only.
        // Only other option is to Factory-Reset device.
        try {
            devicePolicyManager.clearDeviceOwnerApp(packageName)
            Toast.makeText(this, "Removed App as Device Owner", Toast.LENGTH_LONG).show()
        } catch (ex: Exception) {
            Toast.makeText(this, "ERR: Can only be called by device owner", Toast.LENGTH_LONG).show()
            Log.e("KIOSK", ex.toString())
        }

    }

    fun isDeviceOwner(): Boolean = devicePolicyManager.isDeviceOwnerApp(packageName)

}