package com.example.testime

import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import java.io.Closeable

class BindingVars(vararg closeables: Closeable?) : ViewModel(*closeables) {
	val variable = ObservableField<Float>(20.0f)

	fun setValue(value: Float) {
		variable.set(value)
	}
}