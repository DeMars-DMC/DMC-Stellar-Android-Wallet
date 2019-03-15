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

    const val LUMENS_ASSET_TYPE = "native"
    const val LUMENS_ASSET_CODE = "XLM"
    const val LUMENS_ASSET_NAME = "Stellar Lumens"

    const val INFLATION_DESTINATION = "GCCD6AJOYZCUAQLX32ZJF2MKFFAUJ53PVCFQI3RHWKL3V47QYE2BNAUT"

    const val LUMENS_IMAGE_RES = R.drawable.ic_logo_stellar
    const val RAND_IMAGE_RES = R.drawable.ic_logo_rand
    const val RGTS_IMAGE_RES = R.drawable.ic_logo_rgts
//        const val NKLS_IMAGE_RES = R.drawable.ic_logo_nkls

    const val RAND_ASSET_TYPE = "RAND"
    const val RAND_ASSET_NAME = "ZAR - South African"
    const val RAND_ASSET_ISSUER = "GDCPYYD2DQZ2R6VPPWRJ5B2HQIJBGUM5EMN3XZJM4OHASMSYX2PUL4BH"

    const val RGTS_ASSET_TYPE = "RGTS"
    const val RGTS_ASSET_NAME = "Zim Dollars"
    const val RGTS_ASSET_ISSUER = "GDM5A2NHVLFZMEXMONWEWV4HZEZGDZDW32TUUNW7QI4M3AEG3Z2VJ4ST"

//    const val NKLS_ASSET_TYPE = "NKLS"
//    const val NKLS_ASSET_NAME = "DéMars Nickels"
//    const val NKLS_ASSET_ISSUER = "GA3VRDTEH2C7DSXRPUEBL55JUJPMFUOUVUAPDX4T3JK5QHLSNLXZDIFC"


    // Maximum double representation in string ((2^63)-1)/(10^7)
    const val MAX_ASSET_STRING_VALUE = "922337203685.4775807"

    const val DEFAULT_TRANSACTION_FAILED_CODE = "tx_failed"
  }
}
