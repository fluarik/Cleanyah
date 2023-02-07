package com.myself.cleanyah

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.myself.cleanyah.databinding.ActivityMapsBinding


@Suppress("DEPRECATION")
class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var mDialog: AlertDialog? = null
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
        val tagId : ByteArray =intent.getByteArrayExtra(NfcAdapter.EXTRA_ID) ?: return
        val list = ArrayList<String>()
        for(byte in tagId) {
            list.add(String.format("%02X", byte.toInt() and 0xFF))
        }
        Log.d("NFC Id",list.joinToString(":"))
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
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE)


    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
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
        mMap.addMarker(MarkerOptions().position(kawasaki).title("Marker!"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kawasaki, 15F))
    }
}