package org.wordpress.android.fluxc.example

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.TypedValue
import androidx.fragment.app.FragmentActivity
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import kotlinx.android.synthetic.main.activity_example.*
import javax.inject.Inject

class MainExampleActivity : FragmentActivity(), HasAndroidInjector {
    @Inject lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_example)

        // enable developers to set a custom font size
        val logFontSz = BuildConfig.EXAMPLE_LOG_FONT_SIZE_SP.toFloat()
        log.setTextSize(TypedValue.COMPLEX_UNIT_SP, logFontSz)

        if (savedInstanceState == null) {
            val mf = MainFragment()
            supportFragmentManager.beginTransaction().add(R.id.fragment_container, mf).commit()
        }
        log.movementMethod = ScrollingMovementMethod()
        prependToLog("I'll log stuff here.")
    }

    fun prependToLog(s: String) {
        val output = s + "\n" + log.text
        log.text = output
    }

    override fun androidInjector(): AndroidInjector<Any> = dispatchingAndroidInjector
}
