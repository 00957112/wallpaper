package com.example.wallpaper
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Bitmap.CompressFormat

class ListViewFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var fabSelectImage: FloatingActionButton
    private val JSON_FILE_NAME = "ImageUris.json"
    private var ImageUris: MutableList<String> = mutableListOf()// 创建一个空的可变列表
    private lateinit var jsonFile: String
    companion object {
        private const val REQUEST_CODE_CROP = 1
    }
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // 将需要上下文的方法移动到这里
        getJsonFilePath()
        ImageUris = readImageUris()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_list, container, false)
        val dataList = listOf(
            Uri.parse("android.resource://com.example.wallpaper/raw/duck"),
            Uri.parse("android.resource://com.example.wallpaper/raw/sky"),
            // 添加更多的数据 Uri...
        )
        recyclerView = view.findViewById(R.id.recyclerView)
        val adapter = YourAdapter(ImageUris)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        adapter.setOnItemClickListener(object : YourAdapter.OnItemClickListener {
            override fun onItemClick(uri: Uri) {
                // 打开预览页面
                val intent = Intent(requireContext(), PreviewActivity::class.java)
                intent.putExtra("key2", uri.toString())
                startActivity(intent)
            }
        })

        fabSelectImage = view.findViewById(R.id.fabSelectImage)
        fabSelectImage.setOnClickListener {
            imagePicker.launch("image/*")
        }


        return view
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CROP && resultCode == Activity.RESULT_OK) {
            val uri = data?.getStringExtra("resulturi")
            if (uri != null) {
                ImageUris.add(uri)
            }
        }
    }

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            // 启动新的页面进行裁剪操作
            val cropIntent = Intent(requireActivity(), CropActivity::class.java)
            cropIntent.putExtra("crop", uri.toString())
            startActivityForResult(cropIntent, REQUEST_CODE_CROP)

            val intent = Intent(requireActivity(), CropActivity::class.java)
            intent.putExtra("crop", uri.toString())
            startActivity(intent)
        }
    }



    class YourAdapter(private val dataList: MutableList<String>) : RecyclerView.Adapter<YourAdapter.ViewHolder>() {

        interface OnItemClickListener {
            fun onItemClick(uri: Uri)
        }

        private var onItemClickListener: OnItemClickListener? = null

        fun setOnItemClickListener(listener: OnItemClickListener) {
            onItemClickListener = listener
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.list_item, parent, false)
            return ViewHolder(itemView)
        }


        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val uri = dataList[position]
            // 使用Glide加载和显示图片和GIF
            Glide.with(holder.itemView)
                .load(uri)
                .into(holder.imageView)


            // 获取屏幕宽度
            val displayMetrics = holder.imageView.context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels

            // 计算正方形的宽度
            val squareSize = screenWidth / 2

            // 设置ImageView的宽高为正方形大小
            val layoutParams = holder.imageView.layoutParams
            layoutParams.width = squareSize
            layoutParams.height = squareSize
            holder.imageView.layoutParams = layoutParams



            // 设置项的点击事件监听器
            holder.itemView.setOnClickListener {
                Log.d("gotopage", uri.toString())
                onItemClickListener?.onItemClick(Uri.parse(uri))
            }
        }

        override fun getItemCount(): Int {
            return dataList.size
        }

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val imageView: ImageView = itemView.findViewById(R.id.imageView)
        }
    }
    // 读取 JSON 文件并返回图片 URI 列表
    private fun readImageUris(): MutableList<String> {
        val jsonFile = File(getJsonFilePath())
        val imageUris = mutableListOf<String>()

        if (jsonFile.exists()) {
            try {
                val json = FileInputStream(jsonFile).bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(json)

                for (i in 0 until jsonArray.length()) {
                    val imageUri = jsonArray.getString(i)
                    imageUris.add(imageUri)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // JSON 文件不存在，写入默认的图片 URI
            writeImageUris()
            // 创建默认的白色图片并保存
            saveDefaultImage()
        }

        return imageUris
    }

    // 将图片 URI 列表写入 JSON 文件
    private fun writeImageUris() {
        try {
            val jsonFile = File(getJsonFilePath())
            val jsonArray = JSONArray()


            FileOutputStream(jsonFile).use { fileOutputStream ->
                fileOutputStream.write(jsonArray.toString().toByteArray())
                fileOutputStream.flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 获取 JSON 文件的完整路径
    private fun getJsonFilePath(): String {
        val internalStorageDir = requireContext().filesDir
        return File(internalStorageDir, JSON_FILE_NAME).absolutePath
    }

    // 创建并保存默认的白色图片
    private fun saveDefaultImage() {
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        val bitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val imageDir = File(requireContext().filesDir, "images")
        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }

        val imageFile = File(imageDir, "default.png")
        val outputStream = FileOutputStream(imageFile)
        bitmap.compress(CompressFormat.PNG, 100, outputStream)
        outputStream.close()
    }
}

