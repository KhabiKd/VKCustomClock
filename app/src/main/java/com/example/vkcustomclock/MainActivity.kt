package com.example.vkcustomclock

import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private lateinit var radioGroup: RadioGroup
    private lateinit var acv: AnalogClockView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        radioGroup = findViewById(R.id.radiogroup)
        acv = findViewById(R.id.analogClockView)
        radioGroup
            .setOnCheckedChangeListener(radioGroupOnCheckedChangeListener);

        loadPreferences()
    }

    private var radioGroupOnCheckedChangeListener: RadioGroup.OnCheckedChangeListener =
        RadioGroup.OnCheckedChangeListener { _, checkedId ->
            val checkedRadioButton = radioGroup
                .findViewById<View>(checkedId) as RadioButton
            val checkedIndex = radioGroup.indexOfChild(checkedRadioButton)
            acv.setDigitType(checkedIndex)
            savePreferences(KEY_RADIOBUTTON_INDEX, checkedIndex)
        }

    private fun savePreferences(key: String, value: Int) {
        val sharedPreferences = getSharedPreferences(
            APP_PREFERENCES, MODE_PRIVATE
        )
        val editor = sharedPreferences.edit()
        editor.putInt(key, value)
        editor.apply()
    }

    private fun loadPreferences() {
        val sharedPreferences = getSharedPreferences(
            APP_PREFERENCES, MODE_PRIVATE
        )
        val savedRadioIndex = sharedPreferences.getInt(
            KEY_RADIOBUTTON_INDEX, 0
        )
        val savedCheckedRadioButton = radioGroup
            .getChildAt(savedRadioIndex) as RadioButton
        savedCheckedRadioButton.isChecked = true
    }

    companion object {
        const val APP_PREFERENCES = "mysettings"
        const val KEY_RADIOBUTTON_INDEX = "SAVED_RADIO_BUTTON_INDEX"
    }
}