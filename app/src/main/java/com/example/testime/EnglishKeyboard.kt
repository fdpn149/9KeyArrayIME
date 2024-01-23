package com.example.testime

import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import com.example.testime.databinding.ArrayKeyboardBinding
import com.example.testime.databinding.EnglishKeyboardBinding


class EnglishKeyboard(baseView: FrameLayout, inputMethod: InputMethod) :
	BaseKeyboard(baseView, R.id.english, inputMethod) {
	/*Last Touch Position*/
	private var lastTouchX = 0.0f
	private var lastTouchY = 0.0f

	/*Handler*/
	private val handler = Handler(Looper.getMainLooper())

	/*Shift*/
	private var isShifting = false

	private var letterPos = arrayOf(1, 0, 4, 2, 3)

	init {
		val binding: EnglishKeyboardBinding = DataBindingUtil.bind(view)!!
		binding.bindVars = bindVars
		bindVars.setValue(25.0f)

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
				listOf("@", "<", "-", ">", "_"),
				listOf(".", "(", "'", ")", ","),
				listOf("&", "[", "!", "]", "?"),
				listOf(":", "{", "\"", "}", ";")
			)
		)
	}

	override fun onCharButtonTouch(motionEvent: MotionEvent, button: View, name: String): Boolean {
		when (motionEvent.action) {
			MotionEvent.ACTION_DOWN -> {
				button.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.7f).setDuration(0).start()
				lastTouchX = motionEvent.x
				lastTouchY = motionEvent.y
				setPopupText(*letterPos.map { name.getOrNull(it)?.toString() ?: "" }.toTypedArray())
				showPopup(button)
			}

			MotionEvent.ACTION_MOVE -> {
				val deltaX = motionEvent.x - lastTouchX
				val deltaY = motionEvent.y - lastTouchY
				getSwipeDirection(deltaX, deltaY).also { dir ->
					if (dir == SwipeDirection.NONE) setPopupText(*letterPos.map {
						name.getOrNull(it)?.toString() ?: ""
					}.toTypedArray())
					else setPopupText(name.getOrNull(letterPos[dir.value])?.toString() ?: "")
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
				val dir = getSwipeDirection(deltaX, deltaY)
				val letter = name.getOrNull(letterPos[dir.value])
				if (letter != null)
					sendDownKeyEvent(letter.code - 68, isShifting)
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