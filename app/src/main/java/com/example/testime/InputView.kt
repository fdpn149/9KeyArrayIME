package com.example.testime

import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

class InputView(baseView: ConstraintLayout) {
	private var view: TextView? = baseView.findViewById(R.id.inputTextView)

	init {
		clearText()
	}

	fun clearText() {
		view?.text = ""
	}

	fun getText(): String {
		return view?.text.toString()
	}

	fun setText(text: String) {
		view?.text = text
	}

	fun append(text: String) {
		view?.text = "${view?.text} $text"
	}
}