package com.example.test

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.result.contract.ActivityResultContracts
import java.io.IOException
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {
    lateinit var button:Button
    lateinit var imgview:ImageView
    lateinit var wallView:ImageView
    private var cropWidth:Int=0
    private var cropHeight:Int=0
    private var mScaleFactor = 1.0f

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
        private const val ROTATE = 3
        private const val SCALE = 4
    }
    private fun getDistance(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return sqrt(dx * dx + dy * dy)
    }
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imgview = findViewById(R.id.img_view)
        wallView=findViewById(R.id.wallView)
        // cropView=findViewById(R.id.wallView)
        // 在相应位置添加自定义 View
        //val container = findViewById<RelativeLayout>(R.id.container)
        //container.addView(cropView)

        val imagePicker =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {
                    imgview.setImageURI(uri)

                    val wallpaperManager = WallpaperManager.getInstance(this)
                   /* // 取得桌布尺寸
                    val wallpaperWidth = wallpaperManager.desiredMinimumWidth
                    val wallpaperHeight = wallpaperManager.desiredMinimumHeight*/

                    //給框框 wallView大小隨桌布大小
                    val displayMetrics = DisplayMetrics()
                    windowManager.defaultDisplay.getMetrics(displayMetrics)
                    val wallpaperWidth = displayMetrics.widthPixels
                    val wallpaperHeight = displayMetrics.heightPixels
                    cropWidth = ((wallpaperWidth.toFloat()*0.75).toInt())
                    cropHeight = ((wallpaperHeight.toFloat()*0.75).toInt())
                    val params = wallView.layoutParams
                    params.width = cropWidth
                    params.height = cropHeight
                    Log.d("xxx","${wallpaperWidth},${wallpaperHeight}\n${cropWidth},${cropHeight}")
                    wallView.layoutParams = params
                    wallView.requestLayout()

                    //顯示桌布
                    val drawable = imgview.drawable
                    imgview.adjustViewBounds = true
                    try {
                        val bitmap = (drawable!! as BitmapDrawable).bitmap
                        val scaledBitmap =
                            Bitmap.createScaledBitmap(bitmap, wallpaperWidth, wallpaperHeight, true)
                        wallpaperManager.setBitmap(scaledBitmap)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    wallView.adjustViewBounds = true
                    val location = IntArray(2)
                    wallView.getLocationInWindow(location)

                    val x = location[0]
                    val y = location[1]
                    Log.d("size","${x},${y}")
                    //Log.d("first","${imgview.getLeft()},${imgview.getTop() }")
                    Log.d("view","${wallView.width},${wallView.height}")

                }
            }

        button = findViewById(R.id.btn)
        button.setOnClickListener {
            imagePicker.launch("image/*")

        }


        var mode = NONE
        var mLastTouchX = 1.0f
        var mLastTouchY = 1.0f

        // 設置觸摸事件監聽器
        imgview.setOnTouchListener { _, event ->
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {

                    mLastTouchX = event.x
                    mLastTouchY = event.y
                    mode = DRAG
                    Log.d("QQ", "one p")
                }
                MotionEvent.ACTION_POINTER_DOWN -> {

                    val distance = getDistance(event)
                    mScaleFactor = imgview.scaleX / distance
                    mode = ZOOM
                    Log.d("QQ", "two p")
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mode == DRAG) {
                        Log.d("QQ", "move one")
                        val translationX = event.x - mLastTouchX
                        val translationY = event.y - mLastTouchY
                        imgview.translationX += translationX
                        imgview.translationY += translationY

                    } else if (mode == ZOOM) {
                        Log.d("QQ", "move two")

                        val distance = getDistance(event)

                        val newScaleFactor = distance * mScaleFactor
                        imgview.scaleX = newScaleFactor
                        imgview.scaleY = newScaleFactor
                    }

                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    mode = NONE
                    mScaleFactor = 1.0f
                }
            }
            val location = IntArray(2)
            imgview.getLocationInWindow(location)

            val x = location[0]
            val y = location[1]
            Log.d("size","${x},${y}")
            //Log.d("first","${imgview.getLeft()},${imgview.getTop() }")
            Log.d("view","${imgview.width},${imgview.height}")
            //imgview.getLeft() imgview.getTop() //不知道是甚麼值 好像是初始時的左和上
            true
        }

        // 获取 ImageView 上的 Drawable 对象
        val drawable = imgview.drawable


    }
/*
// 获取图片的宽度和高度
        val imageWidth = drawable.intrinsicWidth
        val imageHeight = drawable.intrinsicHeight

// 获取 ImageView 的宽度和高度
        val viewWidth = imgview.width
        val viewHeight = imgview.height

// 计算缩放比例和位移距离
        val scaleFactor = viewWidth.toFloat() / imageWidth.toFloat()
        val dx = (viewWidth - imageWidth * scaleFactor) / 2
        val dy = (viewHeight - imageHeight * scaleFactor) / 2

// 创建一个 Matrix 对象，设置缩放比例和位移距离
        val matrix = Matrix()
        matrix.setScale(scaleFactor, scaleFactor)
        matrix.postTranslate(dx, dy)

// 创建一个 Bitmap 对象，设置宽度、高度和颜色格式等参数
        val bitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)

// 创建一个 Canvas 对象，将画布与 Bitmap 对象关联
        val canvas = Canvas(bitmap)

// 在 Canvas 上设置裁切区域
        val clipRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        canvas.clipRect(clipRect)

// 在 Canvas 上绘制缩放和位移后的图片
        canvas.drawBitmap((drawable as BitmapDrawable).bitmap, matrix, null)

        //val cropLeft = cropView.left
        //val cropTop = cropView.top

// 返回裁切后的 Bitmap 对象
        //val croppedBitmap = Bitmap.createBitmap(bitmap, cropLeft, cropTop, cropWidth, cropHeight)
    }
    inner class CropView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

        private val strokePaint = Paint().apply {
            color = Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }

        private val rect = Rect()

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            // 计算外框的位置和大小
            rect.left = paddingLeft
            rect.top = paddingTop
            rect.right = width - paddingRight
            rect.bottom = height - paddingBottom

            // 绘制矩形
            canvas.drawRect(rect, strokePaint)
        }
    }*/
}
