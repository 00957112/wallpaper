package com.example.test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener
import java.io.IOException
import kotlin.math.sqrt
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import android.widget.Toast

class HomeFragment : Fragment() {
    lateinit var button:Button
    lateinit var button2:Button
    lateinit var button3:Button
    lateinit var imgview:ImageView
    lateinit var wallView:ImageView

    lateinit var wallpaperManager:WallpaperManager
    private var cropWidth:Int=0
    private var cropHeight:Int=0
    private var wallpaperWidth:Int=0
    private var wallpaperHeight:Int=0
    private var mScaleFactor = 1.0f

    lateinit var button5: ToggleButton //clear Timer
    lateinit var timeSet: EditText //set Timer number
    lateinit var alarmManager: AlarmManager //important manager for repeat work
    lateinit var receiverIntent:Intent //for broadcast receiver intent
    lateinit var  pendingIntent : PendingIntent //I don't known
    private var intervalInSeconds: Long = 0 //time (sec)
    private var intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)

    //RECEIVER
    var dataList=mutableListOf<Uri>()//for test, a list of Uri
    //RECEIVER=

    var screenReceiver:ScreenStatusReceiver?=null
    lateinit var button6:ToggleButton //screen
    lateinit var checkbox:CheckBox
    var uriList=mutableListOf<Uri>()//for test, a list of Uri

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }

    private val PERMISSION_REQUEST_CODE = 100
    private var canimg:Boolean = true

    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext() as Activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // 已经获得了读取外部存储的权限
            // 进行相应的操作
        }
    }

    // 处理权限请求结果
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 用户授予了读取外部存储的权限
                // 进行相应的操作
            } else {
                canimg = false
            }
        }
    }

    private fun getDistance(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return sqrt(dx * dx + dy * dy)
    }

    private  fun WallpaperShow(bitmap:Bitmap){
        try {
            val scaledBitmap =
                Bitmap.createScaledBitmap(bitmap, wallpaperWidth, wallpaperHeight, true)
            wallpaperManager.setBitmap(scaledBitmap)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    var ckbox=false
    private lateinit var sharedViewModel: SharedViewModel
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        // 观察imageUris对象的变化
        sharedViewModel.ImageUris.observe(viewLifecycleOwner) { ImageUris ->
            Log.d("imageuri","$ImageUris.size")
            // 在这里更新UI或使用新的imageUris列表
            // ...
            dataList=ImageUris.map { Uri.parse(it) }.toMutableList()
            Log.d("imageuri","$dataList")
            if(ckbox){
                uriList= dataList.shuffled() as MutableList<Uri>
                Log.d("list",uriList.toString())
            }else{
                uriList=dataList
            }
            Log.d("imageuri","$uriList")
        }
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        // 在这里为HomeFragment设置布局文件
        imgview = view.findViewById(R.id.img_view)
        wallView = view.findViewById(R.id.wallView)

        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        wallpaperWidth = displayMetrics.widthPixels
        wallpaperHeight = displayMetrics.heightPixels
        cropWidth = ((wallpaperWidth.toFloat()*0.75).toInt())
        cropHeight = ((wallpaperHeight.toFloat()*0.75).toInt())
        wallpaperManager = WallpaperManager.getInstance(requireContext())
        var URI:Uri? = null

        checkbox=view.findViewById(R.id.checkBox)
        checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
            ckbox = isChecked
        }

        button6=view.findViewById(R.id.btn6)
        button6.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                if(uriList.size==0){
                    buttonView.isChecked = false
                    Toast.makeText(requireContext(), "尚未加入圖片，請移置資料夾新增桌布圖", Toast.LENGTH_SHORT).show()
                }else{
                    // 初始化广播接收器
                    val filter = IntentFilter().apply {
                        addAction(Intent.ACTION_SCREEN_ON)
                        addAction(Intent.ACTION_SCREEN_OFF)
                    }
                    screenReceiver = ScreenStatusReceiver()
                    requireContext().registerReceiver(screenReceiver, filter)
                    // 初始化广播接收器
                    Log.e("???",uriList.toString())
                    val sharedPref =requireContext().getSharedPreferences("screen", Context.MODE_PRIVATE)
                    val editor = sharedPref.edit()
                    // 将 URI 列表转换为字符串列表
                    val stringList: MutableList<String> = uriList.map { it.toString() }.toMutableList()
                    editor.putString("items", Gson().toJson(stringList)) // 將 items 資料寫入
                    editor.putString("wallpaperWidth", Gson().toJson(wallpaperWidth)) // 將 items 資料寫入
                    editor.putString("wallpaperHeight", Gson().toJson(wallpaperHeight)) // 將 items 資料寫入
                    editor.apply() // 提交資料變更
                }

            } else {
                Log.d("toggle","stop")
                //unregisterReceiver(screenReceiver)
                screenReceiver?.let {
                    requireContext().unregisterReceiver(it)
                    screenReceiver = null
                }
                Log.d("toggle","stop over")
            }
        }

        //RECEIVER
        timeSet=view.findViewById(R.id.sec) //Timerset
        /*for test add list contain Uri*/
       /* dataList.add(Uri.parse("android.resource://" + requireContext().packageName + "/drawable/test1"))
        dataList.add(    Uri.parse("android.resource://" + requireContext().packageName + "/drawable/test2"))
        */uriList=dataList
        //RECEIVER=

        //RECEIVER
        button5=view.findViewById(R.id.btn5)
        button5.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                if(uriList.size==0){
                    buttonView.isChecked = false
                    Toast.makeText(requireContext(), "尚未加入圖片，請移置資料夾新增桌布圖", Toast.LENGTH_SHORT).show()
                }else{
                try{
                    intervalInSeconds = timeSet.text.toString().toLong() // get user set time(sec)
                    alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager //get
                    receiverIntent = Intent(requireContext(), AlarmReceiver::class.java) //set
                    receiverIntent.putExtra("SECONDS", intervalInSeconds)//sec
                    receiverIntent.putExtra("wallpaperWidth",wallpaperWidth)//size for img clip
                    receiverIntent.putExtra("wallpaperHeight",wallpaperHeight)//size
                    receiverIntent.putParcelableArrayListExtra("Uri",ArrayList<Uri>( uriList))//list can like this way to transport list
                    pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, receiverIntent, PendingIntent.FLAG_IMMUTABLE)//set
                    if(intervalInSeconds<60)intervalInSeconds=60//easy way to set limit
                    val intervalMillis = intervalInSeconds*1000 // sec->millisecond = every that time to work
                    val startTimeMillis = System.currentTimeMillis() //first run = now

                    alarmManager.setRepeating(//repeat set
                        AlarmManager.RTC_WAKEUP,//??
                        startTimeMillis,//first run time= now
                        intervalMillis,//during
                        pendingIntent //set
                    )
                }catch (e: NumberFormatException) {
                    Toast.makeText(requireContext(), "請輸入間隔時長(限60秒以上)", Toast.LENGTH_SHORT).show()
                    buttonView.isChecked = false
                    Log.e("wrong","no input ")
                    e.printStackTrace()
                }}
            } else {
                try{
                    alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager //get
                    receiverIntent = Intent(requireContext(), AlarmReceiver::class.java) //set
                    pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, receiverIntent, PendingIntent.FLAG_IMMUTABLE)//set

                    // cancel = stop Timer
                    alarmManager.cancel(pendingIntent)//must use that used intent or not work
                    Log.d("cancel","stop")
                }catch (e: IOException) {
                    Log.e("cancel","stop")
                    e.printStackTrace()
                }
            }
        }
        //RECEIVER=
        val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("result",result.resultCode.toString())
            if(result.resultCode==0){
                Toast.makeText(requireContext(), "未設置桌布，已返回初始桌布", Toast.LENGTH_SHORT).show()

            }
            //            if (result.resultCode == Activity.RESULT_OK) {
