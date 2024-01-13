package com.example.testime

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout


class NumberKeyboard(baseView: FrameLayout, inputMethod: InputMethod) :
	BaseKeyboard(baseView, R.id.number, inputMethod) {
	/*Last Touch Position*/
	private var lastTouchX = 0.0f
	private var lastTouchY = 0.0f

	/*Handler*/
	private val handler = Handler(Looper.getMainLooper())

	/*Shift*/
	private var isShifting = false

	val numChoice = arrayOf("0⁰₀", "1¹₁", "2²₂", "3³₃", "4⁴₄", "5⁵₅", "6⁶₆", "7⁷₇","8⁸₈","9⁹₉")
	val choicePos = arrayOf(0, 3, 1, 3, 2)

	init {
		candidateLib.loadFunKeyMap(
			mapOf(
				"enter" to KeyEvent.KEYCODE_NUMPAD_ENTER,
				"space" to KeyEvent.KEYCODE_SPACE,
				"tab" to KeyEvent.KEYCODE_TAB,
				"shift" to KeyEvent.KEYCODE_SHIFT_LEFT,
			)
		)
		candidateLib.loadSymbolList(
			listOf(
				listOf("*", "⋅", "+", "=", "`"),
				listOf("/","⁄","|","\\", "~")
			)
		)
	}

	override fun onCharButtonTouch(motionEvent: MotionEvent, button: View, name: String): Boolean {
		when (motionEvent.action) {
			MotionEvent.ACTION_DOWN -> {
				button.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.7f).setDuration(0).start()
				lastTouchX = motionEvent.x
				lastTouchY = motionEvent.y
				Log.d("my", name[0].digitToInt().toString())
				setPopupText(*IntRange(0,4).map { numChoice[name[0].digitToInt()].getOrNull(choicePos[it])?.toString()?:"" }.toTypedArray())
				showPopup(button)
			}

			MotionEvent.ACTION_MOVE -> {
				val deltaX = motionEvent.x - lastTouchX
				val deltaY = motionEvent.y - lastTouchY
				getSwipeDirection(deltaX, deltaY).also { dir ->
					if (dir == SwipeDirection.NONE) setPopupText(*IntRange(0,4).map { numChoice[name[0].digitToInt()].getOrNull(choicePos[it])?.toString()?:"" }.toTypedArray())
					else setPopupText(numChoice[name[0].digitToInt()].getOrNull(choicePos[dir.value])?.toString()?:"")
				}
				showPopup(button)
			}

			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				popupWindow.dismiss()
				button.animate().scaleX(1.0f).scaleY(1.0f).alpha(1f).setDuration(0).start()
				val deltaX = motionEvent.x - lastTouchX
				val deltaY = motionEvent.y - lastTouchY
				lastTouchX = 0.0f
				lastTouchY = 0.0f
				getSwipeDirection(deltaX, deltaY).also{ dir ->
					if(dir == SwipeDirection.NONE)
						sendDownKeyEvent(name[0].code - 41, isShifting)
					else
						commitText(numChoice[name[0].digitToInt()].getOrNull(choicePos[dir.value])?.toString()?:"")
				}
			}
		}
		return true
	}

	override fun onFunButtonTouch(motionEvent: MotionEvent, button: View, name: String): Boolean {
		when (motionEvent.action) {
			MotionEvent.ACTION_DOWN -> {
				lastTouchX = motionEvent.x
				lastTouchY = motionEvent.y
				button.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.7f).setDuration(0).start()
				when (name) {
					"back" -> handler.postDelayed(backKeyDown, DELAY_MILLIS)
					"mode" -> {}
					"shift" -> {
						isShifting = true
						candidateLib.getKeyValue(name)?.let { sendDownKeyEvent(it, false) }
					}

					else -> candidateLib.getKeyValue(name)?.let { sendDownKeyEvent(it, isShifting) }
				}
			}

			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				val deltaX = motionEvent.x - lastTouchX
				val deltaY = motionEvent.y - lastTouchY
				lastTouchX = 0.0f
				lastTouchY = 0.0f
				button.animate().scaleX(1.0f).scaleY(1.0f).alpha(1f).setDuration(0).start()
				when (name) {
					"back" -> handler.removeCallbacks(backKeyDown)
					"mode" -> getSwipeDirection(deltaX, deltaY).let {
						changeMode(it.value)
					}
					"shift" -> {
						isShifting = false
						candidateLib.getKeyValue(name)?.let { sendUpKeyEvent(it, false) }
					}

					else -> candidateLib.getKeyValue(name)?.let { sendUpKeyEvent(it, isShifting) }
				}
			}
		}
		return true
	}

	override fun onSymButtonTouch(motionEvent: MotionEvent, button: View, name: String): Boolean {
		val index = name.toInt() - 1
		when (motionEvent.action) {
			MotionEvent.ACTION_DOWN -> {
				button.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.7f).setDuration(0).start()
				lastTouchX = motionEvent.x
				lastTouchY = motionEvent.y
				setPopupText(*candidateLib.getSymIndices(index)
					.map { candidateLib.getSymbol(index, it) }.toTypedArray()
				)
				showPopup(button)
			}

			MotionEvent.ACTION_MOVE -> {
				val deltaX = motionEvent.x - lastTouchX
				val deltaY = motionEvent.y - lastTouchY

				val dir = getSwipeDirection(deltaX, deltaY)
				if (dir == SwipeDirection.NONE) setPopupText(
					*candidateLib.getSymIndices(index).map { candidateLib.getSymbol(index, it) }
						.toTypedArray()
				)
				else setPopupText(candidateLib.getSymbol(index, dir.value))

				showPopup(button)
			}

			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				popupWindow.dismiss()
				button.animate().scaleX(1.0f).scaleY(1.0f).alpha(1f).setDuration(0).start()
				val deltaX = motionEvent.x - lastTouchX
				val deltaY = motionEvent.y - lastTouchY
				lastTouchX = 0.0f
				lastTouchY = 0.0f
				val dir = getSwipeDirection(deltaX, deltaY)
				commitText(candidateLib.getSymbol(index, dir.value))
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