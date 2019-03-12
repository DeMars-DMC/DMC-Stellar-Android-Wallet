package io.demars.stellarwallet.interfaces

import androidx.lifecycle.MutableLiveData
import io.demars.stellarwallet.models.ContactsResult

interface ContactsRepository {
    enum class ContactOperationStatus {
        INSERTED,
        UPDATED,
        FAILED
    }
    fun createContact(name : String, stellarAddress : String) : Long
    fun getContactsListLiveData(forceRefresh:Boolean = false) : MutableLiveData<ContactsResult>
    fun createOrUpdateStellarAddress(name:String, address:String) : ContactOperationStatus

}