package com.example.test

import android.annotation.SuppressLint
import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
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
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {//主頁面
    lateinit var button:Button
    lateinit var button2:Button
    lateinit var imgview:ImageView
    lateinit var wallView:ImageView
    lateinit var wallpaperManager:WallpaperManager
    private var cropWidth:Int=0
    private var cropHeight:Int=0
    private var wallpaperWidth:Int=0
    private var wallpaperHeight:Int=0
    private var mScaleFactor = 1.0f

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }
    private fun getDistance(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return sqrt(dx * dx + dy * dy)
    }

    private  fun WallpaperShow(bitmap:Bitmap){
        try {
            val scaledBitmap =
                Bitmap.createScaledBitmap(bitmap, wallpaperWidth, wallpaperHeight, true)
            wallpaperManager.setBitmap(scaledBitmap)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imgview = findViewById(R.id.img_view)
        wallView=findViewById(R.id.wallView)

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        wallpaperWidth = displayMetrics.widthPixels
        wallpaperHeight = displayMetrics.heightPixels
        cropWidth = ((wallpaperWidth.toFloat()*0.75).toInt())
        cropHeight = ((wallpaperHeight.toFloat()*0.75).toInt())
        wallpaperManager = WallpaperManager.getInstance(this)

        val imagePicker =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {//開圖庫取圖片
                    imgview.setImageURI(uri)

                    //給框框 wallView大小隨桌布大小
                    val params = wallView.layoutParams//為了改大小要先取出
                    params.width = cropWidth
                    params.height = cropHeight
                    Log.d("xxx","${wallpaperWidth},${wallpaperHeight}\n${cropWidth},${cropHeight}")
                    wallView.layoutParams = params
                    wallView.requestLayout()


                    //顯示桌布
                    val drawable = imgview.drawable
                    imgview.adjustViewBounds = true
                    WallpaperShow((drawable!! as BitmapDrawable).bitmap)
                }
            }

        button = findViewById(R.id.btn)
        button.setOnClickListener {
            imagePicker.launch("image/*")//按鈕取圖庫照片
        }
        button2 = findViewById(R.id.btn2)

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
            val location2 = IntArray(2)
            wallView.getLocationOnScreen(location2)

            val xx = location[0]
            val yy = location[1]
            Log.d("wallsize","${xx},${yy}")//左上角
            //Log.d("first","${imgview.getLeft()},${imgview.getTop() }")
            Log.d("wallview","${wallView.width},${wallView.height}")
            true
        }//touch

        button2.setOnClickListener {
            // 計算出裁剪區域
            Log.d("QQQQ","set")
            val location2 = IntArray(2)
            wallView.getLocationOnScreen(location2)
            val wallLeft = location2[0]
            val wallTop = location2[1]
            val clipRect = Rect(wallLeft, wallTop, wallLeft + wallView.width, wallTop + wallView.height)
            Log.d("getp","左上角:${clipRect.left},${clipRect.top}")
            Log.d("getp","寬高:${ clipRect.width()},${clipRect.height()}")
            Log.d("getp","右下角:${wallLeft + wallView.width},${wallTop + wallView.height}")
            val relativeLayout = findViewById<RelativeLayout>(R.id.container)
            // 開啟 drawingCache
            relativeLayout.isDrawingCacheEnabled = true
            relativeLayout.buildDrawingCache()

            try {
                //擷取畫面
                val pic=relativeLayout.drawingCache
                if(pic==null) Log.d("QQQQ","nono")
                val location3 = IntArray(2)//取偏差值
                button2.getLocationOnScreen(location3)
                val btntop = location3[1]
                if(pic!=null) {
                    Log.d("QQQQ","setto")
                    val bitmap = Bitmap.createBitmap(pic)
                    relativeLayout.isDrawingCacheEnabled = false
                    // 裁剪出要保存的區域
                    var statusBarHeight=btntop //位置調校
                    Log.d("QQQQ","上面區:${clipRect.top}-${statusBarHeight}->${clipRect.top-statusBarHeight}")
                    val croppedBitmap = Bitmap.createBitmap(bitmap, clipRect.left, clipRect.top-statusBarHeight, clipRect.width(), clipRect.height())
                    Log.d("QQQQ","切圖:${croppedBitmap.width},${croppedBitmap.height}")
                    WallpaperShow(croppedBitmap)
                }
            }catch (e: IOException) {
                e.printStackTrace()
            }

        }//btn2



    }//oncreate




}
