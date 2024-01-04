package com.example.testime

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout

class EnglishKeyboard(baseView: FrameLayout, inputMethod: InputMethod) :
	BaseKeyboard(baseView, R.id.english, inputMethod) {
	/*Last Touch Position*/
	private var lastTouchX = 0.0f
	private var lastTouchY = 0.0f

	/*Handler*/
	private val handler = Handler(Looper.getMainLooper())

	/*Shift*/
	private var shift = false

	init {
		candidateLib.loadFunKeyMap(
			mapOf(
				"⏎" to KeyEvent.KEYCODE_NUMPAD_ENTER,
				" " to KeyEvent.KEYCODE_SPACE,
				"↹" to KeyEvent.KEYCODE_TAB,
				"⇧" to KeyEvent.KEYCODE_SHIFT_LEFT,
			)
		)
	}

	override fun setupButtons() {
		for (i in 1..20) {
			val buttonId =
				inputMethod.resources.getIdentifier("button$i", "id", inputMethod.packageName)
			val button = view.findViewById<Button>(buttonId)
			button?.setOnTouchListener { view: View, event: MotionEvent ->
				onButtonTouch(
					event,
					button
				)
			}
		}

	}

	private fun onButtonTouch(event: MotionEvent, button: Button): Boolean {
		val value = button.text.toString()
		when (event.action) {
			MotionEvent.ACTION_DOWN -> {
				button.animate().scaleX(0.9f).scaleY(0.9f).alpha(0.7f).setDuration(0).start()
				when (value) {
					"⌫" -> handler.postDelayed(backKeyDown, DELAY_MILLIS)
					"\uD83C\uDF10" -> {}
					"BUTTON" -> {}
					"⇧" -> {
						candidateLib.getKeyValue(value)?.let { sendDownKeyEvent(it, false) }
						shift = true
					}

					else -> candidateLib.getKeyValue(value)?.let { sendDownKeyEvent(it, false) }
				}

				lastTouchX = event.x
				lastTouchY = event.y
			}

			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				button.animate().scaleX(0.95f).scaleY(0.95f).alpha(1f).setDuration(0).start()
				val deltaX = event.x - lastTouchX
				val deltaY = event.y - lastTouchY
				lastTouchX = 0.0f
				lastTouchY = 0.0f
				if (value.length > 2) {
					when (getSwipeDirection(deltaX, deltaY)) {
						SwipeDirection.UP -> swipeUp(value)
						SwipeDirection.DOWN -> swipeDown(value)
						SwipeDirection.LEFT -> swipeLeft(value)
						SwipeDirection.RIGHT -> swipeRight(value)
						else -> regularClick(value)
					}
				} else when (value) {
					"⌫" -> handler.removeCallbacks(backKeyDown)
					"\uD83C\uDF10" -> changeMode()
					"BUTTON" -> {}
					"⇧" -> {
						candidateLib.getKeyValue(value)?.let { sendUpKeyEvent(it, false) }
						shift = false
					}

					else -> candidateLib.getKeyValue(value)?.let { sendUpKeyEvent(it, false) }
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

	private fun swipeUp(value: String) {

	}

	private fun swipeDown(value: String) {
		if (value.length < 4) return
		if (value[0].isLetter()) {
			sendDownKeyEvent(value[2].code - 36, shift)
			sendUpKeyEvent(value[2].code - 36, shift)
		}
		else {
			commitText(value[2].toString())
		}
	}

	private fun swipeLeft(value: String) {
		if (value[0].isLetter()) {
			sendDownKeyEvent(value[0].code - 36, shift)
			sendUpKeyEvent(value[0].code - 36, shift)
		}
		else {
			commitText(value[0].toString())
		}
	}

	private fun swipeRight(value: String) {
		val pos = if (value.length > 3) 3 else 2
		if (value[0].isLetter()) {
			sendDownKeyEvent(value[pos].code - 36, shift)
			sendUpKeyEvent(value[pos].code - 36, shift)
		} else {
			commitText(value[pos].toString())
		}
	}

	private fun regularClick(value: String) {
		if (value[0].isLetter()) {
			sendDownKeyEvent(value[1].code - 36, shift)
			sendUpKeyEvent(value[1].code - 36, shift)
		} else {
			commitText(value[1].toString())
		}
	}
}