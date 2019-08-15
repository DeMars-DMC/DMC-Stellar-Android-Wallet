package io.demars.stellarwallet.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File
import android.app.Activity
import android.app.AlertDialog
import android.net.Uri
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import io.demars.stellarwallet.R
import io.demars.stellarwallet.enums.CameraMode
import io.demars.stellarwallet.firebase.Firebase
import io.demars.stellarwallet.utils.ViewUtils
import timber.log.Timber
import java.io.IOException
import android.graphics.SurfaceTexture
import io.demars.stellarwallet.enums.FlashMode
import kotlinx.android.synthetic.main.activity_camera.cameraButton
import kotlinx.android.synthetic.main.activity_camera.ensureImageMessage
import kotlinx.android.synthetic.main.activity_camera.flashButton
import kotlinx.android.synthetic.main.activity_camera.galleryButton
import kotlinx.android.synthetic.main.activity_camera.imagePreview
import kotlinx.android.synthetic.main.activity_camera.mainTitle
import kotlinx.android.synthetic.main.activity_camera.retakeButton
import kotlinx.android.synthetic.main.activity_camera.sendButton
import kotlinx.android.synthetic.main.activity_camera2.*

@Suppress("DEPRECATION")
class CameraActivity : AppCompatActivity() {
  //TODO: FIX PREVIEW SIZE AND RATIO AND FLIP IMAGE 90 DEGREES
  companion object {
    private const val REQUEST_GALLERY = 111
    private const val ARG_CAMERA_MODE = "ARG_CAMERA_MODE"
    private const val PIC_FILE_NAME = "USER_TEST_ID_PICTURE"
    fun newInstance(context: Context, cameraMode: CameraMode): Intent {
      val intent = Intent(context, CameraActivity::class.java)
      intent.putExtra(ARG_CAMERA_MODE, cameraMode)
      return intent
    }
  }

  private lateinit var file: File
  private var camera: Camera? = null
  private var previewTexture: SurfaceTexture? = null
  private var cameraMode = CameraMode.ID_FRONT
  private var pictureBytes: ByteArray? = null
  private var hasCamera = false
  private var frontCameraIndex = -1
  private var useFront = false
  private var flashMode = FlashMode.OFF

  private val picture = Camera.PictureCallback { data, _ ->
    pictureBytes = data
    updateView()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_camera)

    cameraMode = intent.getSerializableExtra(ARG_CAMERA_MODE) as CameraMode

    file = File(getExternalFilesDir(null), PIC_FILE_NAME)
    hasCamera = checkCameraHardware(this)
    useFront = checkFrontCamera()

