package com.example.test

import android.app.Activity
import android.app.WallpaperManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import java.io.IOException

class PreviewActivity : AppCompatActivity() {
    lateinit var previewImageView:ImageView
    lateinit var deleteButton:Button
    lateinit var setWallpaperButton:Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.preview_layout)

        // 获取传递的数据
        val uriString = intent.getStringExtra("key2")
        val uri = Uri.parse(uriString)

        // 设置预览图像
        previewImageView = findViewById<ImageView>(R.id.previewImageView)
        Glide.with(this)
            .load(uri)
            .into(previewImageView)

        // 设置删除按钮点击事件
        deleteButton = findViewById<Button>(R.id.deleteButton)
        deleteButton.setOnClickListener {
            // 关闭预览页面
            setResultAndFinish(uri.toString())
        }

        // 设置设置为桌面壁纸按钮点击事件
        setWallpaperButton = findViewById<Button>(R.id.wallpaperButton)
        setWallpaperButton.setOnClickListener {
            try {
                // 获取预览的图片资源的输入流
                val inputStream = contentResolver.openInputStream(uri)

                // 将图片设置为桌面壁纸
                val wallpaperManager = WallpaperManager.getInstance(this)
                wallpaperManager.setStream(inputStream)

                Toast.makeText(this, "壁纸设置成功", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Toast.makeText(this, "壁纸设置失败", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }

            finish()
        }

    }
    override fun onBackPressed() {
        finish()
    }
    private fun setResultAndFinish(uri: String) {
        val resultIntent = Intent()
        resultIntent.putExtra("resultKey", uri)
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
