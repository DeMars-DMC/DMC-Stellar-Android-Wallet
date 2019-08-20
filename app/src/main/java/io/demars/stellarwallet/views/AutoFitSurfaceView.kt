package io.demars.stellarwallet.views

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Camera
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import io.demars.stellarwallet.enums.FlashMode
import io.demars.stellarwallet.utils.ViewUtils
import timber.log.Timber
import java.io.IOException

@SuppressLint("ViewConstructor")
class AutoFitSurfaceView constructor(
  context: Context,
  private val mCamera: Camera,
  private val flashMode: FlashMode
) : SurfaceView(context, null, 0), SurfaceHolder.Callback {
  private var mSupportedPreviewSizes: List<Camera.Size> = mCamera.parameters.supportedPreviewSizes
  var mPreviewSize: Camera.Size? = null

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val width = View.resolveSize(suggestedMinimumWidth, widthMeasureSpec)

    mPreviewSize = getOptimalPreviewSize(mSupportedPreviewSizes, width)

    mPreviewSize?.let {
      setMeasuredDimension(it.height, it.width)
    }
  }

  private fun getOptimalPreviewSize(sizes: List<Camera.Size>?, w: Int): Camera.Size? {
    if (sizes == null) return null

    var optimalSize: Camera.Size? = null

    for (size in sizes) {
      if (size.height > w) continue

      if (optimalSize != null && size.height > optimalSize.height) {
          optimalSize = size
      } else if (optimalSize == null) {
        optimalSize = size
      }
    }

    return optimalSize
  }

  private val mHolder: SurfaceHolder = holder.apply {
    // Install a SurfaceHolder.Callback so we get notified when the
    // underlying surface is created and destroyed.
    addCallback(this@AutoFitSurfaceView)
  }

  override fun surfaceCreated(holder: SurfaceHolder) {
    // Set portrait mode
    mCamera.setDisplayOrientation(90)

    try {
      //set camera to continually auto-focus
      val params = mCamera.parameters
      val focusModes = params.supportedFocusModes
      if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
        params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
      } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
        params.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
      }

      mCamera.parameters = params
    } catch (e: Exception) {
      ViewUtils.showToast(context, "Auto-focus is not supported, tap to focus")
    }

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