package io.demars.stellarwallet.api.firebase

import android.net.Uri
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.InstanceIdResult
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import io.demars.stellarwallet.enums.CameraMode
import org.jetbrains.anko.doAsync
import org.json.JSONException
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import kotlin.collections.HashMap

object Firebase {

  //region Properties
  private var uid: String = ""
  private var idToken: String = ""
  private var registrationToken: String = ""
  private var notificationKey = ""
  private var initialized = false
  private var operationWhenInit = ""
  //endregion

  //region Init
  fun signOut() {
    removeFromFcmGroup()
    FirebaseAuth.getInstance().signOut()
  }

  private fun removeFromFcmGroup() {
    if (initialized) {
      manageGroupAsync(uid, "remove")
    } else {
      getCurrentUserUid()?.let { uid ->
        initFcm(uid, "remove")
      }
    }
  }
//endregion

  //region Storage
  private fun getStorageReference(): StorageReference = FirebaseStorage.getInstance().reference

  fun getCurrentUser(): FirebaseUser? = FirebaseAuth.getInstance().currentUser
  fun getCurrentUserUid(): String? = getCurrentUser()?.uid

  fun uploadBytes(bytes: ByteArray, cameraMode: CameraMode, onSuccess: OnSuccessListener<Uri?>, onFailure: OnFailureListener) {
    val fileName = when (cameraMode) {
      CameraMode.ID_FRONT -> "id_front.jpg"
      CameraMode.ID_BACK -> "id_back.jpg"
      else -> "id_selfie.jpg"
    }

    getCurrentUser()?.let {
      val fileRef = getStorageReference()
        .child("images/users/${it.uid}")
        .child(fileName)

      fileRef.putBytes(bytes)
        .addOnSuccessListener {
          // File uploaded, let's get download URL
          fileRef.downloadUrl
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure)
        }.addOnFailureListener(onFailure)
    } ?: onFailure.onFailure(Exception("User not authorized"))
  }
//endregion

  //region Real-time Database
  fun getDatabaseReference(): DatabaseReference = FirebaseDatabase.getInstance().reference

  fun getUserRef(uid: String): DatabaseReference = getDatabaseReference().child("users").child(uid)

  fun getUserStellarAddress(listener: ValueEventListener) =
    getCurrentUserUid()?.let { uid ->
      getStellarAddressRef(uid)
        .addValueEventListener(listener)
    }

  fun updateStellarAddress(stellarAddress: String) {
    getCurrentUserUid()?.let { uid ->
      getStellarAddressRef(uid)
        .setValue(stellarAddress)
    }
  }

  fun getStellarAddressRef(uid: String): DatabaseReference = getDatabaseReference().child("users")
    .child(uid).child("stellar_address")

  fun getNotificationKeyRef(uid: String): DatabaseReference = getDatabaseReference().child("users")
    .child(uid).child("notification_key")

  fun getUserBanksZarRef(uid: String): DatabaseReference = getDatabaseReference().child("users")
    .child(uid).child("banksZAR")

  fun getUserBanksNgntRef(uid: String): DatabaseReference = getDatabaseReference().child("users")
    .child(uid).child("banksNGNT")

  fun getAssetRef(assetCode: String): DatabaseReference = getDatabaseReference().child("assets")
    .child(assetCode)

  fun getAssetFresh(assetCode: String, listener: ValueEventListener) {
    getAssetRef(assetCode).removeEventListener(listener)
    getAssetRef(assetCode).addValueEventListener(listener)
  }

  fun getUserFresh(listener: ValueEventListener) {
    getCurrentUserUid()?.let { uid ->
      getUserRef(uid).removeEventListener(listener)
      getUserRef(uid).addValueEventListener(listener)
    }
  }

  fun removeUserListener(listener: ValueEventListener) {
    getCurrentUserUid()?.let { uid ->
      getUserRef(uid).removeEventListener(listener)
    }
  }

  fun removeStellarAddressListener(listener: ValueEventListener) {
    getCurrentUserUid()?.let { uid ->
      getStellarAddressRef(uid).removeEventListener(listener)
    }
  }

  fun removeAssetListener(assetCode: String, listener: ValueEventListener) {
    getAssetRef(assetCode).removeEventListener(listener)
  }
  //endregion

  //region FCM
  private val idTokenListener = OnSuccessListener<GetTokenResult> { result ->
    if (idToken != result.token) {
      idToken = result.token ?: ""
      onInitFinished()
    }
  }

  private val instanceIdListener = OnSuccessListener<InstanceIdResult> { result ->
    if (registrationToken != result.token) {
      registrationToken = result.token
      onInitFinished()
    }
  }

  private fun onInitFinished() {
    if (idToken.isNotEmpty() && registrationToken.isNotEmpty()) {
      Timber.d("Firebase initialized for FCM : uid - $uid, idToken - $idToken, registrationToken - $registrationToken")
      initialized = true
      if (operationWhenInit.isNotEmpty()) {
        manageGroupAsync(uid, operationWhenInit)
        operationWhenInit = ""
      }
    }
  }

  fun initFcm(uid: String, operationWhenInit: String = "") {
    this.uid = uid
    this.operationWhenInit = operationWhenInit
    FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener(instanceIdListener)
    FirebaseAuth.getInstance().currentUser?.getIdToken(false)?.addOnSuccessListener(idTokenListener)
  }

  @Throws(IOException::class, JSONException::class)
  fun manageGroupAsync(uid: String, operation: String) {
    if (uid.isEmpty()) return

    doAsync {
      // First trying to get notification key for this user group
      FirebaseApi.api.getGroup(uid).enqueue(object : Callback<HashMap<String, String>> {
        override fun onResponse(call: Call<HashMap<String, String>>, response: Response<HashMap<String, String>>) {
          notificationKey = response.body()?.get("notification_key") ?: ""

          // HTTP request
          val body = manageFcmGroupBody(operation, notificationKey)

          // Adding new device to group in case this group was created on some other device
          FirebaseApi.api.manageGroup(body).enqueue(object : Callback<HashMap<String, String>> {
            override fun onResponse(call: Call<HashMap<String, String>>, response: Response<HashMap<String, String>>) {
              // Save notification_key to user if successfully created it
              if (response.isSuccessful) {
                notificationKey = if (operation == "remove") "" else
                  response.body()?.get("notification_key") ?: ""
                saveNotificationKey(uid, notificationKey)
              }
            }

            override fun onFailure(call: Call<HashMap<String, String>>, t: Throwable) {
              Timber.e(t)
            }
          })
        }

        override fun onFailure(call: Call<HashMap<String, String>>, t: Throwable) {
          Timber.e(t)
        }
      })
    }
  }

  private fun manageFcmGroupBody(operation: String, groupKey: String): HashMap<String, Any> {
    val body = HashMap<String, Any>()
    body["operation"] = if (groupKey.isNotEmpty() && operation == "create") "add" else operation
    body["notification_key_name"] = uid
    body["registration_ids"] = listOf(registrationToken)
    if (groupKey.isNotEmpty()) {
      body["notification_key"] = groupKey
    }
    return body
  }

  private fun saveNotificationKey(uid: String, notificationKey: String) {
    // Updating value in database every time so we can use it to send messages from web
    getNotificationKeyRef(uid).setValue(notificationKey)
  }
//endregion
}