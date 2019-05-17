package io.demars.stellarwallet.models

import io.demars.stellarwallet.WalletApplication
import io.demars.stellarwallet.helpers.Constants
import org.stellar.sdk.*
import org.stellar.sdk.responses.Response
import org.stellar.sdk.responses.TradeResponse
import org.stellar.sdk.responses.TransactionResponse
import org.stellar.sdk.responses.effects.*
import org.stellar.sdk.responses.operations.*
import org.threeten.bp.Instant

class WalletHeterogeneousWrapper {

  companion object {
    const val TOTAL_INDEX = 0
    const val AVAILABLE_INDEX = 1
    const val PAIR_INDEX = 2
    const val EFFECTS_LIST_INDEX = 3
  }

  var array: ArrayList<Any> = ArrayList()
  private var availableBalanceOffset = 0

  //region Update methods

  fun updateTotalBalance(balance: TotalBalance) {
    array.removeAt(TOTAL_INDEX)
    array.add(TOTAL_INDEX, balance)
  }

  fun updateAvailableBalance(balance: AvailableBalance) {
    if (WalletApplication.userSession.getSessionAsset().assetCode == Constants.LUMENS_ASSET_TYPE) {
      if (availableBalanceOffset != 0) {
        availableBalanceOffset = 0
        array.add(AVAILABLE_INDEX, balance)
      } else {
        array.removeAt(AVAILABLE_INDEX)
        array.add(AVAILABLE_INDEX, balance)
      }
    } else {
      hideAvailableBalance()
    }
  }

  fun updatePair(p: Pair<*, *>) {
    array.removeAt(PAIR_INDEX - availableBalanceOffset)
    array.add(PAIR_INDEX - availableBalanceOffset, p)
  }

  fun hidePair() {
    if (array.size > PAIR_INDEX - availableBalanceOffset) {
      array.removeAt(PAIR_INDEX - availableBalanceOffset)
    }
  }

  fun updateOperationsList(activeAsset: String, list: ArrayList<Pair<OperationResponse, String?>>?) {
    array.subList(EFFECTS_LIST_INDEX - availableBalanceOffset, array.size).clear()
    addFilteredOperations(activeAsset, list)
  }

  fun updateEffectsList(activeAsset: String, list: ArrayList<EffectResponse>?) {
    array.subList(EFFECTS_LIST_INDEX - availableBalanceOffset, array.size).clear()
    addFilteredEffects(activeAsset, list)
  }

  fun updateTransactionsList(activeAsset: String, list: ArrayList<TransactionResponse>?) {
    addFilteredTransactions(activeAsset, list)
  }

  fun updateTradesList(activeAsset: String, list: ArrayList<TradeResponse>?) {
    addFilteredTrades(activeAsset, list)
  }
  //endregion

  fun hideAvailableBalance() {
    if (availableBalanceOffset == 0) {
      array.removeAt(AVAILABLE_INDEX)
      availableBalanceOffset = 1
    }
  }

  private fun addFilteredOperations(activeAsset: String, list: ArrayList<Pair<OperationResponse, String?>>?) {
    val filteredOperations = getFilteredOperations(list, activeAsset)
    if (filteredOperations != null) {
      array.addAll(convertResponseToOperation(filteredOperations))
    }
  }

  private fun addFilteredEffects(activeAsset: String, list: ArrayList<EffectResponse>?) {
    val filteredEffects = getFilteredEffects(list, activeAsset)
    if (filteredEffects != null) {
      array.addAll(convertEffectsToAccountEffects(activeAsset, filteredEffects))
    }
  }

  private fun addFilteredTransactions(activeAsset: String, list: ArrayList<TransactionResponse>?) {
    val filteredTransactions = getFilteredTransactions(list, activeAsset)
    if (filteredTransactions != null) {
      array.addAll(convertResponseToTransaction(filteredTransactions))
    }
  }

  private fun addFilteredTrades(activeAsset: String, list: ArrayList<TradeResponse>?) {
    val filteredTrades = getFilteredTrades(list, activeAsset)
    if (filteredTrades != null) {
      array.addAll(convertResponseToTrade(activeAsset, filteredTrades))
      array.subList(EFFECTS_LIST_INDEX - availableBalanceOffset, array.size).sortByDescending {
        when (it) {
          is Trade -> Instant.parse(it.createdAt).toEpochMilli()
          else -> Instant.parse((it as Operation).createdAt).toEpochMilli()
        }
      }
    }
  }

