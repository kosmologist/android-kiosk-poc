package io.github.kosmologist.kioskappsample

import android.app.admin.DeviceAdminReceiver
import android.bluetooth.BluetoothClass
import android.content.ComponentName
import android.content.Context


/**
 * Created by kosmologist on 5/5/18.
 */
class DeviceAdministratorReceiver : DeviceAdminReceiver() {

    companion object {
        fun getComponentName(context: Context): ComponentName {
            return ComponentName(context.applicationContext,
                    DeviceAdministratorReceiver::class.java)
        }
    }
}