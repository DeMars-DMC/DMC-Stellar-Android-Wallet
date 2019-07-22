package io.demars.stellarwallet.interfaces

import io.demars.stellarwallet.models.Contact

interface ContactListener {
  fun onContactSelected(contact: Contact)
  fun addAddressToContact(contact: Contact)
  fun onPayToContact(contact: Contact)
}