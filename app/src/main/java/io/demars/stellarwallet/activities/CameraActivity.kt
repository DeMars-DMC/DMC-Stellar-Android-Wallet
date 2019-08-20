package io.demars.stellarwallet.activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File
import android.app.Activity
import android.app.AlertDialog
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import io.demars.stellarwallet.R
import io.demars.stellarwallet.enums.CameraMode
import io.demars.stellarwallet.firebase.Firebase
import io.demars.stellarwallet.utils.ViewUtils
import android.graphics.SurfaceTexture
import android.os.Build
import android.view.View.*
import androidx.core.content.ContextCompat
import io.demars.stellarwallet.enums.FlashMode
import io.demars.stellarwallet.views.AutoFitSurfaceView
import kotlinx.android.synthetic.main.activity_camera.backButton
import kotlinx.android.synthetic.main.activity_camera.cameraButton
import kotlinx.android.synthetic.main.activity_camera.ensureImageMessage
import kotlinx.android.synthetic.main.activity_camera.flashButton
import kotlinx.android.synthetic.main.activity_camera.galleryButton
import kotlinx.android.synthetic.main.activity_camera.imagePreview
import kotlinx.android.synthetic.main.activity_camera.mainTitle
import kotlinx.android.synthetic.main.activity_camera.retakeButton
import kotlinx.android.synthetic.main.activity_camera.sendButton

@Suppress("DEPRECATION")
class CameraActivity : AppCompatActivity() {
  companion object {
    private const val REQUEST_GALLERY = 111
    private const val REQUEST_CAMERA_PERMISSION = 222
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
  private var cameraMode = CameraMode.ID_FRONT
  private var pictureBytes: ByteArray? = null
  private var hasCamera = false
  private var frontCameraIndex = -1
  private var wantUseFront = false
  private var flashMode = FlashMode.OFF
  private var previewView: AutoFitSurfaceView? = null

  private val picture = Camera.PictureCallback { data, _ ->
    pictureBytes = data
    val rotateAngle = if (useFrontCamera()) 270f else 90f
    imagePreview.setImageBitmap(ViewUtils.handleBytes(data, rotateAngle))
    imagePreview.visibility = VISIBLE
    updateView()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_camera)

    cameraMode = intent.getSerializableExtra(ARG_CAMERA_MODE) as CameraMode

    file = File(getExternalFilesDir(null), PIC_FILE_NAME)
    hasCamera = checkCameraHardware(this)
    wantUseFront = checkFrontCamera()

    updateView()
  }

  private fun updateView() {
    backButton.setOnClickListener {
      super.onBackPressed()
    }

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

        openCamera()
      } else {
        cameraButton.visibility = GONE
        ensureImageMessage.visibility = VISIBLE
        ensureImageMessage.setText(R.string.no_camera_message)
      }

      galleryButton.visibility = VISIBLE

      imagePreview.setImageDrawable(null)
      imagePreview.visibility = GONE

      cameraPreview.visibility = VISIBLE

      flashButton.visibility = if (useFrontCamera()) GONE else VISIBLE

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

  private fun openCamera() {
    val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
    if (permission != PackageManager.PERMISSION_GRANTED) {
      requestCameraPermission()
      return
    }

    // Create an instance of Camera
    camera = getCameraInstance()
    camera?.let {
      // Create our Preview view & add it to container
      previewView = AutoFitSurfaceView(this, it, flashMode)
      cameraPreview.removeAllViews()
      cameraPreview.addView(previewView)
    }
  }

  private fun useFrontCamera(): Boolean {
    return wantUseFront && frontCameraIndex != -1
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
      if (wantUseFront && frontCameraIndex != -1) {
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
          imagePreview.visibility = VISIBLE

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

    progressBar.visibility = VISIBLE

    retakeButton.visibility = GONE
    sendButton.visibility = GONE
  }

  private fun hideUploadingView() {
    ensureImageMessage.setText(R.string.uploading_photo_error)

    progressBar.visibility = GONE

    retakeButton.visibility = VISIBLE
    sendButton.visibility = VISIBLE
  }

  private fun onError(message: String, finish: Boolean) {
    ViewUtils.showToast(this, message)
    if (finish) finish()
  }

  //region Permission
  private fun requestCameraPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    if (requestCode == REQUEST_CAMERA_PERMISSION) {
      if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        openCamera()
      } else {
        onError("Permission should be granted", true)
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
  }
  //endregion
}