//                val data: Intent? = result.data
//                // 在這裡處理活動結果
//            }
        }

        val imagePicker2 = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {//開圖庫取圖片
                //val uri =
                // Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/drawable/testgif")
                val intentdata = Intent(requireContext(), MyWallpaperService::class.java)
                intentdata.putExtra("key", uri.toString())
                requireContext().startService(intentdata)
                //val wallpaperService = MyWallpaperService()
                WallpaperManager.getInstance(requireContext()).clear()
                /*val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                intent.putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    ComponentName(this, MyWallpaperService::class.java)
                )
                startActivity(intent)*/
                //val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(requireContext(), MyWallpaperService::class.java))
                // 在需要啟動另一個活動時
                //val intent = Intent(this, AnotherActivity::class.java)
                startForResult.launch(intent)
                // startActivityForResult(intent, REQUEST_CODE_WALLPAPER)
            }
        }
/*
        val imagePicker =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {//開圖庫取圖片
                    imgview.setImageURI(uri)
                    //給框框 wallView大小隨桌布大小
                    val params = wallView.layoutParams//為了改大小要先取出
                    params.width = cropWidth
                    params.height = cropHeight
                    Log.d("xxx","${wallpaperWidth},${wallpaperHeight}\n${cropWidth},${cropHeight}")
                    wallView.layoutParams = params
                    wallView.requestLayout()


                    //顯示桌布
                    val drawable = imgview.drawable
                    imgview.adjustViewBounds = true
                    WallpaperShow((drawable!! as BitmapDrawable).bitmap)
                    //++?
                    Glide.with(this)
                        .asDrawable()
                        .load(uri)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: com.bumptech.glide.request.target.Target<Drawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                // Handle image loading failure
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: com.bumptech.glide.request.target.Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                resource?.let {
                                    if (resource is GifDrawable) {
                                        // Loaded resource is a GIF
                                        resource
                                    } else {
                                        requestStoragePermission()
                                        // Loaded resource is a static image
                                        if (canimg) {
                                            BitmapDrawable(
                                                resources,
                                                (resource as BitmapDrawable).bitmap
                                            )
                                        } else {
                                            return false
                                        }
                                    }
                                    // Perform related operations, such as setting the ImageView content
                                    imgview.setImageDrawable(resource)
                                }
                                return false
                            }
                        })
                        .into(imgview)
                    //++?
                }
            }*/


        button3 = view.findViewById(R.id.btn3)
        button3.setOnClickListener {
            Log.d("wall", "按下btn3")
            //WallpaperManager.getInstance(this).clear()
            imagePicker2.launch("image/*")//按鈕取圖庫照片//按鈕取圖庫照片
        }

        /*
        button = view.findViewById(R.id.btn)
        button.setOnClickListener {
            imagePicker.launch("image/*")//按鈕取圖庫照片
        }
        */*/
        var mode = NONE
        var mLastTouchX = 1.0f
        var mLastTouchY = 1.0f
        // 設置觸摸事件監聽器
        imgview.setOnTouchListener { _, event ->
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {

                    mLastTouchX = event.x
                    mLastTouchY = event.y
                    mode = DRAG
                    Log.d("QQ", "one p")
                }
                MotionEvent.ACTION_POINTER_DOWN -> {

                    val distance = getDistance(event)
                    mScaleFactor = imgview.scaleX / distance
                    mode = ZOOM
                    Log.d("QQ", "two p")
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mode == DRAG) {
                        Log.d("QQ", "move one")
                        val translationX = event.x - mLastTouchX
                        val translationY = event.y - mLastTouchY
                        imgview.translationX += translationX
                        imgview.translationY += translationY

                    } else if (mode == ZOOM) {
                        Log.d("QQ", "move two")

                        val distance = getDistance(event)

                        val newScaleFactor = distance * mScaleFactor
                        imgview.scaleX = newScaleFactor
                        imgview.scaleY = newScaleFactor
                    }

                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    mode = NONE
                    mScaleFactor = 1.0f
                }
            }
            val location = IntArray(2)
            imgview.getLocationInWindow(location)

            val x = location[0]
            val y = location[1]
            Log.d("size","${x},${y}")
            //Log.d("first","${imgview.getLeft()},${imgview.getTop() }")
            Log.d("view","${imgview.width},${imgview.height}")
            //imgview.getLeft() imgview.getTop() //不知道是甚麼值 好像是初始時的左和上
            val location2 = IntArray(2)
            wallView.getLocationOnScreen(location2)

            val xx = location[0]
            val yy = location[1]
            Log.d("wallsize","${xx},${yy}")//左上角
            //Log.d("first","${imgview.getLeft()},${imgview.getTop() }")
            Log.d("wallview","${wallView.width},${wallView.height}")
            true
        }//touch
        /*
        button2 = view.findViewById(R.id.btn2)
        button2.setOnClickListener {
            // 計算出裁剪區域
            Log.d("QQQQ","set")
            val location2 = IntArray(2)
            wallView.getLocationOnScreen(location2)
            val wallLeft = location2[0]
            val wallTop = location2[1]
            val clipRect = Rect(wallLeft, wallTop, wallLeft + wallView.width, wallTop + wallView.height)
            Log.d("getp","左上角:${clipRect.left},${clipRect.top}")
            Log.d("getp","寬高:${ clipRect.width()},${clipRect.height()}")
            Log.d("getp","右下角:${wallLeft + wallView.width},${wallTop + wallView.height}")
            val relativeLayout = view.findViewById<RelativeLayout>(R.id.container)
            // 開啟 drawingCache
            relativeLayout.isDrawingCacheEnabled = true
            relativeLayout.buildDrawingCache()

            try {
                //擷取畫面
                val pic=relativeLayout.drawingCache
                if(pic==null) Log.d("QQQQ","nono")
                val location3 = IntArray(2)//取偏差值
                button2.getLocationOnScreen(location3)
                val btntop = location3[1]
                if(pic!=null) {
                    Log.d("QQQQ","setto")
                    val bitmap = Bitmap.createBitmap(pic)
                    relativeLayout.isDrawingCacheEnabled = false
                    // 裁剪出要保存的區域
                    var statusBarHeight=btntop //位置調校
                    Log.d("QQQQ","上面區:${clipRect.top}-${statusBarHeight}->${clipRect.top-statusBarHeight}")
                    val croppedBitmap = Bitmap.createBitmap(bitmap, clipRect.left, clipRect.top-statusBarHeight, clipRect.width(), clipRect.height())
                    Log.d("QQQQ","切圖:${croppedBitmap.width},${croppedBitmap.height}")
                    WallpaperShow(croppedBitmap)
                }
            }catch (e: IOException) {
                e.printStackTrace()
            }

        }//btn2*/
        return view
    }

}