package com.example.test

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import java.io.File
import java.io.FileOutputStream
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


class CropActivity : AppCompatActivity() {
    private lateinit var imageUri: Uri
    private lateinit var backView: View
    private lateinit var imageView: ImageView
    private lateinit var frameView: View
    private lateinit var colorButton : Button
    private lateinit var saveButton: Button
    private lateinit var seekBar : SeekBar
    private lateinit var frameLayout : FrameLayout
    private var screenWidth:Int=0
    private var screenHeight:Int=0

    private var scaleFactor = 1.0f
    private var initialDistance = 0.0f
    private var rotationAngle = 0f // 保存图片的旋转角度

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop)
        // 获取传递过来的图片 Uri

        backView = findViewById(R.id.backView)
        frameView = findViewById(R.id.frameView)

        val layoutParams = frameView.layoutParams
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels
        layoutParams.width = ((screenWidth.toFloat()*0.75).toInt())
        layoutParams.height =((screenHeight.toFloat()*0.75).toInt())
        frameView.layoutParams = layoutParams
        backView.layoutParams = layoutParams

        // 在这里实现图片裁剪逻辑

        // 加载图片到 ImageView
        imageView = findViewById(R.id.imageView)
        imageUri = Uri.parse(intent.getStringExtra("crop"))
        Glide.with(this)
            .load(imageUri)
            .into(imageView)
        // 设置图片的触摸事件
        imageView.setOnTouchListener { _, event ->
            handleTouchEvent(event)
            true
        }
        seekBar = findViewById<SeekBar>(R.id.dragAxisSeekBar)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                rotationAngle  = (progress - 180).toFloat() // 计算角度

                imageView.rotation = rotationAngle // 应用旋转角度到图片
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // 开始拖动拖动条时的操作
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // 停止拖动拖动条时的操作
            }
        })
        frameLayout = findViewById(R.id.frameLayout)
        colorButton = findViewById(R.id.colorButton)
        var currentColor:Int = 0
        colorButton.setOnClickListener {
            val rgbaPicker = RGBAPickerDialogFragment(currentColor)
            rgbaPicker.setOnColorChangedListener { color ->
                currentColor = color
                Log.d("nowcolor","$currentColor")
                backView.setBackgroundColor(color)
            }
            rgbaPicker.show(supportFragmentManager, "RGBAPickerDialog")
        }


        saveButton = findViewById(R.id.saveButton)
        saveButton.setOnClickListener {
            saveImage()
        }
    }
    var mode = NONE
    private var previousX = 1.0f
    private var previousY = 1.0f
    private fun handleTouchEvent(event: MotionEvent) {
        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                previousX = event.x
                previousY = event.y
                mode = DRAG
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                val distance = getDistance(event)
                scaleFactor = imageView.scaleX / distance
                initialDistance = distance
                mode = ZOOM
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    val currentX = event.x
                    val currentY = event.y
                    val transformedDeltaX = (currentX - previousX) * cos(Math.toRadians(-imageView.rotation.toDouble())) +
                            (currentY - previousY) * sin(Math.toRadians(-imageView.rotation.toDouble()))
                    val transformedDeltaY = -(currentX - previousX) * sin(Math.toRadians(-imageView.rotation.toDouble())) +
                            (currentY - previousY) * cos(Math.toRadians(-imageView.rotation.toDouble()))
                    imageView.translationX += transformedDeltaX.toFloat() * imageView.scaleX
                    imageView.translationY += transformedDeltaY.toFloat() * imageView.scaleY
                } else if (mode == ZOOM) {
                    val distance = getDistance(event)
                    val newScaleFactor = imageView.scaleX * (distance / initialDistance)
                    imageView.scaleX = newScaleFactor
                    imageView.scaleY = newScaleFactor
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
                scaleFactor = 1.0f
            }
        }
    }

    private fun getDistance(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return sqrt(dx * dx + dy * dy)
    }

    private fun saveImage() {
        // 获取 FrameLayout 的位置信息
        val frameLayoutLocation = IntArray(2)
        frameLayout.getLocationInWindow(frameLayoutLocation)

        // 获取 FrameView 在 FrameLayout 中的位置信息
        val frameViewLocation = IntArray(2)
        frameView.getLocationInWindow(frameViewLocation)

        // 计算 FrameView 相对于 FrameLayout 的位置
        val frameViewX = frameViewLocation[0] - frameLayoutLocation[0]
        val frameViewY = frameViewLocation[1] - frameLayoutLocation[1]

        // 创建一个与 FrameView 相同大小的 Bitmap
        val bitmap = Bitmap.createBitmap(frameView.width, frameView.height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.TRANSPARENT)

        // 创建一个 Canvas，并将其与 Bitmap 关联
        val canvas = Canvas(bitmap)

        // 将 FrameLayout 上被 FrameView 框住的区域绘制到 Bitmap 上
        canvas.translate(-frameViewX.toFloat(), -frameViewY.toFloat())
        val frameViewDrawable = ContextCompat.getDrawable(this, R.drawable.rectangle_frame)
        frameView.foreground = null
        frameLayout.draw(canvas)
        frameView.foreground = frameViewDrawable

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, screenWidth, screenHeight, true)


        val internalStorageDir = this.filesDir
        val imageDir = File(internalStorageDir, "images")
        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }
        val fileName = generateRandomFileName() // 生成随机文件名
        val imageFile = File(imageDir, "$fileName.png") // 创建图像文件
        val outputStream = FileOutputStream(imageFile) // 创建文件输出流

        // 将 Bitmap 保存到文件输出流
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)

        outputStream.close() // 关闭文件输出流

        Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show()
        val imageUri = Uri.fromFile(imageFile)
        val intent = Intent()
        intent.putExtra("resulturi", imageUri.toString()) // 将保存的 Uri 名称作为额外数据放入 Intent
        setResult(Activity.RESULT_OK, intent) // 设置结果为 RESULT_OK
        // 关闭 CropActivity
        finish()

    }
    private fun generateRandomFileName(): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

        return (1..10)
            .map { Random().nextInt(charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }



}


