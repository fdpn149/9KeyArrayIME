package com.example.testime

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView

class SwipeButton : FrameLayout {

	private var buttonTag = "btn_char_1"
	private var centerText = "Button"
	private var startText = ""
	private var topText = ""
	private var endText = ""
	private var bottomText = ""

	constructor(context: Context) : super(context) {
		initViews()
	}

	constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
		val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SwipeButton,0,0)
		getValues(typedArray)
		initViews()
	}

	constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
		context, attrs, defStyleAttr
	) {
		val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SwipeButton, defStyleAttr, 0)
		getValues(typedArray)
		initViews()
	}

	private fun getValues(typedArray: TypedArray) {
		buttonTag = typedArray.getString(R.styleable.SwipeButton_buttonTag) ?: buttonTag
		centerText = typedArray.getString(R.styleable.SwipeButton_centerText) ?: centerText
		startText = typedArray.getString(R.styleable.SwipeButton_startText) ?: startText
		topText = typedArray.getString(R.styleable.SwipeButton_topText) ?: topText
		endText = typedArray.getString(R.styleable.SwipeButton_endText) ?: endText
		bottomText = typedArray.getString(R.styleable.SwipeButton_bottomText) ?: bottomText

		typedArray.recycle()
	}

	private fun initViews() {
		inflate(context, R.layout.component_swipebutton,this)
		val frameLayout = getChildAt(0) as FrameLayout
		(frameLayout.getChildAt(0) as Button).tag = buttonTag
		(frameLayout.getChildAt(1) as TextView).text = centerText
		(frameLayout.getChildAt(2) as TextView).text = startText
		(frameLayout.getChildAt(3) as TextView).text = topText
		(frameLayout.getChildAt(4) as TextView).text = endText
		(frameLayout.getChildAt(5) as TextView).text = bottomText
	}
}
