package io.demars.stellarwallet.firebase

import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import io.demars.stellarwallet.WalletApplication

object Firebase {
  //region Storage
  private fun getStorageReference(): StorageReference {
    return FirebaseStorage.getInstance().reference
  }

  private fun getCurrentUser(): FirebaseUser? {
    return FirebaseAuth.getInstance().currentUser
  }

  fun getCurrentUserUid(): String? {
    return getCurrentUser()?.uid
  }

  fun uploadBytes(bytes: ByteArray, forSelfie:Boolean, onSuccess: OnSuccessListener<UploadTask.TaskSnapshot>, onFailure: OnFailureListener) {
    val fileName = if (forSelfie) "id_selfie.jpg" else "id_photo.jpg"
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
  fun getDatabaseReference(): DatabaseReference {
    return FirebaseDatabase.getInstance().reference
  }

  fun updateUsersWallet() {
    getCurrentUserUid()?.let { uid ->
      val addressReference = getDatabaseReference().child("users")
        .child(uid).child("stellar_address")
      WalletApplication.wallet.getStellarAccountId().let { address ->
        addressReference.addValueEventListener(object : ValueEventListener {
          override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (!dataSnapshot.exists() || dataSnapshot.getValue(String::class.java) != address) {
              addressReference.setValue(address)
            }
          }

          override fun onCancelled(error: DatabaseError) {}
        })
      }
    }
  }
  //endregion
}