package com.example.test

import android.app.Service
import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.IOException
import java.io.InputStream



import java.util.*

// 后台服务类
class TimerService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var counter = 0
    private lateinit var drawable:Drawable
    private var sec:Long=0

    private var wallpaperWidth:Int=0
    private var wallpaperHeight:Int=0
    private var dataList=mutableListOf<Uri>()
    private lateinit var timer: Timer

    override fun onCreate() {
        super.onCreate()
        // 创建定时器
        timer = Timer()

    }
    private val timerTask = object : TimerTask() {
        override fun run() {
            // 在此处编写每秒执行的任务
            // 注意：如果需要更新UI，确保在主线程中进行操作
            // 在这个示例中，我们通过Log打印计数器的值
            dowall()
            //Log.d("BackgroundService", "Counter: $counter")
        }
    }

    private  fun WallpaperShow(bitmap: Bitmap){
        try {
            val scaledBitmap =
                Bitmap.createScaledBitmap(bitmap, wallpaperWidth, wallpaperHeight, true)
            WallpaperManager.getInstance(this@TimerService).setBitmap(scaledBitmap)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    private  fun dowall(){
        // 在此处编写定时执行的任务
        counter++
        counter=counter%(dataList.size)

        Log.d("OKK","$counter")

        val items=dataList[counter]
        val inputStream: InputStream? = contentResolver.openInputStream(items)
        val yourDrawable = Drawable.createFromStream(inputStream, items.toString())

        if(yourDrawable is Drawable){
            Log.d("OKK","YES")
            drawable=yourDrawable
        }
        //顯示桌布
        WallpaperShow((drawable!! as BitmapDrawable).bitmap)

        // 继续下一次定时执行任务
        //handler.postDelayed(this, sec * 1000)
    }
//    private val runnable = object : Runnable {
//        override fun run() {
//            // 在此处编写定时执行的任务
//            counter++
//            // 更新UI或执行其他操作
//            // 注意：如果需要更新UI，确保在主线程中进行操作
//            // 在这个示例中，我们通过Log打印计数器的值
//            println("Counter: $counter")
//
//
//            val items=dataList[counter%(dataList.size)]
//            val inputStream: InputStream? = contentResolver.openInputStream(items)
//            val yourDrawable = Drawable.createFromStream(inputStream, items.toString())
//
//            if(yourDrawable is Drawable){
//                Log.d("OKK","YES")
//                drawable=yourDrawable
//            }
//            //顯示桌布
//            WallpaperShow((drawable!! as BitmapDrawable).bitmap)
//
//            // 继续下一次定时执行任务
//            handler.postDelayed(this, sec * 1000)
//        }
    //}
    private var isTimerRunning = false

    override fun onBind(intent: Intent): IBinder? {
        // 不使用绑定方式，返回 null
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sec= intent!!.getLongExtra("SECONDS",0)
        if(sec<3){sec=3}
        wallpaperWidth =intent.getIntExtra("wallpaperWidth",0)
        wallpaperHeight =intent.getIntExtra("wallpaperHeight",0)
        val dataArray = intent.getParcelableArrayListExtra<Uri>("Uri")
        dataList = dataArray!!.toMutableList()
// 启动定时任务，每秒执行一次
        timer.scheduleAtFixedRate(timerTask, 0, sec*1000)
        startTimer()
        return START_STICKY
    }

    override fun onDestroy() {
        stopTimer()
//// 销毁定时器
//        timer.cancel()
        super.onDestroy()
    }

    private fun startTimer() {
        if (!isTimerRunning) {
            Log.d("work","start")
            isTimerRunning = true
        }
    }

    private fun stopTimer() {
        if (isTimerRunning) {
           // handler.removeCallbacks(runnable)
            isTimerRunning = false
        }
    }

}