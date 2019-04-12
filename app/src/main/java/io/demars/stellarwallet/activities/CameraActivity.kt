package io.demars.stellarwallet.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
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
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import io.demars.stellarwallet.firebase.Firebase

@Suppress("DEPRECATION")
class CameraActivity : AppCompatActivity() {
  companion object {
    private const val REQUEST_GALLERY = 111
    private const val ARG_FOR_SELFIE = "ARG_FOR_SELFIE"
    private const val PIC_FILE_NAME = "USER_TEST_ID_PICTURE"
    fun newInstance(context: Context, forSelfie: Boolean): Intent {
      val intent = Intent(context, CameraActivity::class.java)
      intent.putExtra(ARG_FOR_SELFIE, forSelfie)
      return intent
    }
  }

  private lateinit var file: File
  private var camera: Camera? = null
  private var pictureBytes: ByteArray? = null
  private var hasCamera = false
  private var forSelfie = false
  private var frontCameraIndex = -1
  private var useFront = false
  private val picture = Camera.PictureCallback { data, _ ->
    pictureBytes = data
    updateView()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_camera)

    forSelfie = intent.getBooleanExtra(ARG_FOR_SELFIE, false)

    file = File(getExternalFilesDir(null), PIC_FILE_NAME)
    hasCamera = checkCameraHardware(this)
    if (hasCamera && forSelfie) {
      // Double check that frontal camera exists
      useFront = checkFrontCamera()
    }

    updateView()
  }

  private fun updateView() {
    if (pictureBytes != null) {
      cameraButton.visibility = GONE
      galleryButton.visibility = GONE

      retakeButton.visibility = VISIBLE
      sendButton.visibility = VISIBLE

      ensureImageMessage.visibility = VISIBLE
      ensureImageMessage.setText(R.string.ensure_image)

      retakeButton.setOnClickListener {
        onBackPressed()
      }

      sendButton.setOnClickListener {
        sendPictureToFirebase()
      }
    } else {
      retakeButton.visibility = GONE
      sendButton.visibility = GONE

      mainTitle.setText(if (forSelfie) R.string.selfie_with_id else R.string.photo_of_id)

      if (hasCamera) {
        cameraButton.visibility = VISIBLE
        ensureImageMessage.visibility = GONE
        // Create an instance of Camera
        camera = getCameraInstance()
        camera?.let {
          // Create our Preview view & add it to container
          cameraPreview.addView(CameraPreview(this, it))
        }
      } else {
        cameraButton.visibility = GONE
        ensureImageMessage.visibility = VISIBLE
        ensureImageMessage.setText(R.string.no_camera_message)
      }

      galleryButton.visibility = VISIBLE

      imagePreview.setImageDrawable(null)
      cameraPreview.visibility = VISIBLE
      camera?.startPreview()

      cameraButton.setOnClickListener {
        takePicture()
      }

      galleryButton.setOnClickListener {
        pickFromGallery()
      }

      cameraPreview.setOnClickListener {
        camera?.autoFocus(null)
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
    pictureBytes?.let { bytes ->
      Firebase.getCurrentUserUid()?.let { uid ->
        showUploadingView()
        Firebase.uploadBytes(bytes, forSelfie,
          OnSuccessListener {
            setResult(Activity.RESULT_OK)
            finish()
          }, OnFailureListener {
          Toast.makeText(this, R.string.failed_upload_image, Toast.LENGTH_LONG).show()
          hideUploadingView()
        })
      }
        ?: Toast.makeText(this, "Cannot find user account, please try to verify again", Toast.LENGTH_LONG).show()
    }
  }

  /** Check if this device has a camera */
  private fun checkCameraHardware(context: Context): Boolean {
    return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
  }

  /** Check if this device has a frontal camera */
  private fun checkFrontCamera(): Boolean {
    var cameraCount = 0
    val cameraInfo = Camera.CameraInfo()
    cameraCount = Camera.getNumberOfCameras()
    for (camIdx in 0 until cameraCount) {
      Camera.getCameraInfo(camIdx, cameraInfo)
      if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
        frontCameraIndex = camIdx
        return true
      }
    }

    return false
  }

  /** A safe way to get an instance of the Camera object. */
  private fun getCameraInstance(): Camera? {
    return try {
      if (useFront && frontCameraIndex != -1) {
        Camera.open(frontCameraIndex)
      } else {
        Camera.open() // attempt to get a Camera instance
      }
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

          cameraPreview.visibility = GONE
          camera?.stopPreview()

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