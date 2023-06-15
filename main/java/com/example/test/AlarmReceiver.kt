package com.example.test

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import java.io.IOException
import java.io.InputStream

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private var counter = 0
    }
    private lateinit var drawable:Drawable
    private var sec:Long=0
    private var wallpaperWidth:Int=0
    private var wallpaperHeight:Int=0
    private var dataList=mutableListOf<Uri>()
    private lateinit var wallpaperManager:WallpaperManager
    private lateinit var receivercontext:Context

    private  fun WallpaperShow(bitmap: Bitmap){
        try {
            val scaledBitmap =
                Bitmap.createScaledBitmap(bitmap, wallpaperWidth, wallpaperHeight, true)//clip
            wallpaperManager.setBitmap(scaledBitmap)//set
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    private  fun dowall(){
        counter=(counter%(dataList.size))//count 0~data length

        Log.d("OKK","$counter")

        /*uri to Drawable*/
        val items=dataList[counter]
        val inputStream: InputStream? = receivercontext.contentResolver.openInputStream(items)
        val yourDrawable = Drawable.createFromStream(inputStream, items.toString())

        /*set and check*/
        if(yourDrawable is Drawable){
            Log.d("OKK","YES")
            drawable=yourDrawable
        }

        WallpaperShow((drawable!! as BitmapDrawable).bitmap)
        counter++
    }
    override fun onReceive(context: Context, intent: Intent) {//first
        Log.d("alarm","start")
        sec= intent!!.getLongExtra("SECONDS",0) //time millisecond
        //if(sec<3){sec=3}
        wallpaperWidth =intent.getIntExtra("wallpaperWidth",0)//size
        wallpaperHeight =intent.getIntExtra("wallpaperHeight",0)
        val dataArray = intent.getParcelableArrayListExtra<Uri>("Uri")//uri list receive array
        dataList = dataArray!!.toMutableList()//array -> list
        receivercontext=context//for use
        wallpaperManager=WallpaperManager.getInstance(context)//set
        dowall()
    }
}