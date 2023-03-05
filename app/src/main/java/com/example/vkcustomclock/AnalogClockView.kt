package com.example.vkcustomclock

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.text.format.DateUtils.SECOND_IN_MILLIS
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.properties.Delegates


enum class ClockLineType {
    Hours,
    Minutes
}

class AnalogClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mActive: Boolean = false
    private lateinit var mTime: Calendar

    private lateinit var mPaint: Paint
    private lateinit var mMinutesPaint: Paint
    private lateinit var mDigitsPaint: Paint
    private lateinit var mHandsPaint: Paint
    private lateinit var mSecondHandPaint: Paint

    private var digitType by Delegates.notNull<Int>()

    private val mRect = Rect()
    private val mPath = Path()

    private var mRadius = 0f
    private var mPadding = 0f
    private var mNumeralSpacing = 0f
    private var mSecondHandRadius = 16f

    private var digitsRoman: Array<String> = arrayOf(
        "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"
    )
    private var digitsArabic: Array<String> =
        arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12")

    private val mClockTick: Runnable = object : Runnable {
        override fun run() {
            onTimeChanged()
            if (mActive) {
                val now = System.currentTimeMillis()
                val delay: Long = SECOND_IN_MILLIS - now % SECOND_IN_MILLIS
                postDelayed(this, delay)
            }
        }
    }

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.AnalogClockView,
            R.attr.analogClockStyle,
            0
        ).apply {
            try {
                digitType = getInteger(R.styleable.AnalogClockView_digitType, 0)
            } finally {
                recycle()
            }
        }
        mActive = true

        initPainter()
    }


    private fun initPainter() {
        mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            strokeWidth = 4f
        }
        mMinutesPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            strokeWidth = 8f
        }
        mDigitsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            typeface = Typeface.DEFAULT_BOLD
            textSize =
                TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 28f, resources.displayMetrics)
                    .toInt().toFloat()
        }
        mHandsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            strokeWidth = 16f
            strokeCap = Paint.Cap.ROUND
        }
        mSecondHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isDither = true
            color = Color.RED
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 8f
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_TIME_TICK)
        filter.addAction(Intent.ACTION_TIME_CHANGED)
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
        mTime = Calendar.getInstance()
        onTimeChanged()
        if (mActive) {
            mClockTick.run()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(mClockTick)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mRadius = (min(w, h) / 2f)
    }