  private fun getFilteredEffects(list: ArrayList<EffectResponse>?, assetType: String): ArrayList<EffectResponse>? {
    if (list == null) return null

    return (list.filter {
      (it.type == EffectType.RECEIVED.value && getAssetCode(it) == assetType) ||
        (it.type == EffectType.SENT.value && getAssetCode(it) == assetType) ||
        (it.type == EffectType.TRUSTLINE_CREATED.value && getAssetCode(it) == assetType) ||
        (it.type == EffectType.TRUSTLINE_REMOVED.value && getAssetCode(it) == assetType) ||
        (it.type == EffectType.TRUSTLINE_UPDATED.value && getAssetCode(it) == assetType) ||
        (it.type == EffectType.ACCOUNT_INFLATION_DESTINATION_UPDATED.value && assetType == Constants.LUMENS_ASSET_TYPE) ||
        (it.type == EffectType.CREATED.value && assetType == Constants.LUMENS_ASSET_TYPE) ||
        (it.type == EffectType.SIGNER_UPDATED.value && assetType == Constants.LUMENS_ASSET_TYPE) ||
        (it.type == EffectType.SIGNER_REMOVED.value && assetType == Constants.LUMENS_ASSET_TYPE) ||
        (it.type == EffectType.SIGNER_CREATED.value && assetType == Constants.LUMENS_ASSET_TYPE) ||
        (it.type == EffectType.TRADE.value && (convertAsset((it as TradeEffectResponse).boughtAsset) == assetType ||
          convertAsset(it.boughtAsset) == assetType))

    } as ArrayList)
  }

  private fun getFilteredOperations(list: ArrayList<Pair<OperationResponse, String?>>?, assetType: String): ArrayList<Pair<OperationResponse, String?>>? {
    if (list == null) return null

    val newList = ArrayList<Pair<OperationResponse, String?>>(list)
    val returnList = ArrayList<Pair<OperationResponse, String?>>()
    for (it in newList) {
      when (it.first) {
        is CreateAccountOperationResponse -> {
          if (assetType == Constants.LUMENS_ASSET_TYPE) {
            returnList.add(it)
          }
        }
        is PaymentOperationResponse -> {
          if (assetType == getAssetCode(it.first)) {
            returnList.add(it)
          }
        }
        is PathPaymentOperationResponse -> {
        }
        is ChangeTrustOperationResponse -> {
          if (assetType == getAssetCode(it.first)) {
            returnList.add(it)
          }
        }
        is AllowTrustOperationResponse -> {
          if (assetType == getAssetCode(it.first)) {
            returnList.add(it)
          }
        }
        is ManageSellOfferOperationResponse -> {
        }
        is ManageBuyOfferOperationResponse -> {
        }
        else -> {}
      }
    }

    return returnList
  }

  private fun getFilteredTransactions(list: ArrayList<TransactionResponse>?, assetType: String): ArrayList<TransactionResponse>? {
    if (list == null) return null

    return (list.filter {
      it.isSuccessful
    } as ArrayList)
  }

  private fun getFilteredTrades(list: ArrayList<TradeResponse>?, assetType: String): ArrayList<TradeResponse>? {
    if (list == null) return null

    return (list.filter {
      convertAsset(it.baseAsset) == assetType || convertAsset(it.counterAsset) == assetType
    } as ArrayList)
  }

  private fun convertEffectsToAccountEffects(activeAsset: String, list: ArrayList<EffectResponse>): ArrayList<Any> {
    return list.map {
      if (it is TradeEffectResponse) {
        return@map TradeEffect(activeAsset, it.type, it.createdAt, convertAsset(it.boughtAsset), convertAsset(it.soldAsset),
          it.boughtAmount, it.soldAmount)
      } else {
        return@map AccountEffect(it.type, it.createdAt, getAssetCode(it), getAmount(it))
      }
    } as ArrayList
  }

  private fun convertResponseToOperation(list: ArrayList<Pair<OperationResponse, String?>>): ArrayList<Operation> {
    return list.map {
      val operation = it.first
      val memo = it.second
      return@map Operation(operation.id.toString(), operation.sourceAccount.accountId,
        operation.type, operation.createdAt, operation.isTransactionSuccessful,
        operation.transactionHash, operation.links.transaction.href,
        getAmount(operation), getFrom(operation), getTo(operation),
        getAssetCode(operation), getCounterAssetCode(operation), getPrice(operation), memo)
    } as ArrayList
  }

