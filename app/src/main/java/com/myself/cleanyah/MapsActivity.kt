package com.myself.cleanyah

import android.Manifest
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.NfcF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.myself.cleanyah.databinding.ActivityMapsBinding
import java.text.SimpleDateFormat
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var mCameraContent: ActivityResultLauncher<Intent>
    private var mCameraImageUri: Uri? = null
    private var mDialog: AlertDialog? = null
    private var mTimeCount = 0
    private val mLatVal = arrayOf(0.0, 0.0)
    private var mIsCountIncrease = true
    private val mMoveCatIconTimer = Timer()
    private lateinit var mAdapter: NfcAdapter
    private lateinit var mPendingIntent: PendingIntent
    private lateinit var mFilters: Array<IntentFilter>
    private lateinit var mTechLists: Array<Array<String>>
    companion object {
        const val PERMISSION_REQUEST_CODE = 1
        const val NFC_ACTION = NfcAdapter.ACTION_TAG_DISCOVERED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mCameraContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val imageView = ImageView(this)
                imageView.setImageURI(mCameraImageUri)
                imageView.adjustViewBounds = true
                showSendImageDialog(imageView)
            }
        }

        // for NFC
        mAdapter = NfcAdapter.getDefaultAdapter(this)
        mPendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        //mFilters = arrayOf(IntentFilter(NFC_ACTION).apply { this.addDataType("*/*") })
        mFilters = arrayOf(IntentFilter(NFC_ACTION))
        mTechLists = arrayOf(arrayOf(NfcF::class.java.name))

        /*mAdapter.enableReaderMode(
            this,
            {
                val idm: ByteArray = it.id
                val sb = java.lang.StringBuilder()
                val formatter = Formatter(sb)
                for (b in idm) {
                    formatter.format("%02x", b)
                }
                val idmString: String = sb.toString().uppercase(Locale.getDefault())
                //Toast.makeText(this , idmString, Toast.LENGTH_LONG).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    showMessage("NFC読み取り")
                }, 100)
            },
            NfcAdapter.FLAG_READER_NFC_F,
            null
        )*/
    }

    override fun onResume() {
        super.onResume()
        setCameraButton()
        //mAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists)
    }
    override fun onPause() {
        super.onPause()
        if (this.isFinishing) {
            //mAdapter.disableForegroundDispatch(this)
        }
    }
    override fun onStop() {
        super.onStop()
        mMoveCatIconTimer.cancel()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null) return
        // showNfcTag
        if (intent.action == NFC_ACTION) {
            intent.getByteArrayExtra(NfcAdapter.EXTRA_ID).let { it ->
                val buffer = StringBuilder()
                for (b in it!!) {
                    val hex = String.format("%02X", b)
                    buffer.append(hex).append(" ")
                }
                val text = buffer.toString().trim { it <= ' ' }
                Toast.makeText(this , text, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showMessage(text: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(text)
        mDialog = builder.create()
        mDialog.let {
            Handler(Looper.getMainLooper()).postDelayed({
                mDialog!!.dismiss()
            }, 3000)
            mDialog!!.show()
        }
    }
    private fun showSendImageDialog(imageView: ImageView){
        AlertDialog.Builder(this)
            .setTitle("画像を送信しますか")
            .setPositiveButton("はい") { _, _ ->
                val builder = AlertDialog.Builder(this)
                builder.setMessage("画像の送信中です")
                val progressBar = ProgressBar(this)
                builder.setView(progressBar)
                mDialog = builder.create()
                mDialog.let {
                    Handler(Looper.getMainLooper()).postDelayed({
                        mDialog!!.dismiss()
                        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                        showMessage("画像を送信しました")
                    }, 7000)
                    mDialog!!.show()
                    window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                }
            }
            .setNegativeButton("いいえ") { _, _ ->
                showMessage("画像の送信をキャンセルしました")
            }
            .setView(imageView)
            .show()
    }
    private fun takePicture() {
        val dateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.JAPAN)
        val now = Date()
        val nowStr = dateFormat.format(now)
        val fileName = "${nowStr}.jpg"
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE,fileName)
        values.put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")

        //外部ストレージのURIを生成する
        mCameraImageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT,mCameraImageUri)
        mCameraContent.launch(intent)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE)
            if (grantResults.isNotEmpty())
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    if (PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission.CAMERA))
                        if (PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE))
                            if (PackageManager.PERMISSION_GRANTED == checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                                takePicture()
    }
    private fun checkPermission(permission: String): Boolean {
        var ret = true
        if (PackageManager.PERMISSION_DENIED == checkSelfPermission(permission)) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSION_REQUEST_CODE)
            // go to onRequestPermissionsResult()
            ret = false
        }
        return ret
    }
    private fun setCameraButton(){
        binding.launchButton.setOnClickListener {
            // カメラ機能を実装したアプリが存在するかチェック
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(packageManager)?.let {
                var isTakePicture = true
                if (!checkPermission(Manifest.permission.CAMERA)) {
                    isTakePicture = false
                }
                if (!checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    isTakePicture = false
                }
                if (!checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    isTakePicture = false
                }
                if (isTakePicture) {
                    takePicture()
                }
            } ?: Toast.makeText(this, "カメラを扱うアプリがありません", Toast.LENGTH_LONG).show()
        }

    }

    private fun getMakerIcon(id: Int): BitmapDescriptor {
        val width = 92
        val height = 120
        val b = BitmapFactory.decodeResource(resources, id)
        val smallMarker = Bitmap.createScaledBitmap(b, width, height, false)
        return BitmapDescriptorFactory.fromBitmap(smallMarker)
    }
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        //val sydney = LatLng(-34.0, 151.0)
        val kawasaki = LatLng(35.5319793421874, 139.69685746995052)
        val marker = MarkerOptions().position(kawasaki).title("Marker!")
        mMap.addMarker(marker)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kawasaki, 16.5F))

        val MIN_LAT_LNG = arrayOf(35.531685, 139.694082)
        //val MAX_LAT_LNG = arrayOf(35.531711, 139.695253)

        val catMarker = MarkerOptions().position(LatLng(MIN_LAT_LNG[0], MIN_LAT_LNG[1])).title("Cat")
        catMarker.icon(getMakerIcon(R.drawable.cat_spot))
        mMap.addMarker(catMarker)
        mTimeCount = 0
        mLatVal[0] = MIN_LAT_LNG[0]
        mLatVal[1] = MIN_LAT_LNG[1]

        mMoveCatIconTimer.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    if (mIsCountIncrease) {
                        mTimeCount += 1
                        if (mTimeCount % 2 == 0)
                            mLatVal[0] += 0.000001
                        mLatVal[1] += 0.00002
                        if (mTimeCount == 60)
                            mIsCountIncrease = false
                    } else {
                        mTimeCount -= 1
                        if (mTimeCount % 2 == 0)
                            mLatVal[0] -= 0.000001
                        mLatVal[1] -= 0.00002
                        if (mTimeCount == 0)
                            mIsCountIncrease = true
                    }
                    catMarker.position(LatLng(mLatVal[0], mLatVal[1]))
                    //Log.i(LOG_TAG, "Task called.")
                }
            }, 10, 1000
        )
    }

}