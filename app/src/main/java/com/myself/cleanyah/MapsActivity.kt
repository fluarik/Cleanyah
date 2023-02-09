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
import android.nfc.tech.NfcF
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
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
import com.google.android.gms.maps.model.*
import com.myself.cleanyah.card_emu.HostCardEmulatorService
import com.myself.cleanyah.databinding.ActivityMapsBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var mCameraContent: ActivityResultLauncher<Intent>
    private var mCameraImageUri: Uri? = null
    private var mDialog: AlertDialog? = null
    private var mTimeCount = 0
    private val mLatVal = arrayOf(0.0, 0.0)
    private var mIsCountIncrease = true
    private var mIsTimerStart = false
    private var mIsMapStart = false
    private var mMoveCatIconThread = Thread()
    private lateinit var mAdapter: NfcAdapter
    private lateinit var mPendingIntent: PendingIntent
    private lateinit var mFilters: Array<IntentFilter>
    private lateinit var mTechLists: Array<Array<String>>
    private var mMakerKawasaki: Marker? = null
    private var mMakerCat: Marker? = null
    private var mMakerTrash: Marker? = null
    private val mLock = ReentrantLock()
    private val mTrashMap: MutableMap<Marker, ImageView> = mutableMapOf()
    private var mIsNfcAuthentication = false
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
        mFilters = arrayOf(IntentFilter(NFC_ACTION))
        mTechLists = arrayOf(arrayOf(NfcF::class.java.name))

        mAdapter.enableReaderMode(
            this,
            { mIsNfcAuthentication = true },
            NfcAdapter.FLAG_READER_NFC_F,
            null
        )

        startService(Intent(application, HostCardEmulatorService::class.java))
        Log.i("MapsActivity", "startService")
    }

    override fun onResume() {
        super.onResume()
        setCameraButton()
        mAdapter.enableForegroundDispatch(this, mPendingIntent, mFilters, mTechLists)
        if (!mIsTimerStart) {
            mIsTimerStart = true
            startMoveCatIcon()
        }
    }
    override fun onPause() {
        super.onPause()
        if (this.isFinishing) {
            mAdapter.disableForegroundDispatch(this)
        }
    }
    override fun onStop() {
        super.onStop()
        if (mIsTimerStart) {
            mIsTimerStart = false
            mMoveCatIconThread.interrupt()
        }
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

    private fun showMessage(title: String?, text: String) {
        val builder = AlertDialog.Builder(this)
        if (title != null) {
            builder.setTitle(title).setIcon(R.drawable.cat_image2)
        }
        builder.setMessage(text)
        mDialog = builder.create()
        mDialog?.let {
            Handler(Looper.getMainLooper()).postDelayed({
                it.dismiss()
            }, 3000)
            it.show()
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
                        showMessage("報告ありがとうニャ！！", "ゴミを報告しました　　+5pt!")
                        setTrashMaker()?.let { it1 -> mTrashMap[it1] = imageView }
                    }, 7000)
                    mDialog!!.show()
                    window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
                }
            }
            .setNegativeButton("いいえ") { _, _ ->
                showMessage(null, "画像の送信をキャンセルしました")
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
                    checkPermissions()
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
    private fun checkPermissions() {
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
    }
    private fun setCameraButton(){
        binding.launchButton.setOnClickListener {
            // カメラ機能を実装したアプリが存在するかチェック
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(packageManager)?.let {
                checkPermissions()
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
    private fun setMainMaker(): Marker? {
        val options = MarkerOptions()
        options.position(LatLng(35.5319793421874, 139.69685746995052))
        options.title("Marker!")
        options.draggable(true)
        return mMap.addMarker(options)
    }
    private fun setCatMaker(): Marker? {
        val options = MarkerOptions()
        options.position(LatLng(35.531685, 139.694082))
        options.title("Cat!")
        options.icon(getMakerIcon(R.drawable.spot_cat))
        return mMap.addMarker(options)
    }
    private fun setTrashMaker(): Marker? {
        val options = MarkerOptions()
        options.position(LatLng(35.531711, 139.6952532))
        options.title("Trash!")
        options.icon(getMakerIcon(R.drawable.spot_gomi))
        val marker = mMap.addMarker(options)
        marker?.let { it.position = mMakerKawasaki?.position!! }
        return marker
    }
    private fun startMoveCatIcon() {
        mMoveCatIconThread = Thread {
            this.runOnUiThread {
                while (true) {
                    Thread.sleep(1000)
                    if (mIsMapStart) {
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
                        mMakerCat?.position = LatLng(mLatVal[0], mLatVal[1])
                        //moveCatMaker(mLatVal[0], mLatVal[1])
                        //reloadMaker(isTrashPointChange = false, isTrashPoint = false)
                    }
                    Log.i("MainActivity", "Timer Task called.")
                }
            }
        }
        //mMoveCatIconThread.start()
    }
    private fun removeTrashMaker(marker: Marker){
    }
    override fun onMapReady(googleMap: GoogleMap) {
        //val MIN_LAT_LNG = arrayOf(35.531685, 139.694082)
        //val MAX_LAT_LNG = arrayOf(35.531711, 139.695253)
        mMap = googleMap
        mMakerKawasaki = setMainMaker()
        mMakerCat = setCatMaker()
        //mMakerTrash = setTrashMaker()
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(35.5319793421874, 139.69685746995052), 16.5F))
        mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {
            }
            override fun onMarkerDragEnd(marker: Marker) {
                if (marker == mMakerKawasaki) {
                    mMakerKawasaki?.let {
                        it.position = marker.position
                    }
                }
            }
            override fun onMarkerDrag(marker: Marker) {
            }
        })
        mMap.setOnMarkerClickListener { p0 ->
            if (p0 != mMakerKawasaki && p0 != mMakerCat) {
                val imageView = mTrashMap[p0]
                imageView?.let {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("ゴミを片付けましたか")
                    builder.setPositiveButton("はい") { _, _ ->
                        mIsNfcAuthentication = false
                        val builder1 = AlertDialog.Builder(this)
                        builder1.setMessage("くりーにゃーにスマホをタッチしてください。")
                        //mDialog.let { it!!.dismiss() }
                        //mDialog = builder1.create()
                        //mDialog.let { it!!.show() }
                        val dialog1 = builder1.create()
                        dialog1.show()

                        var count = 0
                        while (!mIsNfcAuthentication) {
                            Thread.sleep(1)
                            count += 1
                            if (count > 5000) break
                        }
                        dialog1.dismiss()
                        if (mIsNfcAuthentication) {
                            showMessage("清掃ありがとうニャ！！", "ゴミを片付けました　　+10pt!")
                            mTrashMap.remove(p0)
                            p0.remove()
                        }
                    }
                    builder.setNegativeButton("いいえ") {_, _ ->}
                    //builder.setView(imageView)
                    //mDialog.let { it!!.dismiss() }
                    //mDialog = builder.create()
                    //mDialog.let { it!!.show() }
                    val dialog = builder.create()
                    dialog.show()
                }
            }
            false
        }
        mTimeCount = 0
        mLatVal[0] = 35.531685
        mLatVal[1] = 139.694082
        mIsMapStart = true
    }

}