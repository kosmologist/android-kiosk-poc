package io.github.kosmologist.kioskappsample

import android.app.ActivityManager
import android.app.DownloadManager
import android.app.admin.DevicePolicyManager
import android.app.admin.SystemUpdatePolicy
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import java.io.File
import java.io.FileInputStream


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

        val updateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.i("KIOSK", "App Update Sequence Completed")
                val intent = Intent(this@BaseActivity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                unregisterReceiver(this)
            }
        }
        registerReceiver(updateReceiver, IntentFilter("GO_TO_HELL_PLEASE"))
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
            R.id.menu_update_app -> {
                onAppUpdate()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onAppUpdate() {
        Log.d("KIOSK", "Checking for Updates...")
        Toast.makeText(this, "Checking for Updates", Toast.LENGTH_SHORT).show()
        var destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/"
        val apkName = "app-debug.apk"
        destination += apkName
        val destinationUri = Uri.parse("file://" + destination)
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "app-debug.apk")
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(Uri.parse("https://github.com/kosmologist/kiosksample/releases/download/1.0.1/app-debug.apk"))
        request.setDescription("Downloading KioskSample App Update...")
        request.setTitle("Kiosk App Update")
        request.setDestinationUri(destinationUri)
        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
//                val toInstall = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
//                        "app-debug.apk")
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                    val apkUri = FileProvider.getUriForFile(this@BaseActivity, "kiosk.provider", toInstall)
//                    val inst = Intent(Intent.ACTION_INSTALL_PACKAGE)
//                    inst.data = apkUri
//                    inst.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//                    //inst.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startActivity(inst)
//                } else {
//                    val apkUri = Uri.fromFile(toInstall)
//                    val installIntent = Intent(Intent.ACTION_VIEW)
//                    installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive")
//                    //inst.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startActivity(installIntent)
//                }
                val apkFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "app-debug.apk")
                val fileInputStream = FileInputStream(apkFile)
                UpdateUtil.installPackage(this@BaseActivity.applicationContext, fileInputStream, "io.github.kosmologist.kioskappsample")
                unregisterReceiver(this)
            }
        }
        registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun onToggleKioskMode() {
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