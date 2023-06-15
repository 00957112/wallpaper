package com.example.test
import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import kotlin.math.sqrt
import android.Manifest
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.widget.RelativeLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener

class MainActivity : AppCompatActivity() {//主頁面
    lateinit var button:Button
    lateinit var button2:Button
    lateinit var button3:Button
    lateinit var imgview:ImageView
    lateinit var wallView:ImageView
    //RECEIVER
    lateinit var button4:Button //set Timer button
    lateinit var button5:Button //clear Timer
    lateinit var timeSet: EditText //set Timer number
    lateinit var alarmManager:AlarmManager //important manager for repeat work
    lateinit var receiverIntent:Intent //for broadcast receiver intent
    lateinit var  pendingIntent : PendingIntent //I don't known
    private var intervalInSeconds: Long = 0 //time (sec)
    //RECEIVER=
    lateinit var wallpaperManager:WallpaperManager
    private var cropWidth:Int=0
    private var cropHeight:Int=0
    private var wallpaperWidth:Int=0
    private var wallpaperHeight:Int=0
    private var mScaleFactor = 1.0f
    private var canimg:Boolean = true

     //RECEIVER
    val dataList=mutableListOf<Uri>()//for test, a list of Uri
    //RECEIVER=
    
    //screen
    var screenReceiver:ScreenStatusReceiver?=null
    lateinit var button6:Button //screen
    lateinit var checkbox:CheckBox
    var uriList=mutableListOf<Uri>()//for random
    //screen=
    
    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
    }
    private fun getDistance(event: MotionEvent): Float {
        val dx = event.getX(0) - event.getX(1)
        val dy = event.getY(0) - event.getY(1)
        return sqrt(dx * dx + dy * dy)
    }
 private val PERMISSION_REQUEST_CODE = 100

    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
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
    private  fun WallpaperShow(bitmap:Bitmap){
        try {
            val scaledBitmap =
                Bitmap.createScaledBitmap(bitmap, wallpaperWidth, wallpaperHeight, true)
            wallpaperManager.setBitmap(scaledBitmap)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


   @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        //screen
        checkbox=findViewById(R.id.checkBox)//for random
        checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                uriList= dataList.shuffled() as MutableList<Uri>
                Log.d("list",uriList.toString())
            } else {
                uriList=dataList
            }
        }

        button6=findViewById(R.id.btn6)//for screen on/off
        button6.setOnClickListener{
            //init
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            screenReceiver = ScreenStatusReceiver()
            registerReceiver(screenReceiver, filter)
            Log.e("???",uriList.toString())
            //send data = store
            val sharedPref = getSharedPreferences("screen", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            // 将 URI 列表转换为字符串列表
            val stringList: MutableList<String> = uriList.map { it.toString() }.toMutableList()
            editor.putString("items", Gson().toJson(stringList)) // 將 items 資料寫入
            editor.putString("wallpaperWidth", Gson().toJson(wallpaperWidth)) // 將 items 資料寫入
            editor.putString("wallpaperHeight", Gson().toJson(wallpaperHeight)) // 將 items 資料寫入
            editor.apply() // 提交資料變更

        }
        //screen=

        imgview = findViewById(R.id.img_view)
        wallView=findViewById(R.id.wallView)

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        wallpaperWidth = displayMetrics.widthPixels
        wallpaperHeight = displayMetrics.heightPixels
        cropWidth = ((wallpaperWidth.toFloat()*0.75).toInt())
        cropHeight = ((wallpaperHeight.toFloat()*0.75).toInt())
        wallpaperManager = WallpaperManager.getInstance(this)
       var URI:Uri? = null

       fun imagePicker2 () {
            //val uri =
            // Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/drawable/testgif")
            val intentdata = Intent(this, MyWallpaperService::class.java)
            intentdata.putExtra("key", URI.toString())
            startService(intentdata)

            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, MyWallpaperService::class.java)
            )
            startActivity(intent)
        }

        val imagePicker =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {//開圖庫取圖片
                    URI = uri
                    val params = wallView.layoutParams//為了改大小要先取出
                    params.width = cropWidth
                    params.height = cropHeight
                    Log.d("xxx", "${wallpaperWidth},${wallpaperHeight}\n${cropWidth},${cropHeight}")
                    wallView.layoutParams = params
                    wallView.requestLayout()
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
        //RECEIVER
        timeSet=findViewById(R.id.sec) //Timerset
        /*for test add list contain Uri*/
        dataList.add(Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/drawable/test1"))
        dataList.add(    Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/drawable/test2"))
        uriList=dataList

        button5=findViewById(R.id.btn5)
        button5.setOnClickListener{
            alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager //get
            receiverIntent = Intent(this, AlarmReceiver::class.java) //set
            pendingIntent = PendingIntent.getBroadcast(this, 0, receiverIntent, 0)//set

            // cancel = stop Timer
            alarmManager.cancel(pendingIntent)//must use that used intent or not work
            Log.d("cancel","stop")
        }
        button4=findViewById(R.id.btn4)
        button4.setOnClickListener{
            intervalInSeconds = timeSet.text.toString().toLong() // get user set time(sec)
            alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager //get
            receiverIntent = Intent(this, AlarmReceiver::class.java) //set
            receiverIntent.putExtra("SECONDS", intervalInSeconds)//sec
            receiverIntent.putExtra("wallpaperWidth",wallpaperWidth)//size for img clip
            receiverIntent.putExtra("wallpaperHeight",wallpaperHeight)//size
            receiverIntent.putParcelableArrayListExtra("Uri",ArrayList<Uri>( uriList))//list can like this way to transport list
            pendingIntent = PendingIntent.getBroadcast(this, 0, receiverIntent, 0)//set
            if(intervalInSeconds<60)intervalInSeconds=60//easy way to set limit
            val intervalMillis = intervalInSeconds*1000 // sec->millisecond = every that time to work
            val startTimeMillis = System.currentTimeMillis() //first run = now

            alarmManager.setRepeating(//repeat set
                AlarmManager.RTC_WAKEUP,//??
                startTimeMillis,//first run time= now
                intervalMillis,//during
                pendingIntent //set
            )

        }
        //RECEIVER=

        button3 = findViewById(R.id.btn3)
        button3.setOnClickListener {
            Log.d("wall", "按下btn3")
            //WallpaperManager.getInstance(this).clear()
            imagePicker2()//按鈕取圖庫照片
        }
        button = findViewById(R.id.btn)
        button.setOnClickListener {
            imagePicker.launch("image/*")//按鈕取圖庫照片
        }
        button2 = findViewById(R.id.btn2)

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
            val relativeLayout = findViewById<RelativeLayout>(R.id.container)
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

        }//btn2
    }




}
