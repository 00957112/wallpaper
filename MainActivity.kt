package com.example.test

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {
    
    lateinit var button:Button
    lateinit var imgview:ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imgview=findViewById(R.id.img_view)
        /*用按鈕選擇照片顯示*/
        val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                imgview.setImageURI(uri)
            }
        }

        button=findViewById(R.id.btn)
        button.setOnClickListener {
            imagePicker.launch("image/*")
        }


}
}
