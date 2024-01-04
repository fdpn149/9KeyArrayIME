package com.example.testime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout

class InputMethod : InputMethodService() {
	private lateinit var baseView: FrameLayout
	private lateinit var arrayKeyboard: ArrayKeyboard
	private lateinit var englishKeyboard: EnglishKeyboard

	private var mode = 0

	override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
		super.onStartInputView(info, restarting)
	}

	override fun onCreateInputView(): View {
		baseView = layoutInflater.inflate(R.layout.input_method, null) as FrameLayout
		arrayKeyboard = ArrayKeyboard(baseView, this)
		englishKeyboard = EnglishKeyboard(baseView, this)
		return baseView
	}

	fun changeMode() {
		englishKeyboard.view.visibility = mode
		mode = mode xor 8
		arrayKeyboard.view.visibility = mode
	}
}