//    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
//        val minWidth  = suggestedMinimumWidth + paddingLeft + paddingRight
//        val minHeight = suggestedMinimumHeight + paddingTop + paddingBottom
//
//        val desiredRadiusInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, DESIRED_RADIUS,
//            resources.displayMetrics).toInt()
//
//        val desiredWidth = max(minWidth, desiredRadiusInPixels + paddingLeft + paddingRight)
//        val desiredHeight = max(minHeight, desiredRadiusInPixels + paddingTop + paddingBottom)
//
//        setMeasuredDimension(
//            resolveSize(desiredWidth, widthMeasureSpec),
//            resolveSize(desiredHeight, heightMeasureSpec)
//        )
//    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        drawLines(canvas, mMinutesPaint)
        when (digitType) {
            0 -> drawDigits(canvas, mDigitsPaint, digitsArabic)
            1 -> drawDigits(canvas, mDigitsPaint, digitsRoman)
        }
        drawHandLine(canvas)
        canvas?.drawCircle((width / 2).toFloat(), (height / 2).toFloat(), 14F, mMinutesPaint)
    }

    private fun drawLines(canvas: Canvas?, mMinutesP: Paint) {
        for (i in 0..59) {
            val lineType = when {
                i % 5 == 0 -> ClockLineType.Hours
                else -> ClockLineType.Minutes
            }
            val lineLength = when (lineType) {
                ClockLineType.Minutes -> 15
                ClockLineType.Hours -> 30
            }
            when (lineType) {
                ClockLineType.Minutes -> mMinutesP.also { it.strokeWidth = 4f }
                ClockLineType.Hours -> mMinutesP.also { it.strokeWidth = 8f }
            }
            when (lineType) {
                ClockLineType.Minutes -> mMinutesP.also { it.strokeCap = Paint.Cap.BUTT }
                ClockLineType.Hours -> mMinutesP.also { it.strokeCap = Paint.Cap.ROUND }
            }
            val angleInRad = i * (360f / 60f) * (PI.toFloat() / 180f)
            val lineStart: Pair<Float, Float> = Pair(
                ((mRadius - lineLength) * cos(angleInRad) + width / 2),
                ((mRadius - lineLength) * sin(angleInRad) + height / 2)
            )
            val lineEnd: Pair<Float, Float> = Pair(
                mRadius * cos(angleInRad) + width / 2,
                mRadius * sin(angleInRad) + height / 2
            )
            canvas?.drawLine(
                lineStart.first,
                lineStart.second,
                lineEnd.first,
                lineEnd.second,
                mMinutesP
            )
        }
    }

    private fun drawDigits(canvas: Canvas?, mDigitsP: Paint, digits: Array<String>) {
        mPadding = mNumeralSpacing + 90

        for ((cnt, i) in (0..59 step 5).withIndex()) {
            mDigitsP.getTextBounds(digits[cnt], 0, digits[cnt].length, mRect)
            val angleInRad = i * (360f / 60f) * (PI.toFloat() / 180f) - PI.toFloat() / 3f

            val coordinates: Pair<Float, Float> = Pair(
                if ((digits[cnt] != "12" && digits[cnt] != "XII") && (digits[cnt] != "6" && digits[cnt] != "VI")) ((mRadius - mPadding) * cos(
                    angleInRad
                ) + width / 2 - mRect.height() / 2)
                else if (digits[cnt] == "6" || digits[cnt] == "VI") ((mRadius - mPadding) * cos(
                    angleInRad
                ) + width / 2 - mRect.height() / 2 + 5)
                else ((mRadius - mPadding) * cos(angleInRad) + width / 2 - mRect.height() / 2 - 15),
                ((mRadius - mPadding) * sin(angleInRad) + height / 2 + mRect.height() / 2)
            )
            canvas?.drawText(digits[cnt], coordinates.first, coordinates.second, mDigitsP)
        }
    }

    private fun drawHandLine(canvas: Canvas?) {
        val lengthSeconds = mRadius
        val lengthMinutes = mRadius - mPadding + 10
        val lengthHour: Double = lengthMinutes / 1.5
        val calendar = Calendar.getInstance()

        val tSec = 6f * calendar[Calendar.SECOND]

        val tMin =
            6f * (calendar[Calendar.MINUTE] + (1 / 60f) * calendar[Calendar.SECOND])

        val tHour =
            30f * (calendar[Calendar.HOUR] + (1 / 60f) * calendar[Calendar.MINUTE] + (1 / 3600f) * calendar[Calendar.SECOND])

        // Часовая стрелка
        canvas?.drawLine(
            (width / 2 - lengthHour / 6f * cos((-90f + tHour) * (PI / 180f))).toFloat(),
            (height / 2 - lengthHour / 6f * sin((-90f + tHour) * (PI / 180f))).toFloat(),
            (width / 2 + lengthHour * cos((-90f + tHour) * (PI / 180f))).toFloat(),
            (height / 2 + lengthHour * sin((-90f + tHour) * (PI / 180f))).toFloat(),
            mHandsPaint.apply {
                color = Color.BLACK
                strokeWidth = 16f
            })

        // Минутная стрелка
        canvas?.drawLine(
            (width / 2 - lengthMinutes / 6f * cos((-90f + tMin) * (PI / 180f))).toFloat(),
            (height / 2 - lengthMinutes / 6f * sin((-90f + tMin) * (PI / 180f))).toFloat(),
            (width / 2 + lengthMinutes * cos((-90f + tMin) * (PI / 180f))).toFloat(),
            (height / 2 + lengthMinutes * sin((-90f + tMin) * (PI / 180f))).toFloat(),
            mHandsPaint.apply {
                color = Color.BLACK
                strokeWidth = 16f
            })

        // Секундная стрелка
        mPath.reset()
        mPath.moveTo(
            (width / 2 - (lengthSeconds / 4f) * cos((-90f + tSec) * (PI / 180f))).toFloat(),
            (height / 2 - (lengthSeconds / 4f) * sin((-90f + tSec) * (PI / 180f))).toFloat(),
        )
        mPath.lineTo(
            (width / 2 - mSecondHandRadius * cos((-90f + tSec) * (PI / 180f))).toFloat(),
            (height / 2 - mSecondHandRadius * sin((-90f + tSec) * (PI / 180f))).toFloat(),
        )
        mPath.addCircle(width / 2f, height / 2f, mSecondHandRadius, Path.Direction.CW)
        mPath.moveTo(
            (width / 2 + mSecondHandRadius * cos((-90f + tSec) * (PI / 180f))).toFloat(),
            (height / 2 + mSecondHandRadius * sin((-90f + tSec) * (PI / 180f))).toFloat(),
        )
        mPath.lineTo(
            (width / 2 + lengthSeconds * cos((-90f + tSec) * (PI / 180f))).toFloat(),
            (height / 2 + lengthSeconds * sin((-90f + tSec) * (PI / 180f))).toFloat(),
        )
        canvas?.drawPath(mPath, mSecondHandPaint)
    }

    private fun onTimeChanged() {
        mTime.timeInMillis = System.currentTimeMillis()
        invalidate()
//        requestLayout()
    }

    @JvmName("setDigitType1")
    fun setDigitType(type: Int) {
        digitType = type
    }
}