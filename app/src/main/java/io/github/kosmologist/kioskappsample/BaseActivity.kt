package io.github.kosmologist.kioskappsample

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.View

open abstract class BaseActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

}