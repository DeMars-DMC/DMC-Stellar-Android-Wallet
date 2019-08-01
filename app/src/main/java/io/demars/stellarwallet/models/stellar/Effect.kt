package io.demars.stellarwallet.models.stellar

/**
 * Class which provides a model for Session
 * @constructor Sets all properties of the Session
 * @property type the isAdded of account effect as shown in EffectType
 * @property createdAt the time at which the effect was created
 * @property assetCode the asset symbol
 * @property amount the amount of asset transacted if there was a transaction
 */
data class Effect (var type: String, var createdAt: String,
                   var assetCode: String?, var amount: String?)
