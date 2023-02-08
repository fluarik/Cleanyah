package com.myself.cleanyah

import android.Manifest
import android.R.drawable.*
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.myself.cleanyah.databinding.ActivityMapsBinding
import java.util.*


@Suppress("DEPRECATION")
class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var mDialog: AlertDialog? = null
    private var mTimeCount = 0
    private val mLatVal = arrayOf(0.0, 0.0)
    private var mIsCountIncrease = true
    private val mMoveCatIconTimer = Timer()

    companion object {
        const val CAMERA_REQUEST_CODE = 1
        const val CAMERA_PERMISSION_REQUEST_CODE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // タグのIDを取得
        val tagId: ByteArray = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID) ?: return
        val list = ArrayList<String>()
        for (byte in tagId) {
            list.add(String.format("%02X", byte.toInt() and 0xFF))
        }
        Log.d("NFC Id", list.joinToString(":"))
    }

    override fun onResume() {
        super.onResume()
        binding.launchButton.setOnClickListener {
            // カメラ機能を実装したアプリが存在するかチェック
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(packageManager)?.let {
                if (checkCameraPermission()) {
                    takePicture()
                } else {
                    grantCameraPermission()
                }
            } ?: Toast.makeText(this, "カメラを扱うアプリがありません", Toast.LENGTH_LONG).show()
        }
    }

    override fun onStop() {
        super.onStop()
        mMoveCatIconTimer.cancel()
    }

    private fun showMessage(text: String) {
        val builder = AlertDialog.Builder(this)
        builder.setMessage(text)
        mDialog = builder.create()
        mDialog.let {
            val handler = Handler()
            val dialogDismiss = Runnable { mDialog!!.dismiss() }
            mDialog!!.show()
            handler.postDelayed(dialogDismiss, 3000)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            data.let {
                val imageBitmap = it!!.extras!!.get("data") as Bitmap
                this.let {
                    val imageView = ImageView(this)
                    imageView.setImageBitmap(imageBitmap)
                    imageView.adjustViewBounds = true
                    AlertDialog.Builder(this)
                        .setTitle("画像を送信しますか")
                        .setPositiveButton("はい") { _, _ ->
                            val builder = AlertDialog.Builder(this)
                            builder.setMessage("画像の送信中です")
                            val progressBar = ProgressBar(this)
                            builder.setView(progressBar)
                            mDialog = builder.create()
                            mDialog.let {
                                val handler = Handler()
                                val dialogDismiss = Runnable {
                                    mDialog!!.dismiss()
                                    showMessage("画像を送信しました")
                                }
                                mDialog!!.show()
                                handler.postDelayed(dialogDismiss, 7000)
                            }
                        }
                        .setNegativeButton("いいえ") { _, _ ->
                            showMessage("画像の送信をキャンセルしました")
                        }
                        .setView(imageView)
                        .show()
                }
            }
        }
    }

    private fun takePicture() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
            }
        }
    }

    private fun checkCameraPermission() = PackageManager.PERMISSION_GRANTED ==
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA)


    private fun grantCameraPermission() =
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                takePicture()
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
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
        catMarker.icon(getMakerIcon(R.drawable.cat_image))
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

    private fun getMakerIcon(id: Int): BitmapDescriptor {
        val width = 170
        val height = 85
        val b = BitmapFactory.decodeResource(resources, id)
        val smallMarker = Bitmap.createScaledBitmap(b, width, height, false)
        return BitmapDescriptorFactory.fromBitmap(smallMarker)
    }
}