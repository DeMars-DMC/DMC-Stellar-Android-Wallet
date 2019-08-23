package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.*
import android.text.style.ClickableSpan
import android.view.View
import androidx.appcompat.app.AlertDialog
import io.demars.stellarwallet.R
import io.demars.stellarwallet.api.firebase.model.DmcUser
import io.demars.stellarwallet.api.firebase.Firebase
import io.demars.stellarwallet.interfaces.AfterTextChanged
import io.demars.stellarwallet.views.pin.PinLockView
import kotlinx.android.synthetic.main.activity_deposit.*
import kotlinx.android.synthetic.main.activity_deposit.amountTextView
import kotlinx.android.synthetic.main.activity_deposit.numberKeyboard
import androidx.core.content.ContextCompat
import android.text.method.LinkMovementMethod
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import io.demars.stellarwallet.DmcApp
import io.demars.stellarwallet.api.firebase.model.DmcAsset
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.helpers.MailHelper
import io.demars.stellarwallet.interfaces.SuccessErrorCallback
import io.demars.stellarwallet.api.horizon.model.HorizonException
import io.demars.stellarwallet.api.cowrie.model.DepositResponse
import io.demars.stellarwallet.api.cowrie.model.WithdrawResponse
import io.demars.stellarwallet.api.cowrie.CowrieApi
import io.demars.stellarwallet.api.horizon.Horizon
import io.demars.stellarwallet.api.stellarport.StellarPort
import io.demars.stellarwallet.models.local.*
import io.demars.stellarwallet.utils.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class DepositActivity : BaseActivity(), PinLockView.DialerListener {
  enum class Mode {
    DEPOSIT, WITHDRAW
  }

  companion object {
    private const val ARG_MODE = "ARG_MODE"
    private const val ARG_ASSET_CODE = "ARG_ASSET_CODE"
    private const val ARG_ASSET_ISSUER = "ARG_ASSET_ISSUER"
    fun newInstance(context: Context, mode: Mode, assetCode: String, assetIssuer: String): Intent =
      Intent(context, DepositActivity::class.java).apply {
        putExtra(ARG_MODE, mode)
        putExtra(ARG_ASSET_CODE, assetCode)
        putExtra(ARG_ASSET_ISSUER, assetIssuer)
      }
  }

  private lateinit var dmcUser: DmcUser
  private lateinit var cowrieApi: CowrieApi
  private var mode = Mode.DEPOSIT
  private var modeString = ""
  private var assetCode = ""
  private var assetIssuer = ""
  private var supportedBanks = HashMap<String, String>()
  private var userBankAccounts = ArrayList<DmcUser.BankAccount>()
  private var bankToAdd = DmcUser.BankAccount()
  private var accountDest = ""
  private var anchorAccount = ""
  private var anchorMemo = ""
  private var amount = 0.0
  private var minAmount = 0.0
  private var maxAmount = 0.0
  private var amountText = "0"
  private var maxAmountText = "0.0"
  private var maxDecimals = 2
  private var termsChecked = false

  private val userListener = object : ValueEventListener {
    override fun onDataChange(dataSnapshot: DataSnapshot) {
      dataSnapshot.getValue(DmcUser::class.java)?.let { user ->
        onUserFetched(user)
      } ?: finishWithToast("Can't find user. Please try again")
    }

    override fun onCancelled(error: DatabaseError) {
      finishWithToast(error.message)
    }
  }

  private val assetListener = object : ValueEventListener {
    override fun onDataChange(data: DataSnapshot) {
      data.getValue(DmcAsset::class.java)?.let {
        supportedBanks = it.banks
        updateView()
      } ?: finishWithToast("Can't fetch banks for $assetCode")
    }

    override fun onCancelled(error: DatabaseError) {
      finishWithToast(error.message)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_deposit)

    checkIntent()

    if (!AssetUtils.isDepositSupported(assetCode, assetIssuer)) {
      if (mode == Mode.DEPOSIT) {
        finishWithToast(R.string.deposit_not_supported)
      } else {
        finishWithToast(R.string.withdrawal_not_supported)
      }
      return
    } else if (!DmcApp.wallet.isVerified()) {
      finishWithToast(if (mode == Mode.DEPOSIT) R.string.deposit_not_verified
      else R.string.withdraw_not_verified)
      return
    }

    cowrieApi = CowrieApi.Creator.create()

    setupUI()

    Firebase.getCurrentUser()?.let { _ ->
      Firebase.getUserFresh(userListener)
      if (AssetUtils.isZar(assetCode, assetIssuer) ||
        AssetUtils.isNgnt(assetCode, assetIssuer))
        Firebase.getAssetFresh(assetCode, assetListener)
    } ?: finishWithToast("Can't find user. Please try again")
  }

  private fun checkIntent() {
    intent?.extras?.let {
      mode = it.get(ARG_MODE) as Mode
      modeString = getString(if (mode == Mode.DEPOSIT)
        R.string.deposit else R.string.withdraw)

      assetCode = it.getString(ARG_ASSET_CODE, "")
      assetIssuer = it.getString(ARG_ASSET_ISSUER, "")
      maxDecimals = AssetUtils.getMaxDecimals(assetCode)

      when (mode) {
        Mode.DEPOSIT -> {
          maxAmount = 5000.0
          maxAmountText = "5000.00"
        }
        Mode.WITHDRAW -> {
          val available = AccountUtils.getAvailableBalance(assetCode)
          maxAmount = available.toDouble()
          maxAmountText = StringFormat.truncateDecimalPlaces(available, maxDecimals)
          when (assetCode) {
            Constants.NGNT_ASSET_CODE -> minAmount = 500.0
          }
        }
      }
    }
  }

  private fun setupUI() {
    backButton.setOnClickListener {
      onBackPressed()
    }

    val title = "$modeString $assetCode"
    titleView.text = title

    numberKeyboard.mDialerListener = this

    submitButton.setOnClickListener {
      hideAmountInput()
      showDepositAmount()
    }

    bankPicker?.setOnClickListener {
      showBankPicker()
    }

    accountNumberInput?.inputType = if (isBank()) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
    accountNumberInput?.addTextChangedListener(object : AfterTextChanged() {
      override fun afterTextChanged(editable: Editable) {
        onAccountInput(editable.trim().toString())
      }
    })

    accountTypePicker?.setOnClickListener {
      showAccountTypePicker()
    }

    addBankButton.setOnClickListener {
      when {
        isBank() -> addBank()
        isCrypto() -> addDestination()
      }
    }

    agreeCheckBox.setOnCheckedChangeListener { _, isChecked ->
      termsChecked = isChecked
      updateConfirmButton()
    }

    // Agree Terms & Conditions clickable text
    val clickableSpan = object : ClickableSpan() {
      override fun onClick(widget: View) {
        AlertDialog.Builder(this@DepositActivity)
          .setTitle(R.string.dialog_deposit_terms_title)
          .setMessage(R.string.dialog_deposit_terms_message)
          .setPositiveButton(R.string.back_to_deposit) { _, _ -> }
          .show()
      }

      override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        val color = ContextCompat.getColor(this@DepositActivity, R.color.colorAccent)
        ds.color = color
        ds.isFakeBoldText = true
        ds.isUnderlineText = false
      }
    }

    // initialize a new SpannableStringBuilder instance
    val requestType = if (mode == Mode.WITHDRAW) getString(R.string.withdrawal) else modeString
    val agreeStr = getString(R.string.agree_request_terms, requestType.toLowerCase(Locale.getDefault()))
    val strForSpan = "Terms and Conditions"
    val ssBuilder = SpannableStringBuilder(agreeStr)

    ssBuilder.setSpan(clickableSpan, agreeStr.indexOf(strForSpan),
      agreeStr.indexOf(strForSpan) + strForSpan.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    agreeText.text = ssBuilder
    agreeText.movementMethod = LinkMovementMethod.getInstance()
    agreeText.highlightColor = Color.TRANSPARENT

    amountLabel.text = getString(R.string.request_amount, requestType)
    editAmountButton.setOnClickListener {
      hideDepositAmount()
      showAmountInput()
      updateAmount("0")
    }

    confirmButton.setOnClickListener {
      when (mode) {
        Mode.DEPOSIT -> confirmDeposit()
        Mode.WITHDRAW -> confirmWithdrawal()
      }
    }


    val shortAssetCode = AssetUtils.getShortCode(assetCode)
    when (mode) {
      Mode.DEPOSIT -> {
        limitText.text = getString(R.string.pattern_deposit_limit, shortAssetCode, shortAssetCode)
      }
      Mode.WITHDRAW -> {
        var minWithdrawalText = ""
        if (minAmount != 0.0) {
          val minAmountText = StringFormat.truncateDecimalPlaces(minAmount, maxDecimals)
          minWithdrawalText = getString(R.string.pattern_min_withdrawal, "$shortAssetCode$minAmountText. ")
        }
        val limitStr = minWithdrawalText + getString(R.string.pattern_available, "$shortAssetCode$maxAmountText")
        limitText.text = limitStr
      }
    }

    depositConfirmExplain.visibility = if (mode == Mode.DEPOSIT &&
      assetCode == Constants.ZAR_ASSET_CODE) View.VISIBLE else View.GONE
  }

  private fun onUserFetched(user: DmcUser) {
    dmcUser = user

    when (assetCode) {
      Constants.ZAR_ASSET_CODE -> userBankAccounts = user.banksZAR
      Constants.NGNT_ASSET_CODE -> userBankAccounts = user.banksNGNT
    }

    val fullNameString = "${user.first_name} ${user.last_name}"
    nameText?.text = fullNameString
    phoneText?.text = user.phone
    updateView()
  }

  private fun updateView() {
    when {
      isBank() -> updateForBankDeposit()
      isCrypto() -> updateForCryptoDeposit()
      else -> finishWithToast("Error - $assetCode $modeString not supported")
    }
  }

  private fun updateForBankDeposit() {
    if (userBankAccounts.isEmpty()) {
      showAddAccount()
      hideSelectedAccount()
      hideAmountInput()
    } else {
      hideAddAccount()
      showSelectedAccount()
      showAmountInput()
    }
  }

  private fun showSelectedAccount() {
    if (isBank()) {
      val selectedBankAccount = userBankAccounts[0]
      selectedBankName?.text = selectedBankAccount.bankName
      selectedBankBranch?.text = getString(R.string.pattern_branch_number, selectedBankAccount.branch)
      selectedBankAccountNumber?.text = selectedBankAccount.number
      selectedBankAccountType?.text = selectedBankAccount.type.toLowerCase(Locale.getDefault())
      selectedBankContainer?.visibility = View.VISIBLE
    } else if (isCrypto()) {
      selectedBankName?.text = AssetUtils.getName(assetCode)
      selectedBankBranch?.text = accountDest
      selectedBankContainer?.visibility = View.VISIBLE
    }
  }

  private fun hideSelectedAccount() {
    selectedBankContainer?.visibility = View.GONE
  }

  private fun showAddAccount() {
    addBankContainer?.visibility = View.VISIBLE
  }

  private fun hideAddAccount() {
    addBankContainer?.visibility = View.GONE
  }

  private fun updateForCryptoDeposit() {
    val cryptoName = AssetUtils.getName(assetCode)

    if (accountDest.isEmpty()) {
      showAddAccount()
      // Hide bank picker and account type picker
      bankPicker?.visibility = View.GONE
      accountTypePicker?.visibility = View.GONE

      bankingInfoLabel?.setText(R.string.destination_account)
      accountNumberInput?.hint = getString(R.string.pattern_enter_account, cryptoName)
      addBankButton?.setText(R.string.next)
    } else {
      hideAddAccount()
      showSelectedAccount()
      showAmountInput()
    }
  }

  private fun onAccountInput(input: String) {
    bankToAdd.number = input

    updateAddBankButton()
  }

  private fun showBankPicker() {
    val banks = supportedBanks.values.toTypedArray()
    val branchCodes = supportedBanks.keys.toTypedArray()

    AlertDialog.Builder(this)
      .setTitle(R.string.select_bank)
      .setItems(banks) { _, which ->
        val bankName = banks[which]
        val branchCode = branchCodes[which]
        bankPicker?.setTextColor(Color.BLACK)
        bankPicker?.text = bankName

        bankToAdd.bankName = bankName
        bankToAdd.branch = branchCode

        if (bankToAdd.number.isEmpty()) {
          ViewUtils.showKeyboard(this, accountNumberInput)
        }

        updateAddBankButton()
      }.show()
  }

  private fun showAccountTypePicker() {
    val accountTypes = resources.getStringArray(R.array.bank_account_types)
    AlertDialog.Builder(this)
      .setTitle(R.string.select_account_type)
      .setItems(accountTypes) { _, which ->
        val accountType = accountTypes[which]
        accountTypePicker?.setTextColor(Color.BLACK)
        accountTypePicker?.text = accountType

        bankToAdd.type = accountType

        if (bankToAdd.isValid()) {
          //hide keyboard if user already filled all the fields needed to add new bank
          ViewUtils.hideKeyboard(this)
        }

        updateAddBankButton()
      }.show()
  }

  private fun updateAddBankButton() {
    when {
      AssetUtils.isZar(assetCode, assetIssuer) || AssetUtils.isNgnt(assetCode, assetIssuer) ->
        addBankButton.isEnabled = bankToAdd.isValid()
      AssetUtils.isBtc(assetCode, assetIssuer) || AssetUtils.isEth(assetCode, assetIssuer) ->
        addBankButton.isEnabled = accountNumberInput?.text.toString().trim().length in 25..35
      else -> addBankButton.isEnabled = false
    }
  }

  private fun updateConfirmButton() {
    when {
      isBank() -> confirmButton.isEnabled = termsChecked
      isCrypto() -> confirmButton.isEnabled = termsChecked && anchorAccount.isNotEmpty()
    }
  }

  private fun showAmountInput() {
    numberKeyboard?.visibility = View.VISIBLE
    submitButton?.visibility = View.VISIBLE
    limitText?.visibility = View.VISIBLE
    amountTextView?.visibility = View.VISIBLE
  }

  private fun hideAmountInput() {
    amountTextView?.visibility = View.GONE
    limitText?.visibility = View.GONE
    submitButton?.visibility = View.GONE
    numberKeyboard?.visibility = View.GONE
  }

  private fun addBank() {
    if (!bankToAdd.isValid()) return
    addBankButton?.isEnabled = false
    ViewUtils.hideKeyboard(this)

    userBankAccounts.clear()

    when {
      AssetUtils.isZar(assetCode, assetIssuer) -> {
        userBankAccounts.add(bankToAdd)
        Firebase.getUserBanksZarRef(dmcUser.uid).setValue(userBankAccounts)
          .addOnSuccessListener {
            updateView()
          }.addOnFailureListener {
            bankInputError("Something went wrong please try to add bank Account again")
          }
      }
      AssetUtils.isNgnt(assetCode, assetIssuer) -> {
        // Verify NGNT bank account with cowrie exchange API first
        cowrieApi.ngntForNgn(bankToAdd.branch, bankToAdd.number).enqueue(object : Callback<WithdrawResponse> {
          override fun onResponse(call: Call<WithdrawResponse>, response: Response<WithdrawResponse>) {
            if (response.isSuccessful) {
              userBankAccounts.add(bankToAdd)
              Firebase.getUserBanksNgntRef(dmcUser.uid).setValue(userBankAccounts)
                .addOnSuccessListener {
                  updateView()
                }.addOnFailureListener {
                  bankInputError("Something went wrong please try to add bank Account again")
                }
            } else {
              bankInputError("Looks like not valid Bank Account, check your input and try again")
            }
          }

          override fun onFailure(call: Call<WithdrawResponse>, t: Throwable) {
            bankInputError(t.localizedMessage
              ?: "Error adding bank account, check your input and try again")
          }
        })
      }
    }
  }

  private fun addDestination() {
    accountDest = accountNumberInput?.text.toString()
    if (accountDest.isEmpty()) return
    ViewUtils.hideKeyboard(this)
    updateView()
  }

  private fun bankInputError(message: String) {
    toast(message)
    addBankButton?.isEnabled = true
    userBankAccounts.clear()
  }

  private fun showDepositAmount() {
    depositAmountContainer.visibility = View.VISIBLE
    depositAmountTextView.text = String.format("%s %s", StringFormat.truncateDecimalPlaces(
      amount, maxDecimals), assetCode)
    feeTextView.visibility = View.VISIBLE

    if (mode == Mode.WITHDRAW) {
      val shortCode = AssetUtils.getShortCode(assetCode)
      // 1% DMC withdrawal fee
      val dmcFeeValue = "$shortCode${StringFormat.truncateDecimalPlaces(amount * getDmcFeePercent(), maxDecimals)}"

      // count and show fee(s) for withdrawal
      val assetFeeString = if (isBank() && assetCode == Constants.NGNT_ASSET_CODE) {
        "\n${getString(R.string.pattern_withdrawal_fee_asset, "200.00", assetCode)}"
      } else if (isCrypto()) {
        StellarPort.authenticatedWithdraw(this, assetCode, accountDest, object : StellarPort.WithdrawListener {
          override fun onWithdrawResponse(response: io.demars.stellarwallet.api.stellarport.model.WithdrawResponse) {
            //Update Fee
            val fixedFeeFormatted = StringFormat.truncateDecimalPlaces(response.feeFixed, maxDecimals)
            val assetFeeString = "\nFixed: $fixedFeeFormatted $assetCode\nPercent: ${response.feePercent}%"
            updateFeeView(dmcFeeValue, assetFeeString)

            // Update confirm button if have Anchor stellar account & memo
            anchorAccount = response.accountId
            anchorMemo = response.memo
            updateConfirmButton()
          }
        })
        "\nLoading $assetCode fee..."
      } else {
        ""
      }

      updateFeeView(dmcFeeValue, assetFeeString)
    } else {
      feeTextView.visibility = View.GONE
    }
  }

  private fun updateFeeView(dmcFeeValue: String, assetFeeString: String) {
    val percents = (getDmcFeePercent() * 100).toString()
    val feesString = getString(R.string.pattern_withdrawal_fee_dmc, dmcFeeValue, percents, assetFeeString)
    feeTextView.text = feesString
  }

  private fun hideDepositAmount() {
    depositAmountContainer.visibility = View.GONE
  }

  //region Dialer
  override fun onDial(number: Int) {
    if (amountText == "0" && number == 0) return
    updateAmount(if (amountText == "0") "$number" else "$amountText$number")
  }

  override fun onDelete() {
    if (amountText == "0") return

    var newAmountText: String
    if (amountText.length <= 1) {
      newAmountText = "0"
    } else {
      newAmountText = amountText.substring(0, amountText.length - 1)
      if (newAmountText[newAmountText.length - 1] == '.') {
        newAmountText = newAmountText.substring(0, newAmountText.length - 1)
      }
    }
    updateAmount(newAmountText)
  }

  override fun onDeleteAll() {
    if (amountText == "0") return
    updateAmount("0")
  }

  override fun onDot() {
    if (!StringFormat.hasDecimalPoint(amountText)) {
      amountText = if (amountText == "0") "0." else "$amountText."
      showAmount(amountText)
    }
  }

  private fun updateAmount(newAmountText: String) {
    val newAmount = if (newAmountText.isEmpty()) 0.0 else newAmountText.toDouble()
    val maxDecimals = AssetUtils.getMaxDecimals(assetCode)
    if (newAmount >= 0.0 && StringFormat.getNumDecimals(newAmountText) <= maxDecimals) {
      amountText = if (newAmount > maxAmount) maxAmountText else newAmountText
      amount = if (newAmount > maxAmount) maxAmount else newAmount
      showAmount(amountText)
    }
  }

  private fun showAmount(amountToUse: String) {
    amountTextView?.text = amountToUse
    submitButton.isEnabled = this.amount > 0 && this.amount >= minAmount
  }

  private fun showProgressBar() {
    progressBar?.visibility = View.VISIBLE
  }

  private fun hideProgressBar() {
    progressBar?.visibility = View.GONE
  }
  //endregion

  private fun confirmDeposit() {
    confirmButton.isEnabled = false

    if (NetworkUtils(applicationContext).isNetworkAvailable()) {
      showProgressBar()

      val amount = StringFormat.truncateDecimalPlaces(amount, maxDecimals)
      when (assetCode) {
        Constants.NGNT_ASSET_CODE -> {
          // Use Cowrie exchange api to request deposit NGNT
          cowrieApi.ngnForNgnt(dmcUser.stellar_address).enqueue(object : Callback<DepositResponse> {
            override fun onResponse(call: Call<DepositResponse>, response: Response<DepositResponse>) {
              response.body()?.let {
                val anchorBank = DmcUser.BankAccount(it.accountName, "", "", it.accountNumber, it.bankName)
                val deposit = BankDeposit(assetCode, amount, it.depositRef, anchorBank, userBankAccounts[0])
                MailHelper.notifyAboutNewDeposit(dmcUser, deposit)
                showDepositInfoDialog(deposit)
                hideProgressBar()
              }
            }

            override fun onFailure(call: Call<DepositResponse>, t: Throwable) {
              Timber.e(t)
              toast(t.localizedMessage ?: "Unknown error while deposit $assetCode")
              hideProgressBar()
            }
          })
        }
        Constants.ZAR_ASSET_CODE -> {
          // Notify user with our ZAR banking info
          val anchorBank = DmcUser.BankAccount("DMC Rand (Pty) Ltd", "250655",
            "Current/Cheque Account", "62756496496", "FNB")
          val depositRef = "${dmcUser.email_address}*demars.io"
          val deposit = BankDeposit(assetCode, amount, depositRef, anchorBank, userBankAccounts[0])
          MailHelper.notifyAboutNewDeposit(dmcUser, deposit)
          showDepositInfoDialog(deposit)
          hideProgressBar()
        }
        Constants.BTC_ASSET_CODE,
        Constants.ETH_ASSET_CODE -> {
          StellarPort.authenticatedDeposit(this, assetCode, object : StellarPort.DepositListener {
            override fun onDepositResponse(response: io.demars.stellarwallet.api.stellarport.model.DepositResponse) {
              val deposit = CryptoDeposit(assetCode, amount, response.how, response.extraInfo.message)
              MailHelper.notifyAboutNewDeposit(dmcUser, deposit)
              showDepositInfoDialog(deposit)
            }

          })
        }
      }
    } else {
      confirmButton.isEnabled = true
      NetworkUtils(applicationContext).displayNoNetwork()
    }
  }

  private fun showDepositInfoDialog(deposit: Deposit) {
    AlertDialog.Builder(this)
      .setTitle(deposit.toReadableTitle())
      .setMessage(deposit.toReadableMessage())
      .setCancelable(false)
      .setPositiveButton(R.string.copy_and_finish) { _, _ ->
        copyAndFinish(deposit)
      }
      .show()

  }

  private fun copyAndFinish(deposit: Deposit) {
    when (deposit) {
      is BankDeposit -> ViewUtils.copyToClipBoard(this, deposit.anchorBank.number,
        R.string.bank_account_number_copied)
      is CryptoDeposit -> ViewUtils.copyToClipBoard(this, deposit.anchorAccount,
        getString(R.string.pattern_account_copied, AssetUtils.getName(deposit.assetCode)))
    }

    finish()
  }

  private fun confirmWithdrawal() {
    confirmButton.isEnabled = false

    val feePercent = getDmcFeePercent()
    val fee = StringFormat.truncateDecimalPlaces(amount * feePercent, maxDecimals)
    val amount = StringFormat.truncateDecimalPlaces(amount * (1.0 - feePercent), maxDecimals)
    val secretSeed = AccountUtils.getSecretSeed(applicationContext)

    if (NetworkUtils(applicationContext).isNetworkAvailable()) {
      showProgressBar()

      when (assetCode) {
        Constants.NGNT_ASSET_CODE -> {
          withdrawNgnt(secretSeed, amount, fee)
        }
        Constants.ZAR_ASSET_CODE -> {
          withdrawZar(secretSeed, amount, fee)
        }
        Constants.BTC_ASSET_CODE,
        Constants.ETH_ASSET_CODE -> {
          withdrawCrypto(secretSeed, amount, fee)
        }
      }
    } else {
      confirmButton.isEnabled = true
      NetworkUtils(applicationContext).displayNoNetwork()
    }
  }

  private fun withdrawZar(secretSeed: CharArray, amount: String, fee: String) {
    Horizon.getWithdrawTask(object : SuccessErrorCallback {
      override fun onSuccess() {
        val withdrawal = BankWithdrawal(assetCode, amount, fee, userBankAccounts[0])

        MailHelper.notifyAboutNewWithdrawal(dmcUser, withdrawal)

        finishWithToast(getString(R.string.request_sent_message,
          getString(R.string.withdrawal)))
      }

      override fun onError(error: HorizonException) {
        finishWithToast(error.localizedMessage ?: "Unknown error while withdraw $assetCode")
        hideProgressBar()
      }
    }, getAsset(), secretSeed, Constants.ZAR_ASSET_ISSUER, "", amount, fee).execute()
  }

  private fun withdrawNgnt(secretSeed: CharArray, amount: String, fee: String) {
    val bankAccount = userBankAccounts[0]
    cowrieApi.ngntForNgn(bankAccount.branch, bankAccount.number).enqueue(object : Callback<WithdrawResponse> {
      override fun onResponse(call: Call<WithdrawResponse>, response: Response<WithdrawResponse>) {
        if (!response.isSuccessful) {
          toast("Error requesting $assetCode withdrawal")
          hideProgressBar()
          return
        }

        response.body()?.let { info ->
          Horizon.getWithdrawTask(object : SuccessErrorCallback {
            override fun onSuccess() {
              val withdrawal = BankWithdrawal(assetCode, amount, fee, userBankAccounts[0])

              MailHelper.notifyAboutNewWithdrawal(dmcUser, withdrawal)

              finishWithToast(getString(R.string.request_sent_message,
                getString(R.string.withdrawal)))
            }

            override fun onError(error: HorizonException) {
              finishWithToast(error.localizedMessage ?: "Unknown error while withdraw $assetCode")
            }
          }, getAsset(), secretSeed, info.address, info.meta, amount, fee).execute()
        }
      }

      override fun onFailure(call: Call<WithdrawResponse>, t: Throwable) {
        Timber.e(t)
        toast(t.localizedMessage ?: "Unknown error while requesting withdraw $assetCode")
        hideProgressBar()
      }
    })
  }

  private fun withdrawCrypto(secretSeed: CharArray, amount: String, fee: String) {
    Horizon.getWithdrawTask(object : SuccessErrorCallback {
      override fun onSuccess() {
        val withdrawal = CryptoWithdrawal(assetCode, amount, fee)

        MailHelper.notifyAboutNewWithdrawal(dmcUser, withdrawal)

        finishWithToast(getString(R.string.request_sent_message,
          getString(R.string.withdrawal)))
      }

      override fun onError(error: HorizonException) {
        finishWithToast(error.localizedMessage ?: "Unknown error while withdraw $assetCode")
      }
    }, getAsset(), secretSeed, anchorAccount, anchorMemo, amount, fee).execute()
  }

  override fun onDestroy() {
    super.onDestroy()
    Firebase.removeUserListener(userListener)
    Firebase.removeAssetListener(assetCode, assetListener)
  }

  private fun getAsset() = AssetUtils.getAsset(assetCode, assetIssuer)

  private fun isBank(): Boolean = AssetUtils.isZar(assetCode, assetIssuer) ||
    AssetUtils.isNgnt(assetCode, assetIssuer)

  private fun isCrypto(): Boolean = AssetUtils.isBtc(assetCode, assetIssuer) ||
    AssetUtils.isEth(assetCode, assetIssuer)

  private fun getDmcFeePercent(): Double = if (isCrypto()) 0.005 else 0.01
}