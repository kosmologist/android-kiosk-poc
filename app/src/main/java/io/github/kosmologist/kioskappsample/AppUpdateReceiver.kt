package io.github.kosmologist.kioskappsample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Created by kosmologist on 5/6/18.
 */
class AppUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i("KIOSK", "App Updated, Relauncing...")
        val intent = Intent(context?.applicationContext, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context?.startActivity(intent)
    }
}