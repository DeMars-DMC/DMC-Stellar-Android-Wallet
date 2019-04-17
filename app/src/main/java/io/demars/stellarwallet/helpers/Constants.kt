package io.demars.stellarwallet.helpers

import io.demars.stellarwallet.R

class Constants {

  companion object {
    const val DEFAULT_ACCOUNT_BALANCE = "0.00"
    const val STELLAR_ADDRESS_LENGTH = 56
    const val USER_INDEX = 0
    const val MINIMUM_BALANCE_INCREMENT = 0.5
    const val BASE_RESERVE = 0.5

    const val UNKNOWN_ERROR = 520

    const val INFLATION_DESTINATION = "GCCD6AJOYZCUAQLX32ZJF2MKFFAUJ53PVCFQI3RHWKL3V47QYE2BNAUT"

    const val LUMENS_ASSET_TYPE = "native"
    const val LUMENS_ASSET_CODE = "XLM"
    const val LUMENS_ASSET_NAME = "Stellar Lumens"
    const val LUMENS_IMAGE_RES = R.drawable.ic_logo_stellar

    const val ZAR_ASSET_TYPE = "ZAR"
    const val ZAR_ASSET_NAME = "South African Rand"
    const val ZAR_ASSET_ISSUER = "GBW4A2PFL6Y2PMFDR2V6F4X4VSJYBZHPL53AI7YXF65VLCUNQ73WTJ6Q"
    const val ZAR_IMAGE_RES = R.drawable.ic_logo_zar

    const val RTGS_ASSET_TYPE = "RTGS"
    const val RTGS_ASSET_NAME = "Zimbabwean Dollars"
    const val RTGS_ASSET_ISSUER = "GANVBQ5JXPMDXH7QKNM3FRFBO23VJYQAQ42ZNXDWUZHLEIOK7JNPOCN3"
    const val RTGS_IMAGE_RES = R.drawable.ic_logo_rtgs

    const val DMC_ASSET_TYPE = "DMC"
    const val DMC_ASSET_NAME = "DéMars Coins"
    const val DMC_ASSET_ISSUER = "GALRZBMCK47XLHQQXQSPCNJ375NGB6LP6RCBIGPXQAWIE2A4UCDTGQ52"
    const val DMC_IMAGE_RES = R.drawable.ic_main_logo

    const val USD_ASSET_TYPE = "USD"
    const val USD_ASSET_NAME = "US Dollars"
    const val USD_ASSET_ISSUER = "GBXKY77EBFFUM35EDEMU3BB4YW6TFPMHOQLDED5C2LAIMIADQJADT4IP"
    const val USD_IMAGE_RES = R.drawable.ic_logo_usd

    // Maximum double representation in string ((2^63)-1)/(10^7)
    const val MAX_ASSET_STRING_VALUE = "922337203685.4775807"

    const val DEFAULT_TRANSACTION_FAILED_CODE = "tx_failed"

    const val URL_TERMS_AND_CONDITIONS = "https://docs.google.com/document/d/1T4QLKk0UCBMUo8v8hofGJ4CD1P35GkorLH2sE0Y1iKs/edit?usp=sharing"
  }
}
