package io.demars.stellarwallet.firebase

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.demars.stellarwallet.enums.CameraMode

object Firebase {
  //region Storage
  private fun getStorageReference(): StorageReference {
    return FirebaseStorage.getInstance().reference
  }

  fun getCurrentUser(): FirebaseUser? {
    return FirebaseAuth.getInstance().currentUser
  }

  fun getCurrentUserUid(): String? {
    return getCurrentUser()?.uid
  }

  fun uploadBytes(bytes: ByteArray, cameraMode: CameraMode, onSuccess: OnSuccessListener<UploadTask.TaskSnapshot>, onFailure: OnFailureListener) {
    val fileName = when (cameraMode) {
      CameraMode.ID_FRONT -> "id_front.jpg"
      CameraMode.ID_BACK -> "id_back.jpg"
      else -> "id_selfie.jpg"
    }

    getCurrentUser()?.let {
      getStorageReference()
        .child("images/users/${it.uid}")
        .child(fileName)
        .putBytes(bytes)
        .addOnSuccessListener(onSuccess)
        .addOnFailureListener(onFailure)
    }
  }
  //endregion

  //region Real-time Database
  fun getDatabaseReference(): DatabaseReference = FirebaseDatabase.getInstance().reference


  fun getUserStellarAddress(listener: ValueEventListener) =
    getCurrentUserUid()?.let { uid ->
      getStellarAddressRef(uid)
        .addValueEventListener(listener)
    }

  fun getStellarAddressRef(uid: String): DatabaseReference = getDatabaseReference().child("users")
    .child(uid).child("stellar_address")

  fun getUser(uid: String, listener: ValueEventListener) {
    getDatabaseReference().child("users")
      .child(uid).removeEventListener(listener)
    getDatabaseReference().child("users")
      .child(uid).addValueEventListener(listener)
  }

  fun removeUserListener(listener: ValueEventListener) {
    getCurrentUserUid()?.let { uid ->
      getDatabaseReference().child("users")
        .child(uid).removeEventListener(listener)
    }
  }
  //endregion
}