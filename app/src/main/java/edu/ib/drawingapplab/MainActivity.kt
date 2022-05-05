package edu.ib.drawingapplab

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.MotionEvent
import android.view.MotionEvent.INVALID_POINTER_ID
import android.view.View
import android.view.ViewConfiguration
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.MotionEventCompat
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView.clipBounds

        val myCanvasView = MyCanvasView(this)
        drawingPlace.addView(myCanvasView)

        modeSelection.setOnCheckedChangeListener{ _, _ ->
            val selId = modeSelection.indexOfChild(
                findViewById(modeSelection.checkedRadioButtonId)
            )
            val mode = Mode.values()[selId] //?: Mode.DRAW
            myCanvasView.onModeChange(mode)
        }

    }

}

enum class Mode{
    DRAW, CLEAR, RECT
}

class MyCanvasView(
    context: Context
) : View(context) {

    private lateinit var extraCanvas: Canvas
    private lateinit var extraBitmap: Bitmap

    val rects = mutableListOf<RectF>()

    private val backgroundColor = ResourcesCompat.getColor(resources, R.color.teal_700, null)
    private val drawColor = ResourcesCompat.getColor(resources, R.color.purple_700, null)
    private val intersectionColor = ResourcesCompat.getColor(resources, R.color.red_700, null)

    private val paintDraw = paintCreator(drawColor)
    private val paintClear = paintCreator(backgroundColor)
    private val paintIntersection = paintCreator(intersectionColor)

    private var path = Path()

    var drawingMode: Mode = Mode.DRAW
    var paintChosen = paintDraw
    fun onModeChange(mode: Mode){
        drawingMode = mode
        when(mode){
            Mode.CLEAR -> paintChosen = paintClear
            else -> paintChosen = paintDraw
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (::extraBitmap.isInitialized) extraBitmap.recycle()
        extraBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        extraCanvas = Canvas(extraBitmap)
        extraCanvas.drawColor(backgroundColor)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(extraBitmap, 0f, 0f, null)
    }

    fun paintCreator(color_: Int) = Paint().apply {
        color = color_
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 12f
    }


    private var motionTouchEventX = 0f
    private var motionTouchEventY = 0f
    private var currentX = 0f
    private var currentY = 0f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        motionTouchEventX = event.x
        motionTouchEventY = event.y

        when(event.action){
            MotionEvent.ACTION_DOWN -> touchStart()
            MotionEvent.ACTION_MOVE -> touchMove()
            MotionEvent.ACTION_UP -> touchUp()
        }
        return true
    }

    private fun touchStart(){
        if(drawingMode == Mode.RECT){
            currentX = motionTouchEventX
            currentY = motionTouchEventY
            val rect = RectF(
                currentX -20f,
                currentY -20f,
                currentX +20f,
                currentY +20f
            )

            if(rects.any{
                RectF.intersects(it, rect)
                }) extraCanvas.drawRect(rect, paintIntersection)
            else extraCanvas.drawRect(rect, paintDraw)

            rects.add(rect)
            invalidate()

        } else{
            path.reset()
            path.moveTo(motionTouchEventX, motionTouchEventY)
            currentX = motionTouchEventX
            currentY = motionTouchEventY
        }

    }

    private val touchTolerance = ViewConfiguration.get(context).scaledTouchSlop
    private fun  touchMove(){
        if(drawingMode != Mode.RECT){
            val dx = Math.abs(motionTouchEventX - currentX)
            val dy = Math.abs(motionTouchEventY - currentY)
            if(dx >= touchTolerance || dy >= touchTolerance){

                path.quadTo(
                    currentX,
                    currentY,
                    (motionTouchEventX + currentX) /2,
                    (motionTouchEventY + currentY) /2
                )
                currentX = motionTouchEventX
                currentY = motionTouchEventY

                extraCanvas.drawPath(path, paintChosen)
            }
            invalidate()
        }

    }

    private fun touchUp(){
        if(drawingMode != Mode.RECT)
        path.reset()
    }

}