    updateView()
  }

  private fun updateView() {
    mainTitle.text = when (cameraMode) {
      CameraMode.ID_FRONT -> getString(R.string.front_of_id)
      CameraMode.ID_BACK -> getString(R.string.back_of_id)
      else -> getString(R.string.selfie_with_id)
    }

    if (pictureBytes != null) {
      cameraButton.visibility = GONE
      galleryButton.visibility = GONE
      flashButton.visibility = GONE

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

      if (hasCamera) {
        cameraButton.visibility = VISIBLE
        ensureImageMessage.visibility = GONE
        // Create an instance of Camera
        camera = getCameraInstance()
        camera?.let {
          // Create our Preview view & add it to container
          cameraPreview.addView(CameraPreview(this, it, flashMode))
        }
      } else {
        cameraButton.visibility = GONE
        ensureImageMessage.visibility = VISIBLE
        ensureImageMessage.setText(R.string.no_camera_message)
      }

      galleryButton.visibility = VISIBLE

      imagePreview.setImageDrawable(null)
      cameraPreview.visibility = VISIBLE

      flashButton.visibility = if (useFront) GONE else VISIBLE

      try {
        camera?.startPreview()
      } catch (ex: Exception) {
        onError("Failed to start Camera preview, please try again", true)
      }

      cameraButton.setOnClickListener {
        takePicture()
      }

      galleryButton.setOnClickListener {
        pickFromGallery()
      }

      flashButton.setOnClickListener {
        changeFlashMode()
      }

      cameraPreview.setOnClickListener {
        autoFocus()
      }
    }
  }

  private fun autoFocus() {
    try {
      camera?.autoFocus(null)
    } catch (ex: Exception) {
      // Auto-focus failed for some reason, just ignore
    }
  }

  private fun pickFromGallery() {
    //Create an Intent with action as ACTION_PICK
    val intent = Intent(Intent.ACTION_PICK)
    // Sets the isAdded as image/*. This ensures only components of isAdded image are selected
    intent.type = "image/*"
    //We pass an extra array with the accepted mime types. This will ensure only components with these MIME types as targeted.
    val mimeTypes = arrayOf("image/jpeg", "image/png")
    intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
    // Launching the Intent
    startActivityForResult(intent, REQUEST_GALLERY)
  }

  private fun changeFlashMode() {
    when (flashMode) {
      FlashMode.OFF -> {
        flashMode = FlashMode.ON
        flashButton.setImageResource(R.drawable.ic_flash_on)
      }
      FlashMode.ON -> {
        flashMode = FlashMode.OFF
        flashButton.setImageResource(R.drawable.ic_flash_off)
      }
    }

    updateView()
  }

  private fun takePicture() {
    try {
      camera?.takePicture(null, null, picture)
    } catch (e: Exception) {
      // Ignore
    }
  }

  private fun sendPictureToFirebase() {
    pictureBytes?.let { bytes ->
      showUploadingView()
      Firebase.uploadBytes(bytes, cameraMode,
        OnSuccessListener {
          onFirebaseResult(it)
        }, OnFailureListener {
        onError(it.localizedMessage, false)
        hideUploadingView()
      })
    }
  }

  private fun onFirebaseResult(uri: Uri?) {
    if (uri != null && uri.toString().isNotEmpty()) {
      setResult(Activity.RESULT_OK, Intent().putExtra("url", uri.toString()))
      finish()
    } else {
      onError("Downloading URL is Null", false)
      hideUploadingView()
    }
  }

  /** Check if this device has a camera */
  private fun checkCameraHardware(context: Context): Boolean {
    return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
  }

  /** Check if this device has a frontal camera */
  private fun checkFrontCamera(): Boolean {
    try {
      if (hasCamera && cameraMode == CameraMode.ID_SELFIE) {
        val cameraInfo = Camera.CameraInfo()
        val cameraCount = Camera.getNumberOfCameras()
        for (camIdx in 0..cameraCount) {
          Camera.getCameraInfo(camIdx, cameraInfo)
          if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            frontCameraIndex = camIdx
            return true
          }
        }
      }
    } catch (ex: Exception) {
      // Do nothing and return false
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
      AlertDialog.Builder(this).setMessage(e.localizedMessage).show()
      // Camera is not available (in use or does not exist)
      null // returns null if camera is unavailable
    }
  }

  /** A basic Camera preview class */
  @SuppressLint("ViewConstructor")
  internal class CameraPreview(
    context: Context,
    private val mCamera: Camera,
    private val flashMode: FlashMode
  ) : SurfaceView(context), SurfaceHolder.Callback {

    private val mHolder: SurfaceHolder = holder.apply {
      // Install a SurfaceHolder.Callback so we get notified when the
      // underlying surface is created and destroyed.
      addCallback(this@CameraPreview)
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
      try {
        //Turn flash on
        val p = mCamera.parameters
        p.flashMode = if (flashMode == FlashMode.ON) Camera.Parameters.FLASH_MODE_TORCH else Camera.Parameters.FLASH_MODE_OFF
        mCamera.parameters = p

        mCamera.setPreviewDisplay(mHolder)
        mCamera.startPreview()

      } catch (e: Exception) {
        Timber.d("Error starting camera preview: ${e.message}")
      }
    }
  }

  override fun onResume() {
    super.onResume()
    if (pictureBytes == null) {
      camera?.startPreview()
    }
  }

  override fun onStop() {
    super.onStop()
    if (pictureBytes == null) {
      camera?.stopPreview()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    camera?.stopPreview()
    camera?.release()
    previewTexture?.release()
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

  private fun onError(message: String, finish: Boolean) {
    ViewUtils.showToast(this, message)
    if (finish) finish()
  }
}