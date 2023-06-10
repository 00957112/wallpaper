package com.example.test

import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import androidx.annotation.RequiresApi
import java.lang.Float.min


class WallpaperTest: WallpaperService() {
    override fun onCreateEngine(): WallpaperService.Engine {
        Log.d("paperwall","onCreateEngine")
        return WallpaperEngine()
    }
    inner class WallpaperEngine: WallpaperService.Engine(){
        private val frameDuration = 20
        private var holder: SurfaceHolder? = null
        //private val movie: Movie? = null
        private var gifDrawable: AnimatedImageDrawable? = null
        private var visible = false
        private var handler: Handler? = Handler(Looper.getMainLooper())
        private lateinit var gestureDetector: GestureDetector

        @RequiresApi(Build.VERSION_CODES.P)
        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            Log.d("paperwall","onCreate")
            super.onCreate(surfaceHolder)

            //gestureDetector = GestureDetector(this@WallpaperTest, this@WallpaperEngine)
            //gestureDetector = GestureDetector(this@YourWallpaperService, this@YourWallpaperEngine)
            //gestureDetector.setOnDoubleTapListener(this@WallpaperEngine)


            val source = ImageDecoder.createSource(resources,R.drawable.th)
            val drawable = ImageDecoder.decodeDrawable(source)

            if (drawable is AnimatedImageDrawable) {
                gifDrawable=drawable
                gifDrawable!!.start()
            }
            holder = surfaceHolder
        }

        private val drawGIF = Runnable { draw() }
        private fun draw() {
            Log.d("paperwall","draw")
            if (visible) {
                val canvas: Canvas = holder!!.lockCanvas()
                canvas.save()
                // Adjust size and position so that
                // the image looks good on your screen
                val gifWidth = gifDrawable!!.intrinsicWidth
                val gifHeight = gifDrawable!!.intrinsicHeight
                val scale = 3f
                val offsetX = (canvas.width - gifWidth * scale) / 5
                val offsetY = (canvas.height - gifHeight * scale) / 5
                canvas.scale(scale, scale)
                canvas.translate(offsetX, offsetY)

/*

                val centerX = (canvas.width - gifDrawable!!.intrinsicWidth) / 2
                val centerY = (canvas.height - gifDrawable!!.intrinsicHeight) / 2

                // 將畫布平移至置中位置
                canvas.translate(centerX.toFloat(), centerY.toFloat())
                canvas.scale(3f, 3f)*/


                //movie.draw(canvas, -100, 0)
                gifDrawable?.draw(canvas)
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
        override fun onDestroy() {
            Log.d("wall","onDestroy")
            super.onDestroy()
            handler!!.removeCallbacks(drawGIF)
        }

        override fun onTouchEvent(event: MotionEvent) {
            // 在這裡處理觸碰事件
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 觸碰按下事件
                   // Log.d("touch","!!!!!!!")
                    //gestureDetector.onTouchEvent(event)
                }
                MotionEvent.ACTION_POINTER_DOWN->{
                    Log.d("touch","!!!!!!!")

                }

                MotionEvent.ACTION_MOVE -> {
                    // 觸碰移動事件
                }
                MotionEvent.ACTION_UP -> {
                    // 觸碰抬起事件
                }
            }
        }

    }
}
