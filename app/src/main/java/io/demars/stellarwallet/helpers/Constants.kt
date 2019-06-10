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
    const val DEFAULT_ITEMS_LIMIT = 50

    const val INFLATION_DESTINATION = "GCCD6AJOYZCUAQLX32ZJF2MKFFAUJ53PVCFQI3RHWKL3V47QYE2BNAUT"

    const val LUMENS_ASSET_TYPE = "native"
    const val LUMENS_ASSET_CODE = "XLM"
    const val LUMENS_IMAGE_RES = R.drawable.ic_logo_stellar

    const val ZAR_ASSET_TYPE = "ZAR"
    const val ZAR_ASSET_NAME = "South African Rand"
    const val ZAR_ASSET_ISSUER = "GBW4A2PFL6Y2PMFDR2V6F4X4VSJYBZHPL53AI7YXF65VLCUNQ73WTJ6Q"
    const val ZAR_IMAGE_RES = R.drawable.ic_logo_zar

    const val DMC_ASSET_TYPE = "DMC"
    const val DMC_ASSET_NAME = "DéMars Coins"
    const val DMC_ASSET_ISSUER = "GALRZBMCK47XLHQQXQSPCNJ375NGB6LP6RCBIGPXQAWIE2A4UCDTGQ52"
    const val DMC_IMAGE_RES = R.drawable.ic_main_logo

    // TODO: Switch to production when app is ready
    const val RTGS_ASSET_TYPE = "RTGSTEST"
    const val RTGS_ASSET_NAME = "Zimbabwean Dollars"
    const val RTGS_ASSET_ISSUER = "GANTABE3SPS3PR3FOJMSPMOF667XGNTKML5CYQAQOL3SHVVDDNUJAPLG"
    const val RTGS_IMAGE_RES = R.drawable.ic_logo_rtgs

    const val FEE_ACCOUNT = "GDXYQD3YPBLRZF7D3LKSFXBFSG5JXRRZHTWMPPTUWEA337RPZZYDO4QF"

    // Maximum double representation in string ((2^63)-1)/(10^7)
    const val MAX_ASSET_STRING_VALUE = "922337203685.4775807"

    const val DEFAULT_TRANSACTION_FAILED_CODE = "tx_failed"

    const val URL_TERMS_AND_CONDITIONS = "https://docs.google.com/document/d/1T4QLKk0UCBMUo8v8hofGJ4CD1P35GkorLH2sE0Y1iKs/edit?usp=sharing"
    const val URL_QUICK_START = "https://docs.google.com/document/d/1qSSWxP9cxpyBLcXkB5Dv4ClIj_lam2CyfvTG9mpPgxs/edit?usp=sharing"
  }
}
