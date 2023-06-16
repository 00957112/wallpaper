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
    lateinit var button4:ImageView
    lateinit var button3:Button
    lateinit var button9:ImageView
    lateinit var imgview:ImageView
    lateinit var wallView:ImageView
    lateinit var tipText:TextView

    lateinit var wallpaperManager:WallpaperManager
    private var cropWidth:Int=0
    private var cropHeight:Int=0
    private var wallpaperWidth:Int=0
    private var wallpaperHeight:Int=0

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
    private var selectstate=0
    private var timersetnow=false
    private var screensetnow=false


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
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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

        sharedViewModel.ImageUris.observe(viewLifecycleOwner) { ImageUris ->
            Log.d("imageuri","$ImageUris.size")
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
        tipText = view.findViewById(R.id.tip_text)
        tipText.isSelected = true

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
        button6.setOnClickListener{

            if (button6.isChecked) {
                if(selectstate==2){
                    button6.isChecked = false
                    Toast.makeText(requireContext(), "請關閉計時功能", Toast.LENGTH_SHORT).show()
                }
                else if(uriList.size==0&&!screensetnow){
                    button6.isChecked = false
                    Log.d("nono","${uriList.size}")
                    Toast.makeText(requireContext(), "尚未加入圖片，請移置資料夾新增桌布圖", Toast.LENGTH_SHORT).show()
                }else if(!screensetnow){
                    screensetnow=true
                    button5.isEnabled = false
                    selectstate=1
                    // 初始化广播接收器
                    val filter = IntentFilter().apply {
                        addAction(Intent.ACTION_SCREEN_ON)
                        addAction(Intent.ACTION_SCREEN_OFF)
                    }
                    screenReceiver = ScreenStatusReceiver()
                    requireContext().registerReceiver(screenReceiver, filter)
                    Toast.makeText(requireContext(), "開啟功能", Toast.LENGTH_SHORT).show()
                    // 初始化广播接收器
                    //Log.e("???",uriList.toString())
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
                selectstate=0
                screensetnow=false
                //Log.d("toggle","stop")
                screenReceiver?.let {
                    requireContext().unregisterReceiver(it)
                    screenReceiver = null
                }
                Toast.makeText(requireContext(), "關閉功能", Toast.LENGTH_SHORT).show()
                button5.isEnabled = true
                //Log.d("toggle","stop over")
            }
        }

        //RECEIVER
        timeSet=view.findViewById(R.id.sec) //Timerset
        uriList=dataList
        //RECEIVER=

        //RECEIVER
        button5=view.findViewById(R.id.btn5)
        button5.setOnClickListener {
            if (button5.isChecked) {
                if(selectstate==1){
                    button5.isChecked = false
                    Toast.makeText(requireContext(), "請關閉計時功能", Toast.LENGTH_SHORT).show()
                }
                else if(uriList.size==0&&!screensetnow){
                    Log.d("nono","${uriList.size}")
                    button5.isChecked = false
                    Toast.makeText(requireContext(), "尚未加入圖片，請移置資料夾新增桌布圖", Toast.LENGTH_SHORT).show()
                }else if(!timersetnow){
                try{
                    //Log.d("nono","stop6")
                    timersetnow=true
                    button6.isEnabled = false
                    selectstate=2
                    intervalInSeconds = timeSet.text.toString().toLong() // get user set time(sec)
                    alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager //get
                    receiverIntent = Intent(requireContext(), AlarmReceiver::class.java) //set
                    receiverIntent.putExtra("SECONDS", intervalInSeconds)//sec
                    receiverIntent.putExtra("wallpaperWidth",wallpaperWidth)//size for img clip
                    receiverIntent.putExtra("wallpaperHeight",wallpaperHeight)//size
                    receiverIntent.putParcelableArrayListExtra("Uri",ArrayList<Uri>( uriList))//list can like this way to transport list
                    pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, receiverIntent, PendingIntent.FLAG_IMMUTABLE)//set
                    if(intervalInSeconds<60){
                        intervalInSeconds=60//easy way to set limit
                        Toast.makeText(requireContext(), "小於60秒，將設置為60秒", Toast.LENGTH_SHORT).show()
                    }
                    val intervalMillis = intervalInSeconds*1000 // sec->millisecond = every that time to work
                    val startTimeMillis = System.currentTimeMillis() //first run = now

                    alarmManager.setRepeating(//repeat set
                        AlarmManager.RTC_WAKEUP,//??
                        startTimeMillis,//first run time= now
                        intervalMillis,//during
                        pendingIntent //set
                    )
                    Toast.makeText(requireContext(), "開啟功能", Toast.LENGTH_SHORT).show()
                }catch (e: NumberFormatException) {
                    Toast.makeText(requireContext(), "請輸入間隔時長(限60秒以上)", Toast.LENGTH_SHORT).show()
                    button5.isChecked = false
                    button6.isEnabled = true
                    selectstate=0
                    timersetnow=false
                    //Log.e("wrong","no input ")
                    e.printStackTrace()
                }}
            } else {
                button6.isEnabled = true
                selectstate=0
                try{
                    alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager //get
                    receiverIntent = Intent(requireContext(), AlarmReceiver::class.java) //set
                    pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, receiverIntent, PendingIntent.FLAG_IMMUTABLE)//set

                    // cancel = stop Timer
                    alarmManager.cancel(pendingIntent)//must use that used intent or not work
                    //Log.d("cancel","stop")
                }catch (e: IOException) {
                    //Log.e("cancel","stop")
                    e.printStackTrace()
                }
                Toast.makeText(requireContext(), "關閉功能", Toast.LENGTH_SHORT).show()
            }
        }
        //RECEIVER=
        val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            Log.d("result",result.resultCode.toString())
            if(result.resultCode==0){
                Toast.makeText(requireContext(), "未設置桌布，已返回初始桌布", Toast.LENGTH_SHORT).show()
            }
        }

        val imagePicker2 = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {//開圖庫取圖片
                val intentdata = Intent(requireContext(), MyWallpaperService::class.java)
                intentdata.putExtra("key", uri.toString())
                requireContext().startService(intentdata)
                WallpaperManager.getInstance(requireContext()).clear()
                intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(requireContext(), MyWallpaperService::class.java))
                // 在需要啟動另一個活動時
                startForResult.launch(intent)
            }
        }

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
                }
            }

        button9 = view.findViewById(R.id.btn3)
        Glide.with(requireContext()).asGif().load(R.drawable.image_forgif).into(button9)
        button9.setOnClickListener {
            Log.d("wall", "按下btn3")
            imagePicker2.launch("image/*")//按鈕取圖庫照片//按鈕取圖庫照片
        }


        button4 = view.findViewById(R.id.btn)
        button4.setOnClickListener {
            imagePicker.launch("image/*")//按鈕取圖庫照片
        }

        return view
    }

}