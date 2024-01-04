package com.example.testime

import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

abstract class BaseKeyboard(baseView: FrameLayout, id: Int,
	protected val inputMethod: InputMethod
) :
	InputMethodService() {
	/*Candidate Library*/
	protected val candidateLib = CandidateLibrary(inputMethod)
	/*View*/
	var view: ConstraintLayout = baseView.findViewById(id)
//	private var tableLayout: TableLayout = view.findViewById(R.id.tableLayout)
	private val inputView = InputView(view)
	private var candidateRecyclerView: RecyclerView? = view.findViewById(R.id.candidateRecyclerView)

	abstract fun setupButtons()

	companion object {
		const val SWIPE_THRESHOLD = 50.0f
		const val DELAY_MILLIS = 50L
	}

	enum class SwipeDirection {
		NONE, UP, DOWN, LEFT, RIGHT
	}

	init {
		candidateRecyclerView?.layoutManager =
			LinearLayoutManager(inputMethod, RecyclerView.HORIZONTAL, false)
		candidateRecyclerView?.adapter = CandidateAdapter(inputMethod, this)
		setupButtons()
	}

	protected fun changeMode() {
		inputMethod.changeMode()
	}

	protected fun deleteLastInput() {
		val currentText = inputView.getText()
		if(currentText.isEmpty()) {
			sendDownKeyEvent(KeyEvent.KEYCODE_DEL, false)
		}
		else {
			val newText = currentText.substring(0, currentText.lastIndexOf(" "))
			if (newText.isEmpty()) {
				inputView.clearText()
				(candidateRecyclerView?.adapter as? CandidateAdapter)?.clearCandidates()
			} else {
				inputView.setText(newText)
				updateCandidateView()
			}
		}
	}

	protected fun sendUpKeyEvent(keyCode: Int, shift: Boolean) {
		val event = KeyEvent(0, 0, KeyEvent.ACTION_UP, keyCode, 0, if(shift) 1 else 0)
		inputMethod.currentInputConnection?.sendKeyEvent(event)
	}

	protected fun sendDownKeyEvent(keyCode: Int, shift: Boolean) {
		val event = KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyCode, 0, if(shift) 1 else 0)
		inputMethod.currentInputConnection?.sendKeyEvent(event)
	}

	protected fun updateInputTextView(value: String) {
		inputView.append(value)
		updateCandidateView()
	}

	private fun updateCandidateView() {
		val currentInput = inputView.getText()
		val candidates = candidateLib.getMatchCandidates(currentInput)
		(candidateRecyclerView?.adapter as? CandidateAdapter)?.updateCandidates(candidates)
	}

	fun handleCandidateClick(candidate: String) {
		commitText(candidate)
		inputView.clearText()
	}

	protected fun commitText(candidate: String) {
		val inputConnection = inputMethod.currentInputConnection
		inputConnection?.commitText(candidate, candidate.length)
	}

	protected fun getSwipeDirection(deltaX: Float, deltaY: Float): SwipeDirection {
		if(abs(deltaX) <= SWIPE_THRESHOLD && abs(deltaY) <= SWIPE_THRESHOLD)
			return SwipeDirection.NONE
		if (abs(deltaX) > abs(deltaY)) {
			if (deltaX > 0)
				return SwipeDirection.RIGHT
			return SwipeDirection.LEFT
		}
		if (deltaY > 0)
			return SwipeDirection.DOWN
		return SwipeDirection.UP
	}
}