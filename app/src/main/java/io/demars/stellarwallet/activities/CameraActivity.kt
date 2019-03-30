package io.demars.stellarwallet.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import io.demars.stellarwallet.R
import kotlinx.android.synthetic.main.activity_camera.*
import timber.log.Timber
import java.io.File
import java.io.IOException
import android.app.Activity
import android.net.Uri
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.Toast
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import io.demars.stellarwallet.firebase.Firebase

@Suppress("DEPRECATION")
class CameraActivity : BaseActivity() {
  companion object {
    private const val REQUEST_GALLERY = 111
    private const val PIC_FILE_NAME = "USER_TEST_ID_PICTURE"
    fun newInstance(context: Context): Intent {
      return Intent(context, CameraActivity::class.java)
    }
  }

  private lateinit var file: File
  private var camera: Camera? = null
  private var pictureBytes: ByteArray? = null
  private val picture = Camera.PictureCallback { data, _ ->
    pictureBytes = data
    updateView()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_camera)

    file = File(getExternalFilesDir(null), PIC_FILE_NAME)

    updateView()

    // Create an instance of Camera
    camera = getCameraInstance()
    camera?.let {
      // Create our Preview view
      CameraPreview(this, it)
    }.also {
      camera_preview.addView(it)
    }
  }

  private fun updateView() {
    if (pictureBytes != null) {
      cameraButton.visibility = GONE
      galleryButton.visibility = GONE

      ensureImageMessage.visibility = VISIBLE
      retakeButton.visibility = VISIBLE
      sendButton.visibility = VISIBLE

      retakeButton.setOnClickListener {
        onBackPressed()
      }

      sendButton.setOnClickListener {
        sendPictureToFirebase()
      }
    } else {
      ensureImageMessage.visibility = GONE
      retakeButton.visibility = GONE
      sendButton.visibility = GONE

      cameraButton.visibility = VISIBLE
      galleryButton.visibility = VISIBLE

      imagePreview.setImageDrawable(null)
      camera_preview.visibility = VISIBLE

      cameraButton.setOnClickListener {
        takePicture()
      }

      galleryButton.setOnClickListener {
        pickFromGallery()
      }
    }
  }

  private fun pickFromGallery() {
    //Create an Intent with action as ACTION_PICK
    val intent = Intent(Intent.ACTION_PICK)
    // Sets the type as image/*. This ensures only components of type image are selected
    intent.type = "image/*"
    //We pass an extra array with the accepted mime types. This will ensure only components with these MIME types as targeted.
    val mimeTypes = arrayOf("image/jpeg", "image/png")
    intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
    // Launching the Intent
    startActivityForResult(intent, REQUEST_GALLERY)
  }

  private fun takePicture() {
    camera?.takePicture(null, null, picture)
  }

  private fun sendPictureToFirebase() {
    pictureBytes?.let {
      Firebase.getCurrentUserUid()?.let { uid ->
        showUploadingView()
        Firebase.uploadBytes(it,
          OnSuccessListener {
            Firebase.getDatabaseReference().child("users").child(uid)
              .child("id_image_sent").setValue(true)
              .addOnSuccessListener {
                Toast.makeText(this, "ID photo uploaded", Toast.LENGTH_LONG).show()
                finish()
              }.addOnFailureListener {
                Toast.makeText(this, R.string.failed_upload_image, Toast.LENGTH_LONG).show()
                hideUploadingView()
              }
          }, OnFailureListener {
          Toast.makeText(this, R.string.failed_upload_image, Toast.LENGTH_LONG).show()
          hideUploadingView()
        })
      } ?: Toast.makeText(this, "Cannot find user account, please try to verify again", Toast.LENGTH_LONG).show()
    }
  }

  /** Check if this device has a camera */
  private fun checkCameraHardware(context: Context): Boolean {
    return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
  }

  /** A safe way to get an instance of the Camera object. */
  private fun getCameraInstance(): Camera? {
    return try {
      Camera.open() // attempt to get a Camera instance
    } catch (e: Exception) {
      // Camera is not available (in use or does not exist)
      null // returns null if camera is unavailable
    }
  }

  /** A basic Camera preview class */
  @SuppressLint("ViewConstructor")
  internal class CameraPreview(
    context: Context,
    private val mCamera: Camera
  ) : SurfaceView(context), SurfaceHolder.Callback {

    private val mHolder: SurfaceHolder = holder.apply {
      // Install a SurfaceHolder.Callback so we get notified when the
      // underlying surface is created and destroyed.
      addCallback(this@CameraPreview)
      // deprecated setting, but required on Android versions prior to 3.0
      setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
      // Set portrait mode
      mCamera.setDisplayOrientation(90)

      //set camera to continually auto-focus
      val params = mCamera.parameters
      params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
      mCamera.parameters = params

      // Setup & start preview
      mCamera.apply {
        try {
          setPreviewDisplay(holder)
          startPreview()
        } catch (e: IOException) {
          Timber.d("Error setting camera preview: ${e.message}")
        }
      }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
      // empty. Take care of releasing the Camera preview in your activity.
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
      // If your preview can change or rotate, take care of those events here.
      // Make sure to stop the preview before resizing or reformatting it.
      if (mHolder.surface == null) {
        // preview surface does not exist
        return
      }

      // stop preview before making changes
      try {
        mCamera.stopPreview()
      } catch (e: Exception) {
        // ignore: tried to stop a non-existent preview
      }

      // set preview size and make any resize, rotate or
      // reformatting changes here

      // start preview with new settings
      mCamera.apply {
        try {
          setPreviewDisplay(mHolder)
          startPreview()
        } catch (e: Exception) {
          Timber.d("Error starting camera preview: ${e.message}")
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    camera?.stopPreview()
    camera?.release()
  }

  override fun onBackPressed() {
    if (pictureBytes != null) {
      pictureBytes = null
      camera?.startPreview()
      updateView()
    } else {
      super.onBackPressed()
    }
  }

  public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    // Result code is RESULT_OK only if the user selects an Image
    if (resultCode == Activity.RESULT_OK)
      when (requestCode) {
        REQUEST_GALLERY -> {
          //data.getData returns the content URI for the selected Image
          val picFromGallery = data?.data
          pictureBytes = getBitesFromUri(picFromGallery)
          imagePreview.setImageURI(picFromGallery)
          camera_preview.visibility = GONE
          updateView()
        }
      }
  }

  private fun getBitesFromUri(uri: Uri?): ByteArray? {
    uri?.let {
      contentResolver.openInputStream(it)?.let { inputStream ->
        return inputStream.readBytes()
      }
    } ?: return null
  }

  private fun showUploadingView() {
    ensureImageMessage.setText(R.string.uploading_photo)

    retakeButton.visibility = GONE
    sendButton.visibility = GONE
  }

  private fun hideUploadingView() {
    ensureImageMessage.setText(R.string.uploading_photo_error)

    retakeButton.visibility = VISIBLE
    sendButton.visibility = VISIBLE
  }
}