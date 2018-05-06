package io.github.kosmologist.kioskappsample

import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvStatus.text = if (isDeviceOwner()) "Status: Device Owner"
        else "Status: Device Unprovisioned"
        tvVersion.text = BuildConfig.VERSION_NAME
        if (isDeviceOwner()) {
            tvInstructions.text = resources.getText(R.string.device_owner_removal)
        }
        btnNext.setOnClickListener {
            startActivity(Intent(this@MainActivity, NextActivity::class.java))
        }
    }

    override fun getActivityClass(): Class<*> {
        return MainActivity::class.java
    }

}
