package com.example.testime

import android.inputmethodservice.InputMethodService
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import com.example.testime.databinding.TestBinding

class InputMethod : InputMethodService() {
	private lateinit var baseView: FrameLayout
	private lateinit var arrayKeyboard: ArrayKeyboard
	private lateinit var englishKeyboard: EnglishKeyboard
	private lateinit var numberKeyboard: NumberKeyboard


	override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
		super.onStartInputView(info, restarting)
	}

	override fun onCreateInputView(): View {
		baseView = LayoutInflater.from(this).inflate(R.layout.input_method, null) as FrameLayout
//		baseView = layoutInflater.inflate(R.layout.input_method, null) as FrameLayout

		arrayKeyboard = ArrayKeyboard(baseView, this)
		englishKeyboard = EnglishKeyboard(baseView, this)
		numberKeyboard = NumberKeyboard(baseView, this)

		return baseView
	}

	fun changeMode(mode: Int) {
		val second = (mode and 2) shr 1
		val last = mode and 1
		arrayKeyboard.view.visibility = second shl 3
		englishKeyboard.view.visibility = (second.inv() or last) shl 3
		numberKeyboard.view.visibility = (second and last).inv() shl 3
	}
}