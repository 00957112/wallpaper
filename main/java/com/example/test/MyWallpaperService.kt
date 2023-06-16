package com.example.test

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStream
import kotlin.math.sqrt


class MyWallpaperService: WallpaperService() {
    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }
    override fun onCreateEngine(): WallpaperService.Engine {
        Log.d("paperwall","onCreateEngine")
        return WallpaperEngine()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val data = intent?.getStringExtra("key")
        //Log.d("paperwall","onStartCommand")
        //Log.d("string","${data}")

        val sharedPref = getSharedPreferences("record", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putString("items", Gson().toJson(data)) // 將 items 資料寫入
        editor.apply() // 提交資料變更

        // 在這裡使用資料進行相應的操作
        return super.onStartCommand(intent, flags, startId)
    }
    inner class WallpaperEngine: WallpaperService.Engine(){
        private val frameDuration = 20
        private var holder: SurfaceHolder? = null
        //private val movie: Movie? = null
        private var imgOrgif=false
        private var gifDrawable: AnimatedImageDrawable? = null
        private var drawable: Drawable? = null
        private var visible = false
        private var handler: Handler? = null
        //private lateinit var gestureDetector: GestureDetector
        private var translateX=0f
        private var translateY=0f
        private var scaleX=1f
        private var mScaleFactor = 1.0f

        @RequiresApi(Build.VERSION_CODES.P)
        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            //Log.d("paperwall","onCreate")
            super.onCreate(surfaceHolder)

            val sharedPref = getSharedPreferences("record", Context.MODE_PRIVATE)
            val itemsJson = sharedPref.getString("items", null)
            var items:String?=null
            if (itemsJson != null)
                items = Gson().fromJson(itemsJson, object : TypeToken<String>() {}.type)
            //Log.d("uri","${items}")


            val inputStream: InputStream? = contentResolver.openInputStream(Uri.parse(items))
            val yourDrawable = Drawable.createFromStream(inputStream, items.toString())

            if (yourDrawable is AnimatedImageDrawable) {
                //Log.d("OKK","YESSSSSSSSSSSSSS")
                imgOrgif=false
                gifDrawable?.stop() // 停止播放 GIF 圖片
                gifDrawable=yourDrawable
                gifDrawable!!.start()
            }else if(yourDrawable is Drawable){
                //Log.d("OKK","YES")
                imgOrgif=true
                drawable=yourDrawable
            }
            //Log.d("paperwall","imgOrgif:${imgOrgif}")
            holder = surfaceHolder
            handler=Handler(Looper.getMainLooper())
        }

        private val drawGIF = Runnable { draw() }
        private fun draw() {
            //Log.d("paperwall","draw")
           // if (visible) {

                val canvas: Canvas = holder!!.lockCanvas()
            try {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR) // 清除畫布內容
                canvas.save()

                if(imgOrgif){
                    val matrix = Matrix()
                    val scale = 0.5f//scaleX
                    matrix.setScale(scale, scale) // 設定縮放比例
                    matrix.postTranslate(translateX, translateY) // 設定位移
                    canvas.drawBitmap((drawable!! as BitmapDrawable).bitmap,matrix,null)
                    //Log.d("change",drawable.toString())
                }else{
                    val gifWidth = gifDrawable!!.intrinsicWidth
                    val gifHeight = gifDrawable!!.intrinsicHeight
                    val scale = 3f//scaleX
                    canvas.scale(scale, scale)
                    canvas.translate(translateX*0.05f, translateY*0.05f)
                    gifDrawable?.draw(canvas)
                   // Log.d("change",gifDrawable.toString())
                }

                canvas.restore()
            } catch (e: Exception) {
                e.printStackTrace()
            }finally {
                //Log.d("canvas",canvas.toString())
                holder!!.unlockCanvasAndPost(canvas)
                //Log.d("canvas",canvas.toString())
            }

                handler?.removeCallbacks(drawGIF)
                handler?.postDelayed(drawGIF, frameDuration.toLong())

        }

        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            //Log.d("wall","onSurfaceChanged")
            handler!!.removeCallbacks(drawGIF)
            super.onSurfaceChanged(holder, format, width, height)
        }
        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            //Log.d("canvas","onSurfaceDestroyed")

        }
        override fun onVisibilityChanged(visible: Boolean) {
            //Log.d("wall","onVisibilityChanged")
            this.visible = visible
            if (visible) {
                handler!!.post(drawGIF)
            } else {
                //Log.d("wall","preview")
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
