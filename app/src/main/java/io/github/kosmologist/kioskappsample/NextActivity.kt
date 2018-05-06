package io.github.kosmologist.kioskappsample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle

class NextActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_next)
    }

    override fun getActivityClass(): Class<*> {
        return NextActivity::class.java
    }
}
