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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.io.InputStream

class ScreenStatusReceiver : BroadcastReceiver() {
    private  fun WallpaperShow(bitmap: Bitmap){
        try {
            val scaledBitmap =
                Bitmap.createScaledBitmap(bitmap, wallpaperWidth, wallpaperHeight, true)//clip
            WallpaperManager.getInstance(receivercontext).setBitmap(scaledBitmap)//set
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    private  fun dowall(){
        ScreenStatusReceiver.counter =(ScreenStatusReceiver.counter %(uriList.size))//count 0~data length

        //Log.d("OKK","${ScreenStatusReceiver.counter}")

        /*uri to Drawable*/
        val items=uriList[ScreenStatusReceiver.counter]
        val inputStream: InputStream? = receivercontext.contentResolver.openInputStream(Uri.parse(items))
        val yourDrawable = Drawable.createFromStream(inputStream, items)

        /*set and check*/
        if(yourDrawable is Drawable){
            //Log.d("OKK","YES")
            drawable=yourDrawable
        }

        WallpaperShow((drawable!! as BitmapDrawable).bitmap)
        ScreenStatusReceiver.counter++
    }
    override fun onReceive(context: Context, intent: Intent) {
        receivercontext=context

        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                // 屏幕关闭时的逻辑处理
                wasScreenOn = false
                //Log.e("screen","off")
            }
            Intent.ACTION_SCREEN_ON -> {
                // 屏幕开启时的逻辑处理
                wasScreenOn = true
                //Log.e("screen","on")

                val sharedPref = context.getSharedPreferences("screen", Context.MODE_PRIVATE)
                var itemsJson = sharedPref.getString("items", null)
                if (itemsJson != null)
                    uriList = Gson().fromJson(itemsJson, object : TypeToken<MutableList<String>>() {}.type)
                itemsJson = sharedPref.getString("wallpaperWidth", null)
                if (itemsJson != null)
                    wallpaperWidth = Gson().fromJson(itemsJson, object : TypeToken<Int>() {}.type)
                itemsJson = sharedPref.getString("wallpaperHeight", null)
                if (itemsJson != null)
                    wallpaperHeight = Gson().fromJson(itemsJson, object : TypeToken<Int>() {}.type)
                Log.e("??", uriList.toString())
                dowall()

            }
        }
    }

    companion object {
        var counter=0
        var wasScreenOn = true
        var uriList: MutableList<String> = mutableListOf()
        lateinit var drawable:Drawable
        private var wallpaperWidth:Int=0
        private var wallpaperHeight:Int=0
        private lateinit var receivercontext:Context

    }

}