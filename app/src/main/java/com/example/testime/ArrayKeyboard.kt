package com.example.testime

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TableLayout
import android.widget.TableRow

class ArrayKeyboard(baseView: FrameLayout, inputMethod: InputMethod) :
	BaseKeyboard(baseView, R.id.array, inputMethod) {
	/*Last Touch Position*/
	private var lastTouchX = 0.0f
	private var lastTouchY = 0.0f

	/*Handler*/
	private val handler = Handler(Looper.getMainLooper())

	init {
		candidateLib.loadWords(R.raw.words)
		candidateLib.loadMapping(R.raw.mapping)
		candidateLib.loadFunKeyMap(
			mapOf(
				"enter" to KeyEvent.KEYCODE_NUMPAD_ENTER,
				"space" to KeyEvent.KEYCODE_SPACE,
				"tab" to KeyEvent.KEYCODE_TAB,
				"left" to KeyEvent.KEYCODE_DPAD_LEFT,
				"right" to KeyEvent.KEYCODE_DPAD_RIGHT,
				"shift" to KeyEvent.KEYCODE_SHIFT_LEFT
			)
		)
	}

	@SuppressLint("ClickableViewAccessibility")
	override fun setupButtons() {
		val tableLayout = view.findViewById<TableLayout>(R.id.tableLayout)
		for (i in 0 until tableLayout.childCount) {
			val row: TableRow = tableLayout.getChildAt(i) as TableRow
			for (j in 0 until row.childCount) {
				val button = (row.getChildAt(j) as FrameLayout).getChildAt(0)
				val id = inputMethod.resources.getResourceName(button.id).substringAfter('_')
				val type = id.substringBefore('_')
				val name = id.substringAfter('_')
				button.setOnTouchListener { _: View, motionEvent: MotionEvent ->
					when (type) {
						"fun" -> onFunButtonTouch(motionEvent, button, name)
						"char" -> onNumButtonTouch(motionEvent, button, name)
						else -> false
					}
				}
			}
		}
	}
	private fun onNumButtonTouch(motionEvent: MotionEvent, button: View, name: String): Boolean {
		when (motionEvent.action) {
			MotionEvent.ACTION_DOWN -> {
				button.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.7f).setDuration(0).start()
				lastTouchX = motionEvent.x
				lastTouchY = motionEvent.y
			}
			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				button.animate().scaleX(1.0f).scaleY(1.0f).alpha(1f).setDuration(0).start()
				val deltaX = motionEvent.x - lastTouchX
				val deltaY = motionEvent.y - lastTouchY
				lastTouchX = 0.0f
				lastTouchY = 0.0f
				when (getSwipeDirection(deltaX, deltaY)) {
					SwipeDirection.UP -> updateInputTextView("$name↑")
					SwipeDirection.DOWN -> updateInputTextView("$name↓")
					SwipeDirection.LEFT -> commitText(name[0].toString())
					SwipeDirection.RIGHT -> updateInputTextView("w$name")
					else -> updateInputTextView("$name-")
				}
			}
		}
		return true
	}

	private fun onFunButtonTouch(motionEvent: MotionEvent, button: View, name: String): Boolean {
		when (motionEvent.action) {
			MotionEvent.ACTION_DOWN -> {
				button.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.7f).setDuration(0).start()
				when (name) {
					"back" -> handler.postDelayed(backKeyDown, DELAY_MILLIS)
					"mode" -> {}
					else -> candidateLib.getKeyValue(name)?.let { sendDownKeyEvent(it, false) }
				}
			}
			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				button.animate().scaleX(1.0f).scaleY(1.0f).alpha(1f).setDuration(0).start()
				when (name) {
					"back" -> handler.removeCallbacks(backKeyDown)
					"mode" -> changeMode()
					else -> candidateLib.getKeyValue(name)?.let { sendUpKeyEvent(it, false) }
				}
			}
		}
		return true
	}

	private val backKeyDown = object : Runnable {
		override fun run() {
			deleteLastInput()
			handler.postDelayed(this, DELAY_MILLIS * 2)
		}
	}
}