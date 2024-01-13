package com.example.testime

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.PopupWindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

abstract class BaseKeyboard(
	baseView: FrameLayout, id: Int, private val inputMethod: InputMethod
) : InputMethodService() {
	/*Candidate Library*/
	protected val candidateLib = CandidateLibrary(inputMethod)

	/*View*/
	var view: ConstraintLayout = baseView.findViewById(id)
	private var tableLayout: TableLayout = view.findViewById(R.id.tableLayout)
	private val inputView = InputView(view)
	private var candidateRecyclerView: RecyclerView? = view.findViewById(R.id.candidateRecyclerView)

	protected val popupWindow: PopupWindow = PopupWindow(inputMethod)
	private val popupView: View = inputMethod.layoutInflater.inflate(R.layout.key_popup, null)

	companion object {
		const val SWIPE_THRESHOLD = 50.0f
		const val DELAY_MILLIS = 50L
	}

	private val pos_id = listOf(R.id.textCenter, R.id.textStart, R.id.textTop, R.id.textEnd, R.id.textBottom)
	enum class SwipeDirection(val value: Int) {
		NONE(0), LEFT(1), UP(2), RIGHT(3), DOWN(4)
	}

	init {
		val orientation = inputMethod.resources.configuration.orientation
		val dm = inputMethod.resources.displayMetrics
		var imeHeight = when(orientation) {
			Configuration.ORIENTATION_LANDSCAPE -> dm.heightPixels * 0.7F
			else -> dm.heightPixels * 0.4F
		}
		view.layoutParams.also {
			it.height = imeHeight.toInt()
			view.layoutParams = it
		}
		candidateRecyclerView?.layoutManager =
			LinearLayoutManager(inputMethod, RecyclerView.HORIZONTAL, false)
		candidateRecyclerView?.adapter = CandidateAdapter(inputMethod, this)
		setupButtons()
	}


	@SuppressLint("ClickableViewAccessibility")
	protected fun setupButtons() {
		for (i in 0 until tableLayout.childCount) {
			val row: TableRow = tableLayout.getChildAt(i) as TableRow
			for (j in 0 until row.childCount) {
				val button = (row.getChildAt(j) as FrameLayout).getChildAt(0)
				val id = inputMethod.resources.getResourceName(button.id).substringAfter('_')
				val type = id.substringBefore('_')
				val name = id.substringAfter('_')

				button.setOnTouchListener { _: View, motionEvent: MotionEvent ->
					when (type) {
						"char" -> onCharButtonTouch(motionEvent, button, name)
						"fun" -> onFunButtonTouch(motionEvent, button, name)
						"sym" -> onSymButtonTouch(motionEvent, button, name)
						else -> false
					}
				}
			}
		}
	}

	protected open fun onCharButtonTouch(
		motionEvent: MotionEvent, button: View, name: String
	): Boolean {
		return false
	}

	protected open fun onFunButtonTouch(
		motionEvent: MotionEvent, button: View, name: String
	): Boolean {
		return false
	}

	protected open fun onSymButtonTouch(
		motionEvent: MotionEvent, button: View, name: String
	): Boolean {
		return false
	}

	protected fun changeMode(mode: Int) {
		inputMethod.changeMode(mode)
	}

	protected fun deleteLastInput() {
		val currentText = inputView.getText()
		if (currentText.isEmpty()) {
			sendDownKeyEvent(KeyEvent.KEYCODE_DEL, false)
		} else {
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
		val event = KeyEvent(0, 0, KeyEvent.ACTION_UP, keyCode, 0, if (shift) 1 else 0)
		inputMethod.currentInputConnection?.sendKeyEvent(event)
	}

	protected fun sendDownKeyEvent(keyCode: Int, shift: Boolean) {
		val event = KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyCode, 0, if (shift) 1 else 0)
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
		if(candidate != "") {
			val inputConnection = inputMethod.currentInputConnection
			inputConnection?.commitText(candidate, candidate.length)
		}
	}

	protected fun getSwipeDirection(deltaX: Float, deltaY: Float): SwipeDirection {
		if (abs(deltaX) <= SWIPE_THRESHOLD && abs(deltaY) <= SWIPE_THRESHOLD) return SwipeDirection.NONE
		if (abs(deltaX) > abs(deltaY)) {
			if (deltaX > 0) return SwipeDirection.RIGHT
			return SwipeDirection.LEFT
		}
		if (deltaY > 0) return SwipeDirection.DOWN
		return SwipeDirection.UP
	}

	protected fun setPopupText(
		vararg texts: String
	) {
		for (i in texts.indices)
			popupView.findViewById<TextView>(pos_id[i]).text = texts[i]
		for (i in texts.size until 5)
			popupView.findViewById<TextView>(pos_id[i]).text = ""
		popupWindow.contentView = popupView
	}

	protected fun showPopup(button: View) {
		popupView.measure(
			View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
			View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
		)
		PopupWindowCompat.showAsDropDown(
			popupWindow,
			button,
			abs(button.width - popupView.measuredWidth) / 2,
			-popupView.measuredHeight - button.height,
			Gravity.START
		)
	}
}