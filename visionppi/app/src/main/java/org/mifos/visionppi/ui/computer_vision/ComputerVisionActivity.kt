package org.mifos.visionppi.ui.computer_vision

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.android.synthetic.main.activity_computer_vision.*
import org.mifos.visionppi.R
import org.mifos.visionppi.adapters.SelectedImageAdapter
import org.mifos.visionppi.objectdetection.ObjectDetectionMain


class ComputerVisionActivity : AppCompatActivity(), ComputerVisionMVPView {


    private val images = ArrayList<Bitmap?>()
    private val PICK_FROM_GALLERY = 1
    private val CAMERA_REQUEST = 2
    private val MY_CAMERA_PERMISSION_CODE = 100


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_computer_vision)

        upload_from_gallery_btn.setOnClickListener {
            fetchFromGallery()
        }

        open_camera_btn.setOnClickListener {
            fetchFromCamera()
        }

        selected_images_list.layoutManager = GridLayoutManager(this, 3)
        selected_images_list.adapter = SelectedImageAdapter(images, this) { position: Int -> imageRemove(position) }

        analyse.setOnClickListener {
            analyzeImages()
        }
    }

    override fun fetchFromGallery() {
        try {
            if (ActivityCompat.checkSelfPermission(
                    this@ComputerVisionActivity,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this@ComputerVisionActivity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PICK_FROM_GALLERY
                )
            } else {
                /*val i = Intent(
                    Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI

                )
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                val ACTIVITY_SELECT_IMAGE = 1234
                startActivityForResult(i, ACTIVITY_SELECT_IMAGE)*/

                val intent = Intent()
                intent.type = "image/*"
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                intent.action = Intent.ACTION_GET_CONTENT
                val ACTIVITY_SELECT_IMAGE = 1234
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), ACTIVITY_SELECT_IMAGE)

                selected_images_list.adapter = SelectedImageAdapter(images, this) { position: Int -> imageRemove(position)}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    override fun fetchFromCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(applicationContext,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.CAMERA), MY_CAMERA_PERMISSION_CODE)
            }
            else {
                val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, CAMERA_REQUEST)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun analyzeImages() {
        for (image in images) {
            var d: ObjectDetectionMain = ObjectDetectionMain()
            d.initObjectDetection(image, this)
        }
    }

    override fun showToastMessage(string: String) {
        Toast.makeText(this, string, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            1234 -> if (resultCode == Activity.RESULT_OK) {
                if (data?.data != null) {
                    val mImageUri: Uri = data.data!!
                    val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, mImageUri)
                    images.add(bitmap)
                    selected_images_list.adapter = SelectedImageAdapter(images, this) { position: Int -> imageRemove(position)}
                } else {
                    if (data?.getClipData() != null) {
                        val mClipData: ClipData = data.clipData!!
                        for (i in 0 until mClipData.itemCount) {
                            val item = mClipData.getItemAt(i)
                            val uri: Uri = item.uri
                            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                            images.add(bitmap)
                            selected_images_list.adapter = SelectedImageAdapter(images, this) { position: Int -> imageRemove(position)}
                        }
                    }
                }
            }
        }

        if (requestCode === CAMERA_REQUEST && resultCode === Activity.RESULT_OK) {
            val photoCaptured = data?.extras?.get("data") as Bitmap
            images.add(photoCaptured)
            selected_images_list.adapter = SelectedImageAdapter(images, this) { position: Int -> imageRemove(position)}
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PICK_FROM_GALLERY ->
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val galleryIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    startActivityForResult(galleryIntent, PICK_FROM_GALLERY)
                } else {
                    showToastMessage(getString(R.string.cant_open_gallery))
                }
        }

        if (requestCode === MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] === PackageManager.PERMISSION_GRANTED) {
                showToastMessage("Camera permission granted")
                val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(cameraIntent, CAMERA_REQUEST)
            } else {
                showToastMessage("Camera permission denied")
            }
        }
    }

    fun imageRemove(position : Int){
        images.removeAt(position)
        selected_images_list.adapter = SelectedImageAdapter(images, this) { position: Int -> imageRemove(position)}

    }

}
