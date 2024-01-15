package com.example.testime

import androidx.databinding.ObservableField
import androidx.lifecycle.ViewModel
import java.io.Closeable

class Test(vararg closeables: Closeable?) : ViewModel(*closeables) {
	val variable = ObservableField<Float>()

	fun update(value: Float) {
		variable.set(value)
	}
}