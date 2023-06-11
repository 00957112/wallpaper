package com.example.test

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.icu.text.ListFormatter.Width
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.example.test.WallpaperTest.Companion.Companion.DRAG
import com.example.test.WallpaperTest.Companion.Companion.NONE
import com.example.test.WallpaperTest.Companion.Companion.ZOOM
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.io.InputStream
import kotlin.math.sqrt

class WallpaperTest: WallpaperService() {
    private var ischange: Boolean = false

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("hui123", "onCreate")
    }

    override fun onCreateEngine(): WallpaperService.Engine {
        Log.d("hui123", "onCreateEngine")
        if (ischange) {
            clearwallpaper()
            ischange = false
        }
        return WallpaperEngine()
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uriString = intent?.getStringExtra("key")
        val uri = uriString?.let { Uri.parse(it) }
        Log.d("hui123","onStartCommand")
        Log.d("hui123","unchange:${uri.toString()}")
        if(uri != null) {
            Log.d("hui123","make:${uri.toString()}")
            val itemsJson = Gson().toJson(uri.toString())
            val sharedPref = getSharedPreferences("record", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putString("items", itemsJson)
            ischange = true
            editor.apply()
        }
        Log.d("hui123","change:${uri.toString()}")
        return super.onStartCommand(intent, flags, startId)
    }
    private fun clearwallpaper() {
        val wallpaperManager = WallpaperManager.getInstance(this@WallpaperTest)
        try {
            wallpaperManager.clearWallpaper()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    inner class WallpaperEngine: WallpaperService.Engine(){
       private val frameDuration = 20
        private var holder: SurfaceHolder? = null
        //private val movie: Movie? = null
        private var gifDrawable: AnimatedImageDrawable? = null
        private var imgDrawable: Drawable? = null
        private var visible = false
        private var handler: Handler? = Handler(Looper.getMainLooper())
        private lateinit var gestureDetector: GestureDetector
        private var translateX=0f
        private var translateY=0f
        private var scaleX=1f
        private var mScaleFactor = 1.0f

        @RequiresApi(Build.VERSION_CODES.P)
        override fun onCreate(surfaceHolder: SurfaceHolder?) {

            Log.d("hui123","_onCreate")
            super.onCreate(surfaceHolder)

            //gestureDetector = GestureDetector(this@WallpaperTest, this@WallpaperEngine)
            //gestureDetector = GestureDetector(this@YourWallpaperService, this@YourWallpaperEngine)
            //gestureDetector.setOnDoubleTapListener(this@WallpaperEngine)


            val sharedPref = getSharedPreferences("record", Context.MODE_PRIVATE)
            val itemsJson = sharedPref.getString("items", null)
            var items:String?=null
            if (itemsJson != null)
                items = Gson().fromJson(itemsJson, object : TypeToken<String>() {}.type)
            Log.d("hui123","item:${items.toString()}")
            val itemsUri = Uri.parse(items)

            val source: InputStream? = contentResolver.openInputStream(itemsUri)
            val drawable = Drawable.createFromStream(source, items.toString())

            if (drawable is AnimatedImageDrawable) {
                Log.d("hui123","gifDrawable")
                gifDrawable = drawable
                gifDrawable!!.start()
            }else if (drawable is Drawable) {
                Log.d("hui123","imgDrawable")
                imgDrawable = drawable
            } else {
                // 文件不是图像也不是GIF
                // 处理其他类型的文件逻辑
            }
            holder = surfaceHolder
        }

        private val drawGIF = Runnable { draw() }
        private fun draw() {
            Log.d("wall","draw")

            if (visible) {
                val canvas: Canvas = holder!!.lockCanvas()
                canvas.save()

                if (gifDrawable != null) {
                    val gifWidth = gifDrawable!!.intrinsicWidth
                    val gifHeight = gifDrawable!!.intrinsicHeight
                    val scale = 3f//scaleX
                    canvas.scale(scale, scale)
                    canvas.translate(translateX*0.05f, translateY*0.05f)
                    gifDrawable?.draw(canvas)
                }else if (imgDrawable != null) {
                    val matrix = Matrix()
                    val scale = 0.5f//scaleX
                    matrix.setScale(scale, scale) // 設定縮放比例
                    matrix.postTranslate(translateX, translateY) // 設定位移
                    canvas.drawBitmap((imgDrawable!! as BitmapDrawable).bitmap,matrix,null)
                }
                canvas.restore()
                holder!!.unlockCanvasAndPost(canvas)
                //movie.setTime((System.currentTimeMillis() % movie.duration()) as Int)
                handler?.removeCallbacks(drawGIF)
                handler?.postDelayed(drawGIF, frameDuration.toLong())
            }
        }
        override fun onVisibilityChanged(visible: Boolean) {
            Log.d("wall","onVisibilityChanged")
            this.visible = visible
            if (visible) {
                handler!!.post(drawGIF)
            } else {
                handler!!.removeCallbacks(drawGIF)
            }
        }
        private fun getDistance(event: MotionEvent): Float {
            val dx = event.getX(0) - event.getX(1)
            val dy = event.getY(0) - event.getY(1)
            return sqrt(dx * dx + dy * dy)
        }
        override fun onDestroy() {
            Log.d("wall","onDestroy")
            super.onDestroy()
            handler!!.removeCallbacks(drawGIF)
        }
        var mode = NONE
        var mLastTouchX = 1.0f
        var mLastTouchY = 1.0f
        override fun onTouchEvent(event: MotionEvent) {
            // 在這裡處理觸碰事件
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    // 觸碰按下事件
                    Log.d("touch","!!!!!!!")
                    //gestureDetector.onTouchEvent(event)
                    mLastTouchX = event.x//手下
                    mLastTouchY = event.y
                    mode = DRAG
                }
                MotionEvent.ACTION_POINTER_DOWN->{
                    val distance = getDistance(event)
                    mScaleFactor = scaleX / distance
                    mode = ZOOM
                    Log.d("QQ", "two p")

                }

                MotionEvent.ACTION_MOVE -> {
                    // 觸碰移動事件
                    if (mode == DRAG) {
                        Log.d("QQ", "move one")
                        val translationX = event.x - mLastTouchX//過程
                        val translationY = event.y - mLastTouchY
                        translateX += translationX
                        translateY += translationY
                        mLastTouchX = event.x
                        mLastTouchY = event.y

                    } else if (mode == ZOOM) {
                        Log.d("QQ", "move two")

                        val distance = getDistance(event)

                        val newScaleFactor = distance * mScaleFactor
                        scaleX = newScaleFactor
                    }
                }
                MotionEvent.ACTION_UP -> {
                    // 觸碰抬起事件
                    mode = NONE
                    mScaleFactor = 1.0f
                }
            }

        }

    }
}
