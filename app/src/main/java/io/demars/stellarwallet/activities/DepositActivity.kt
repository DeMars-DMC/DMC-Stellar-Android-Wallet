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
import io.demars.stellarwallet.api.sep.Sep6
import io.demars.stellarwallet.api.sep.Sep6DepositResponse
import io.demars.stellarwallet.api.sep.Sep6WithdrawResponse
import io.demars.stellarwallet.models.local.*
import io.demars.stellarwallet.utils.*
import io.demars.stellarwallet.views.LinkifiedDialog
import io.demars.stellarwallet.views.SearchableListDialog
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

  enum class State {
    ADD_BANK, ADD_AMOUNT, CONFIRM
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
  private var dest = ""
  private var destExtra = ""
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
        updateView(State.ADD_BANK)
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
        AssetUtils.isNgnt(assetCode, assetIssuer)) {
        Firebase.getAssetFresh(assetCode, assetListener)
      }
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

    assetLogo.setImageResource(AssetUtils.getLogo(assetCode))

    numberKeyboard.mDialerListener = this

    submitButton.setOnClickListener {
      updateView(State.CONFIRM)
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

    accountExtraInput?.inputType = if (isBank()) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
    accountExtraInput?.addTextChangedListener(object : AfterTextChanged() {
      override fun afterTextChanged(editable: Editable) {
        onExtraInput(editable.trim().toString())
      }
    })

    accountTypePicker?.setOnClickListener {
      showAccountTypePicker()
    }

    addDestanationButton.setOnClickListener {
      addDestination()
    }

    agreeCheckBox.setOnCheckedChangeListener { _, isChecked ->
      termsChecked = isChecked
      ViewUtils.setButtonEnabled(confirmButton, isChecked)
    }

    // String "Deposit" or "Withdrawal" depends on mode.
    val requestType = if (mode == Mode.WITHDRAW) getString(R.string.withdrawal) else modeString

    // Agree Terms & Conditions clickable text
    val clickableSpan = object : ClickableSpan() {
      override fun onClick(widget: View) {
        val termsTitle = getString(R.string.dialog_deposit_terms_title, AssetUtils.getName(assetCode), assetCode)
        val termsMessage = when {
          isCrypto() -> getString(R.string.dialog_deposit_terms_message_stellarport)
          assetCode == Constants.NGNT_ASSET_CODE -> getString(R.string.dialog_deposit_terms_message_ngnt)
          else -> getString(R.string.dialog_deposit_terms_message_zar) // ZAR
        }

        LinkifiedDialog(this@DepositActivity, termsTitle,
          termsMessage, getString(R.string.pattern_back_to, requestType)).show()
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
    val agreeStr = getString(R.string.agree_request_terms, requestType.toLowerCase(Locale.getDefault()))
    val strForSpan = "Terms and Conditions"
    val ssBuilder = SpannableStringBuilder(agreeStr)

    ssBuilder.setSpan(clickableSpan, agreeStr.indexOf(strForSpan),
      agreeStr.indexOf(strForSpan) + strForSpan.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    agreeText.text = ssBuilder
    agreeText.movementMethod = LinkMovementMethod.getInstance()
    agreeText.highlightColor = Color.TRANSPARENT

    editAmountButton.setOnClickListener {
      updateView(State.ADD_AMOUNT)
    }

    confirmButtonLabel.setText(R.string.confirm_and_submit)
    confirmButton.setOnClickListener {
      when (mode) {
        Mode.DEPOSIT -> confirmDeposit()
        Mode.WITHDRAW -> confirmWithdrawal()
      }
    }


    val shortAssetCode = AssetUtils.getShortCode(assetCode)
    when (mode) {
      Mode.DEPOSIT -> {
        amountTip.text = when (assetCode) {
          Constants.ZAR_ASSET_CODE -> getString(R.string.pattern_deposit_limit, shortAssetCode, shortAssetCode)
          else -> getString(R.string.enter_deposit_sum)

        }
      }
      Mode.WITHDRAW -> {
        var minWithdrawalText = ""
        if (minAmount != 0.0) {
          val minAmountText = StringFormat.truncateDecimalPlaces(minAmount, maxDecimals)
          minWithdrawalText = getString(R.string.pattern_min_withdrawal, "$shortAssetCode$minAmountText. ")
        }
        val limitStr = minWithdrawalText + getString(R.string.pattern_available, "$shortAssetCode$maxAmountText")
        amountTip.text = limitStr
      }
    }

    depositConfirmExplain.visibility = if (mode == Mode.DEPOSIT &&
      assetCode == Constants.ZAR_ASSET_CODE) View.VISIBLE else View.GONE

    bankAccountExplain.visibility = if (assetCode == Constants.ZAR_ASSET_CODE
      || assetCode == Constants.NGNT_ASSET_CODE) View.VISIBLE else View.GONE
  }

  private fun onUserFetched(user: DmcUser) {
    dmcUser = user

    when (assetCode) {
      Constants.ZAR_ASSET_CODE -> userBankAccounts = user.banksZAR
      Constants.NGNT_ASSET_CODE -> userBankAccounts = user.banksNGNT
    }

    updateView(State.ADD_BANK)
  }

  private fun updateView(state: State) {
    ViewUtils.hideKeyboard(this)

    when (state) {
      State.ADD_BANK -> updateViewForAddBankState()
      State.ADD_AMOUNT -> updateViewForAddAmountState()
      State.CONFIRM -> updateViewForConfirmState()
    }
  }

  private fun updateViewForAddBankState() {
    var needAddBankAccount = true
    when {
      isIban() -> {
        // Hide bank and account type picker
        bankPicker?.visibility = View.GONE
        accountTypePicker?.visibility = View.GONE

        bankPicker?.setText(R.string.select_bank)
        accountTypePicker?.setText(R.string.select_account_type)
        // For EURT we need an IBAN + SWIFT or BIC
        accountNumberInput?.setHint(R.string.enter_iban)
        accountExtraInput?.setHint(R.string.enter_swift_or_bic)
        accountExtraInput?.visibility = View.VISIBLE
      }
      isBank() -> {
        if (userBankAccounts.isEmpty()) {
          bankPicker?.visibility = View.VISIBLE
          accountTypePicker?.visibility = View.VISIBLE
          accountExtraInput?.visibility = View.GONE

          bankPicker?.setText(R.string.select_bank)
          accountTypePicker?.setText(R.string.select_account_type)
          accountNumberInput?.setHint(R.string.enter_account_number)

        } else {
          needAddBankAccount = false
        }
      }
      isCrypto() -> {
        // Hide bank picker and account type picker
        bankPicker?.visibility = View.GONE
        accountTypePicker?.visibility = View.GONE
        accountExtraInput?.visibility = View.GONE

        bankAccountTitle?.setText(R.string.destination_account)
        accountNumberInput?.hint = getString(R.string.pattern_enter_your_address,
          AssetUtils.getName(assetCode))
      }
    }

    if (needAddBankAccount) {
      addBankContainer?.visibility = View.VISIBLE
      depositAmountContainer.visibility = View.GONE
      selectedBankContainer?.visibility = View.GONE
      hideAmountInput(false)
      updateAddBankButton()
    } else {
      // We use saved bank account so proceed to the next step
      addBankContainer?.visibility = View.GONE
      updateView(State.ADD_AMOUNT)
    }
  }

  private fun updateViewForAddAmountState() {
    when {
      isIban() || isBank() -> {
        val selectedBankAccount = userBankAccounts[0]
        val patternForExtra = if (isIban())
          R.string.pattern_swift_code else R.string.pattern_branch_number

        selectedBankName?.text = selectedBankAccount.bankName
        selectedBankBranch?.text = getString(
          patternForExtra, selectedBankAccount.branch)
        selectedBankAccountNumber?.text = selectedBankAccount.number
        selectedBankAccountType?.text = selectedBankAccount.type.toLowerCase(Locale.getDefault())
      }
      isCrypto() -> {
        selectedBankName?.text = getString(R.string.pattern_address, AssetUtils.getName(assetCode))
        selectedBankBranch?.text = dest
      }
    }

    addBankContainer?.visibility = View.GONE
    editAmountButton.visibility = View.GONE
    depositAmountContainer.visibility = View.GONE
    selectedBankContainer?.visibility = View.VISIBLE
    showAmountInput()
    updateAmount()
  }

  private fun updateViewForConfirmState() {

    depositAmountTextView.text = String.format("%s %s",
      StringFormat.truncateDecimalPlaces(amount, maxDecimals), assetCode)

    feeTextView.visibility = View.VISIBLE

    if (mode == Mode.DEPOSIT && assetCode == Constants.ZAR_ASSET_CODE) {
      feeTextView.visibility = View.GONE
    } else {
      val shortCode = AssetUtils.getShortCode(assetCode)
      // 1% DMC withdrawal fee
      val dmcFeeValue = "$shortCode${StringFormat.truncateDecimalPlaces(amount * getDmcFeePercent(), maxDecimals)}"

      // count and show fee(s) for withdrawal
      val assetFeeString =
        "\n${getString(R.string.pattern_withdrawal_fee_asset, "200.00", assetCode)}"

      val percents = (getDmcFeePercent() * 100).toString()
      val feesString = if (assetCode != Constants.ZAR_ASSET_CODE) getString(R.string.fee_message_crypto)
      else getString(R.string.pattern_withdrawal_fee_dmc, dmcFeeValue, percents, assetFeeString)
      feeTextView.text = feesString
    }

    ViewUtils.setButtonEnabled(confirmButton, termsChecked)

    addBankContainer.visibility = View.GONE
    editAmountButton.visibility = View.VISIBLE
    depositAmountContainer.visibility = View.VISIBLE
    hideAmountInput(true)
  }

  private fun onAccountInput(input: String) {
    bankToAdd.number = input
    dest = input

    updateAddBankButton()
  }

  private fun onExtraInput(input: String) {
    bankToAdd.branch = input
    destExtra = input

    updateAddBankButton()
  }

  private fun showBankPicker() {
    val banks = supportedBanks.values.toTypedArray()
    val branchCodes = supportedBanks.keys.toTypedArray()

    val dialog = SearchableListDialog(this)
    dialog.showForList(banks.toList(), getString(R.string.select_bank), object : SearchableListDialog.OnItemClick {
      override fun itemClicked(item: String) {
        dialog.dismiss()

        val branchCode = branchCodes[banks.indexOf(item)]
        bankPicker?.setTextColor(Color.WHITE)
        bankPicker?.text = item

        bankToAdd.bankName = item
        bankToAdd.branch = branchCode

        updateAddBankButton()
      }
    })
  }

  private fun showAccountTypePicker() {
    val accountTypes = resources.getStringArray(R.array.bank_account_types)
    AlertDialog.Builder(this)
      .setTitle(R.string.select_account_type)
      .setItems(accountTypes) { _, which ->
        val accountType = accountTypes[which]
        accountTypePicker?.setTextColor(Color.WHITE)
        accountTypePicker?.text = accountType

        bankToAdd.type = accountType

        if (bankToAdd.number.isEmpty()) {
          ViewUtils.showKeyboard(this@DepositActivity, accountNumberInput)
        }

        updateAddBankButton()
      }.show()
  }

  private fun updateAddBankButton() {
    val isEnabled = when {
      AssetUtils.isZar(assetCode, assetIssuer) || AssetUtils.isNgnt(assetCode, assetIssuer) ->
        bankToAdd.isValid()
      AssetUtils.isBtc(assetCode, assetIssuer) || AssetUtils.isEth(assetCode, assetIssuer) ->
        dest.length in 25..35
      AssetUtils.isEurt(assetCode, assetIssuer) ->
        dest.length in 10..34 && destExtra.length in 8..11
      else -> false
    }

    ViewUtils.setButtonEnabled(addDestanationButton, isEnabled)

    // Update tip-label
    if (isEnabled) {
      addBankLabel?.setText(R.string.proceed)
    } else {
      addBankLabel?.text = when {
        AssetUtils.isEurt(assetCode, assetIssuer) ->
          getString(R.string.to_proceed_enter_valid, getString(R.string.iban_and_swift))
        isBank() -> getString(R.string.to_proceed_enter_valid, getString(R.string.bank_account))
        isCrypto() -> getString(R.string.to_proceed_enter_valid,
          "${AssetUtils.getName(assetCode)} ${getString(R.string.account)}")
        else -> ""
      }
    }
  }

  private fun updateConfirmButton() {
  }

  private fun showAmountInput() {
    amountTitle?.visibility = View.VISIBLE
    amountDivider?.visibility = View.VISIBLE
    keyboardDivider?.visibility = View.VISIBLE
    numberKeyboard?.visibility = View.VISIBLE
    submitButton?.visibility = View.VISIBLE
    amountTip?.visibility = View.VISIBLE
    amountTextView?.visibility = View.VISIBLE
  }

  private fun hideAmountInput(keepTitle: Boolean) {
    if (!keepTitle) {
      amountTitle?.visibility = View.GONE
      amountDivider?.visibility = View.GONE
      editAmountButton?.visibility = View.GONE
    }

    keyboardDivider?.visibility = View.GONE
    amountTextView?.visibility = View.GONE
    amountTip?.visibility = View.GONE
    submitButton?.visibility = View.GONE
    numberKeyboard?.visibility = View.GONE
  }

  private fun addDestination() {
    var isBankSet = true
    when {
      isBank() -> {
        // save user bank account for ZAR and NGNT
        saveBankToFirebase()
        isBankSet = false
      }
      isIban() -> {
        userBankAccounts.add(DmcUser.BankAccount("Account number", destExtra, "default account", dest, "IBAN"))
      }
      isCrypto() -> {
        dest = accountNumberInput?.text.toString()
      }
    }

    if (isBankSet) {
      // Update for next step since we don't need to save it
      updateView(State.ADD_AMOUNT)
    }
  }

  private fun saveBankToFirebase() {
    if (!bankToAdd.isValid()) return
    addDestanationButton?.isEnabled = false
    ViewUtils.hideKeyboard(this)

    userBankAccounts.clear()

    fun onSaveError(message: String) {
      toast(message)
      addDestanationButton?.isEnabled = true
      userBankAccounts.clear()
    }

    when {
      AssetUtils.isZar(assetCode, assetIssuer) -> {
        userBankAccounts.add(bankToAdd)
        Firebase.getUserBanksZarRef(dmcUser.uid).setValue(userBankAccounts)
          .addOnSuccessListener {
            updateView(State.ADD_AMOUNT)
          }.addOnFailureListener {
            onSaveError("Something went wrong please try to add bank Account again")
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
                  updateView(State.ADD_AMOUNT)
                }.addOnFailureListener {
                  onSaveError("Something went wrong please try to add bank Account again")
                }
            } else {
              onSaveError("Looks like not valid Bank Account, check your input and try again")
            }
          }

          override fun onFailure(call: Call<WithdrawResponse>, t: Throwable) {
            onSaveError(t.localizedMessage
              ?: "Error adding bank account, check your input and try again")
          }
        })
      }
    }
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
      updateAmount(amountText)
    }
  }

  private fun updateAmount(newAmountText: String = "0") {
    val newAmount = if (newAmountText.isEmpty()) 0.0 else newAmountText.toDouble()
    val maxDecimals = AssetUtils.getMaxDecimals(assetCode)
    if (newAmount >= 0.0 && StringFormat.getNumDecimals(newAmountText) <= maxDecimals) {
      amountText = if (newAmount > maxAmount) maxAmountText else newAmountText
      amount = if (newAmount > maxAmount) maxAmount else newAmount
      amountTextView?.text = amountText
      ViewUtils.setButtonEnabled(submitButton, this.amount > 0 && this.amount >= minAmount)
    }
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

      val account = dmcUser.stellar_address
      val email = dmcUser.email_address

      val amount = StringFormat.truncateDecimalPlaces(amount, maxDecimals)
      when (assetCode) {
        Constants.NGNT_ASSET_CODE -> {
          // Use Cowrie exchange api to request deposit NGNT
          cowrieApi.ngnForNgnt(account).enqueue(object : Callback<DepositResponse> {
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
          val transferUrl = Constants.DMC_TRANSFER_URL
          val depositPath = Constants.DMC_DEPOSIT_PATH

          Sep6.deposit(this, transferUrl, depositPath, null, assetCode, account, email,
            object : Sep6.DepositListener {
              override fun onDepositResponse(response: Sep6DepositResponse) {
                val extraInfo = response.extraInfo
                val anchorBank = DmcUser.BankAccount("DMC Rand (Pty) Ltd", "250655",
                  extraInfo.bankAccountName, extraInfo.bankAccountNumber, extraInfo.bankName)
                val deposit = BankDeposit(assetCode, amount, response.extraInfo.depositRef, anchorBank, userBankAccounts[0])
                MailHelper.notifyAboutNewDeposit(dmcUser, deposit)
                showDepositInfoDialog(deposit)
                hideProgressBar()
              }
            })
        }
        Constants.EURT_ASSET_CODE -> {
          val transferUrl = Constants.TEMPO_TRANSFER_URL
          val depositPath = Constants.TEMPO_DEPOSIT_PATH

          Sep6.deposit(this, transferUrl, depositPath, null,
            assetCode, account, email, object : Sep6.DepositListener {
            override fun onDepositResponse(response: Sep6DepositResponse) {
              val deposit = CryptoDeposit(assetCode, amount, response.how, response.extraInfo.message)
              MailHelper.notifyAboutNewDeposit(dmcUser, deposit)
              showDepositInfoDialog(deposit)
            }
          })
        }
        Constants.BTC_ASSET_CODE,
        Constants.ETH_ASSET_CODE -> {
          val authUrl = Constants.STELLARPORT_AUTH_URL
          val transferUrl = Constants.STELLARPORT_TRANSFER_URL
          val depositPath = Constants.STELLARPORT_DEPOSIT_PATH

          Sep6.authenticatedDeposit(this, authUrl, transferUrl, depositPath,
            assetCode, email, object : Sep6.DepositListener {
            override fun onDepositResponse(response: Sep6DepositResponse) {
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

  private fun confirmWithdrawal() {
    confirmButton.isEnabled = false

    val email = dmcUser.email_address

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
        Constants.EURT_ASSET_CODE -> {
          withdrawEurt(secretSeed, amount, fee)
        }
        Constants.BTC_ASSET_CODE,
        Constants.ETH_ASSET_CODE -> {
          val authUrl = Constants.STELLARPORT_AUTH_URL
          val transferUrl = Constants.STELLARPORT_TRANSFER_URL
          val withdrawPath = "/v2/GBVOL67TMUQBGL4TZYNMY3ZQ5WGQYFPFD5VJRWXR72VA33VFNL225PL5/withdraw"

          Sep6.authenticatedWithdraw(this, authUrl, transferUrl,
            withdrawPath, assetCode, Sep6.TYPE_CRYPTO, dest, "", email, object : Sep6.WithdrawListener {
            override fun onWithdrawResponse(response: Sep6WithdrawResponse) {
              withdrawCrypto(secretSeed, amount, fee, response.accountId, response.memo)
            }
          })
        }
      }
    } else {
      confirmButton.isEnabled = true
      NetworkUtils(applicationContext).displayNoNetwork()
    }
  }

  private fun withdrawEurt(secretSeed: CharArray, amount: String, fee: String) {
    val transferUrl = Constants.TEMPO_TRANSFER_URL
    val withdrawPath = "/t1/withdraw"
    val account = dmcUser.stellar_address
    val email = dmcUser.email_address

    Sep6.withdraw(this, transferUrl, withdrawPath, null, assetCode, Sep6.TYPE_BANK, account,
      dest, destExtra, email, object : Sep6.WithdrawListener {
      override fun onWithdrawResponse(response: Sep6WithdrawResponse) {
        toast("olalala ${response.accountId}")
      }
    })

  }

  private fun withdrawZar(secretSeed: CharArray, amount: String, fee: String) {
    val transferUrl = Constants.DMC_TRANSFER_URL
    val withdrawPath = Constants.DMC_WITHDRAW_PATH
    val account = dmcUser.stellar_address
    val email = dmcUser.email_address
    val dest = userBankAccounts[0].number
    val destExtra = userBankAccounts[0].branch

    Sep6.withdraw(this, transferUrl, withdrawPath, null, assetCode, Sep6.TYPE_BANK,
      account, dest, destExtra, email, object : Sep6.WithdrawListener {
      override fun onWithdrawResponse(response: Sep6WithdrawResponse) {
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
    })
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

  private fun withdrawCrypto(secretSeed: CharArray, amount: String, fee: String,
                             anchorAddress: String, anchorMemo: String) {
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
    }, getAsset(), secretSeed, anchorAddress, anchorMemo, amount, fee).execute()
  }

  private fun showDepositInfoDialog(deposit: Deposit) {
    AlertDialog.Builder(this)
      .setTitle(deposit.toReadableTitle())
      .setMessage(deposit.toReadableMessage())
      .setCancelable(false)
      .setPositiveButton(R.string.copy_and_finish) { _, _ ->
        copyAndFinish(deposit)
      }.show()

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

  override fun onDestroy() {
    super.onDestroy()
    Firebase.removeUserListener(userListener)
    Firebase.removeAssetListener(assetCode, assetListener)
  }

  private fun getAsset() = AssetUtils.getAsset(assetCode, assetIssuer)

  /**
   * @return true if we need IBAN+SWIFT(BIC) account to complete withdrawal
   * eg. EURT
   */
  private fun isIban(): Boolean = AssetUtils.isEurt(assetCode, assetIssuer)

  /**
   * @return true if we need Bank account to complete withdrawal
   * eg. ZAR, NGNT
   */
  private fun isBank(): Boolean = AssetUtils.isZar(assetCode, assetIssuer) ||
    AssetUtils.isNgnt(assetCode, assetIssuer)

  /**
   * @return true if we need Crypto account to complete withdrawal
   * eg. BTC, ETH
   */
  private fun isCrypto(): Boolean = AssetUtils.isBtc(assetCode, assetIssuer) ||
    AssetUtils.isEth(assetCode, assetIssuer)

  private fun getDmcFeePercent(): Double = if (assetCode == Constants.ZAR_ASSET_CODE) 0.01 else 0.005
}