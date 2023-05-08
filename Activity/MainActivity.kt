package com.example.test

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import java.io.IOException
import kotlin.math.sqrt

class MainActivity : AppCompatActivity() {
    lateinit var button:Button
    lateinit var imgview:ImageView
    private var mScaleFactor = 1.0f
    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
        private const val ROTATE = 3
        private const val SCALE = 4
    }
    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt(x * x + y * y)
    }
    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }
    private fun getDistance(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return sqrt(dx * dx + dy * dy)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imgview=findViewById(R.id.img_view)

        val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imgview.setImageURI(uri)

                val wallpaperManager = WallpaperManager.getInstance(this)
                // 取得桌布尺寸
                val wallpaperWidth = wallpaperManager.desiredMinimumWidth
                val wallpaperHeight = wallpaperManager.desiredMinimumHeight

                val drawable = imgview.drawable
                try {
                    val bitmap = (drawable!! as BitmapDrawable).bitmap
                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, wallpaperWidth, wallpaperHeight, true)
                    wallpaperManager.setBitmap(scaledBitmap)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }

        button=findViewById(R.id.btn)
        button.setOnClickListener {
            imagePicker.launch("image/*")

        }


        var mode = NONE
        var mLastTouchX= 1.0f
        var mLastTouchY = 1.0f

        // 設置觸摸事件監聽器
        imgview.setOnTouchListener { _, event ->
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {

                    mLastTouchX=event.x
                    mLastTouchY=event.y
                    mode = DRAG
                    Log.d("QQ","one p")
                }
                MotionEvent.ACTION_POINTER_DOWN -> {

                    val distance = getDistance(event)
                    mScaleFactor = imgview.scaleX / distance
                    mode = ZOOM
                    Log.d("QQ","two p")
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mode == DRAG) {
                        Log.d("QQ","move one")
                        val translationX = event.x - mLastTouchX
                        val translationY = event.y - mLastTouchY
                        imgview.translationX += translationX
                        imgview.translationY += translationY

                    } else if (mode == ZOOM) {
                        Log.d("QQ","move two")

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

            true
        }

    }

}