  private fun convertResponseToTransaction(list: ArrayList<TransactionResponse>): ArrayList<Transaction> {
    return list.map {
      return@map Transaction("Transaction", it.createdAt, "XLM", "amount",
        convertMemo(it.memo), it.sourceAccount.accountId, it.feePaid.toString(), it.operationCount, it.isSuccessful)
    } as ArrayList
  }

  private fun convertResponseToTrade(activeAsset: String, list: ArrayList<TradeResponse>): ArrayList<Trade> {
    return list.map {
      return@map Trade(activeAsset, it.ledgerCloseTime, it.offerId,
        it.isBaseSeller, convertPrice(it.price),
        it.baseOfferId, it.counterOfferId, convertAsset(it.baseAsset), convertAsset(it.counterAsset),
        it.baseAssetType, it.counterAssetType, it.baseAmount, it.counterAmount,
        it.baseAccount.accountId, it.counterAccount.accountId)
    } as ArrayList
  }

  private fun getAssetCode(response: Response): String? {
    return when (response) {
      is AccountCreditedEffectResponse -> convertAsset(response.asset)
      is AccountDebitedEffectResponse -> convertAsset(response.asset)
      is PaymentOperationResponse -> convertAsset(response.asset)
      is PathPaymentOperationResponse -> convertAsset(response.asset)
      is ManageSellOfferOperationResponse -> convertAsset(response.sellingAsset)
      is ManageBuyOfferOperationResponse -> convertAsset(response.sellingAsset)
      is ChangeTrustOperationResponse -> convertAsset(response.asset)
      is AllowTrustOperationResponse -> convertAsset(response.asset)
      is TrustlineCreatedEffectResponse -> (response.asset as AssetTypeCreditAlphaNum).code
      is TrustlineRemovedEffectResponse -> (response.asset as AssetTypeCreditAlphaNum).code
      is TrustlineUpdatedEffectResponse -> (response.asset as AssetTypeCreditAlphaNum).code
      else -> null
    }
  }

  private fun getAmount(response: Response): String? {
    return when (response) {
      is AccountCreditedEffectResponse -> response.amount
      is AccountDebitedEffectResponse -> response.amount
      is AccountCreatedEffectResponse -> response.startingBalance
      is CreateAccountOperationResponse -> response.startingBalance
      is PaymentOperationResponse -> response.amount
      is PathPaymentOperationResponse -> response.amount
      is ManageSellOfferOperationResponse -> response.amount
      is ManageBuyOfferOperationResponse -> response.amount
      else -> null
    }
  }

  private fun convertAsset(asset: Asset): String {
    return if (asset is AssetTypeCreditAlphaNum) {
      asset.code
    } else {
      (asset as AssetTypeNative).type
    }
  }

  private fun convertMemo(memo: Memo): String? {
    return if (memo is MemoText) memo.text else null
  }

  private fun getFrom(response: OperationResponse): String? {
    return when (response) {
      is CreateAccountOperationResponse -> response.funder.accountId
      is PaymentOperationResponse -> response.from.accountId
      is PathPaymentOperationResponse -> response.from.accountId
      else -> null
    }
  }

  private fun getTo(response: OperationResponse): String? {
    return when (response) {
      is CreateAccountOperationResponse -> response.account.accountId
      is PaymentOperationResponse -> response.to.accountId
      is PathPaymentOperationResponse -> response.to.accountId
      else -> null
    }
  }

  private fun getCounterAssetCode(response: OperationResponse): String? {
    return when (response) {
      is ManageSellOfferOperationResponse -> convertAsset(response.buyingAsset)
      is ManageBuyOfferOperationResponse -> convertAsset(response.buyingAsset)
      else -> null
    }
  }

  private fun getPrice(response: OperationResponse): String? {
    return when (response) {
      is ManageSellOfferOperationResponse -> response.price
      is ManageBuyOfferOperationResponse -> response.price
      else -> null
    }
  }

  private fun convertPrice(price: Price?): String? {
    if (price == null) return null

    return (price.numerator.toDouble() / price.denominator.toDouble()).toString()
  }
}
