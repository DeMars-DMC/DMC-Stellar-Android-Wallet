package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.style.ClickableSpan
import android.view.View
import androidx.appcompat.app.AlertDialog
import io.demars.stellarwallet.R
import io.demars.stellarwallet.firebase.DmcUser
import io.demars.stellarwallet.firebase.Firebase
import io.demars.stellarwallet.interfaces.AfterTextChanged
import io.demars.stellarwallet.models.BankAccount
import io.demars.stellarwallet.views.pin.PinLockView
import kotlinx.android.synthetic.main.activity_deposit.*
import kotlinx.android.synthetic.main.activity_deposit.amountTextView
import kotlinx.android.synthetic.main.activity_deposit.numberKeyboard
import kotlinx.android.synthetic.main.activity_deposit.toolbar
import androidx.core.content.ContextCompat
import android.text.TextPaint
import android.text.Spanned
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import io.demars.stellarwallet.firebase.DmcAsset
import io.demars.stellarwallet.helpers.Constants
import io.demars.stellarwallet.helpers.MailHelper
import io.demars.stellarwallet.interfaces.SuccessErrorCallback
import io.demars.stellarwallet.models.Deposit
import io.demars.stellarwallet.models.stellar.HorizonException
import io.demars.stellarwallet.models.Withdrawal
import io.demars.stellarwallet.models.cowrie.DepositResponse
import io.demars.stellarwallet.models.cowrie.WithdrawalResponse
import io.demars.stellarwallet.remote.CowrieRetrofit
import io.demars.stellarwallet.remote.Horizon
import io.demars.stellarwallet.utils.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class DepositActivity : BaseActivity(), PinLockView.DialerListener {
  enum class Mode {
    DEPOSIT, WITHDRAW
  }

  companion object {
    private const val ARG_MODE = "ARG_MODE"
    private const val ARG_ASSET_CODE = "ARG_ASSET_CODE"
    private const val MAX_DECIMALS = 2
    fun newInstance(context: Context, mode: Mode, assetCode: String): Intent {
      val intent = Intent(context, DepositActivity::class.java)
      intent.putExtra(ARG_MODE, mode)
      intent.putExtra(ARG_ASSET_CODE, assetCode)
      return intent
    }
  }

  private lateinit var dmcUser: DmcUser
  private var mode = Mode.DEPOSIT
  private var modeString = ""
  private var assetCode = ""
  private var userBankAccounts = ArrayList<BankAccount>()
  private var supportedBanks = HashMap<String, String>()
  private var bankToAdd = BankAccount()
  private var amount = 0.0
  private var maxAmount = 0.0
  private var amountText = "0"
  private var maxAmountText = "0.0"
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
    setupUI()

    Firebase.getCurrentUser()?.let { _ ->
      Firebase.getUserFresh(userListener)
      Firebase.getAssetFresh(assetCode, assetListener)
    } ?: finishWithToast("Can't find user. Please try again")
  }

  private fun checkIntent() {
    intent?.extras?.let {
      mode = it.get(ARG_MODE) as Mode
      modeString = getString(if (mode == Mode.DEPOSIT)
        R.string.deposit else R.string.withdraw)

      assetCode = it.getString(ARG_ASSET_CODE, "")
      when (mode) {
        Mode.DEPOSIT -> {
          maxAmount = 5000.0
          maxAmountText = "5000.00"
        }
        Mode.WITHDRAW -> {
          val available = AccountUtils.getAvailableBalance(assetCode)
          maxAmount = available.toDouble()
          maxAmountText = StringFormat.truncateDecimalPlaces(available, MAX_DECIMALS)
        }
      }
    }
  }

  private fun setupUI() {
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.title = "$modeString $assetCode"

    numberKeyboard.mDialerListener = this

    submitButton.setOnClickListener {
      hideAmountInput()
      showDepositAmount()
    }

    bankPicker?.setOnClickListener {
      val banks = supportedBanks.values.toTypedArray()
      val branchCodes = supportedBanks.keys.toTypedArray()

      AlertDialog.Builder(this)
        .setTitle(R.string.select_bank)
        .setItems(banks) { _, which ->
          val bankName = banks[which]
          val branchCode = branchCodes[which]
          bankPicker?.setTextColor(Color.BLACK)
          bankPicker?.text = bankName

          bankToAdd.name = bankName
          bankToAdd.branch = branchCode

          if (bankToAdd.number.isEmpty()) {
            ViewUtils.showKeyboard(this, accountNumberInput)
          }

          checkAddBankButton()
        }.show()
    }

    accountNumberInput.addTextChangedListener(object : AfterTextChanged() {
      override fun afterTextChanged(editable: Editable) {
        bankToAdd.number = editable.trim().toString()
        checkAddBankButton()
      }
    })

    accountTypePicker?.setOnClickListener {
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

          checkAddBankButton()
        }.show()
    }

    addBankButton.setOnClickListener {
      addBank()
    }

    agreeCheckBox.setOnCheckedChangeListener { _, isChecked ->
      confirmButton.isEnabled = isChecked
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
    val agreeStr = getString(R.string.agree_request_terms, requestType.toLowerCase())
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

    when (mode) {
      Mode.DEPOSIT -> {
        val shortAssetCode = AssetUtils.getShortCode(assetCode)
        limitText.text = getString(R.string.pattern_deposit_limit, shortAssetCode, shortAssetCode)
      }
      Mode.WITHDRAW -> limitText.text = getString(R.string.pattern_available, maxAmountText)
    }
  }

  private fun onUserFetched(user: DmcUser) {
    dmcUser = user

    when (assetCode) {
      Constants.ZAR_ASSET_TYPE -> userBankAccounts = user.banksZAR
      Constants.NGNT_ASSET_TYPE -> userBankAccounts = user.banksNGNT
    }

    val fullNameString = "${user.first_name} ${user.last_name}"
    nameText?.text = fullNameString
    phoneText?.text = user.phone
    updateView()
  }

  private fun updateView() {
    if (userBankAccounts.isEmpty()) {
      showAddBankView()
      hideSelectedBankView()
      hideAmountInput()
    } else {
      hideAddBankView()
      showSelectedBankView()
      showAmountInput()
    }
  }

  private fun showSelectedBankView() {
    val selectedBankAccount = userBankAccounts[0]
    selectedBankName?.text = selectedBankAccount.name
    selectedBankBranch?.text = getString(R.string.pattern_branch_number, selectedBankAccount.branch)
    selectedBankAccountNumber?.text = selectedBankAccount.number
    selectedBankAccountType?.text = selectedBankAccount.type.toLowerCase()
    selectedBankContainer?.visibility = View.VISIBLE
  }

  private fun hideSelectedBankView() {
    selectedBankContainer?.visibility = View.GONE
  }

  private fun showAddBankView() {
    addBankContainer?.visibility = View.VISIBLE
  }

  private fun hideAddBankView() {
    addBankContainer?.visibility = View.GONE
  }

  private fun checkAddBankButton() {
    addBankButton.isEnabled = bankToAdd.isValid()
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

    ViewUtils.hideKeyboard(this)

    userBankAccounts.add(bankToAdd)
    updateView()

    when (assetCode) {
      Constants.ZAR_ASSET_TYPE ->
        Firebase.getUserBanksZarRef(dmcUser.uid).setValue(userBankAccounts)
      Constants.NGNT_ASSET_TYPE ->
        Firebase.getUserBanksNgntRef(dmcUser.uid).setValue(userBankAccounts)
    }

  }

  private fun showDepositAmount() {
    depositAmountContainer.visibility = View.VISIBLE
    depositAmountText.text = String.format("%s %s", StringFormat.truncateDecimalPlaces(
      amount, MAX_DECIMALS), assetCode)
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
    submitButton.isEnabled = this.amount > 0
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

      val amount = StringFormat.truncateDecimalPlaces(amount, MAX_DECIMALS)
      when (assetCode) {
        Constants.NGNT_ASSET_TYPE -> {
          // Use Cowrie exchange api to request deposit NGNT
          CowrieRetrofit.create().ngnForNgnt(dmcUser.stellar_address).enqueue(object : Callback<DepositResponse> {
            override fun onResponse(call: Call<DepositResponse>, response: Response<DepositResponse>) {
              response.body()?.let {
                val anchorBank = BankAccount(it.accountName, "", "", it.accountNumber, it.bankName)
                val deposit = Deposit(assetCode, amount, it.depositRef, anchorBank, userBankAccounts[0])
                MailHelper.notifyAboutNewDeposit(dmcUser, deposit)
                finishWithToast(getString(R.string.request_sent_message, modeString))
              }
            }

            override fun onFailure(call: Call<DepositResponse>, t: Throwable) {
              Timber.e(t)
              toast(t.localizedMessage)
              hideProgressBar()
            }
          })
        }
        Constants.ZAR_ASSET_TYPE -> {
          // Notify user with our ZAR banking info
          val anchorBank = BankAccount("DMC Rand (Pty) Ltd", "250655",
            "Current/Cheque Account", "62756496496", "FNB")
          val depositRef = "${dmcUser.email_address}*demars.io"
          val deposit = Deposit(assetCode, amount, depositRef, anchorBank, userBankAccounts[0])
          MailHelper.notifyAboutNewDeposit(dmcUser, deposit)
          finishWithToast(getString(R.string.request_sent_message, modeString))
        }
      }
    } else {
      confirmButton.isEnabled = true
      NetworkUtils(applicationContext).displayNoNetwork()
    }
  }

  private fun confirmWithdrawal() {
    confirmButton.isEnabled = false

    val fee = StringFormat.truncateDecimalPlaces(amount * 0.01, MAX_DECIMALS)
    val amount = StringFormat.truncateDecimalPlaces(amount * 0.99, MAX_DECIMALS)
    val secretSeed = AccountUtils.getSecretSeed(applicationContext)

    if (NetworkUtils(applicationContext).isNetworkAvailable()) {
      showProgressBar()

      when (assetCode) {
        Constants.NGNT_ASSET_TYPE -> {
          withdrawNgnt(secretSeed, amount, fee)
        }
        Constants.RTGS_ASSET_TYPE -> {
          withdrawZar(secretSeed, amount, fee)
        }
      }
    } else {
      confirmButton.isEnabled = true
      NetworkUtils(applicationContext).displayNoNetwork()
    }
  }

  private fun withdrawNgnt(secretSeed: CharArray, amount: String, fee: String) {
    val bankAccount = userBankAccounts[0]
    CowrieRetrofit.create().ngntForNgn(bankAccount.branch, bankAccount.number).enqueue(object : Callback<WithdrawalResponse> {
      override fun onResponse(call: Call<WithdrawalResponse>, response: Response<WithdrawalResponse>) {
        if (!response.isSuccessful) {
          toast("Error requesting $assetCode withdrawal")
          hideProgressBar()
          return
        }

        response.body()?.let { info ->
          Horizon.getWithdrawTask(object : SuccessErrorCallback {
            override fun onSuccess() {
              val withdrawal = Withdrawal(assetCode, amount, fee, userBankAccounts[0])

              MailHelper.notifyAboutNewWithdrawal(dmcUser, withdrawal)

              finishWithToast(getString(R.string.request_sent_message,
                getString(R.string.withdrawal)))
            }

            override fun onError(error: HorizonException) {
              finishWithToast(error.localizedMessage)
            }
          }, assetCode, secretSeed, info.address, info.meta, amount, fee).execute()
        }
      }

      override fun onFailure(call: Call<WithdrawalResponse>, t: Throwable) {
        Timber.e(t)
        toast(t.localizedMessage)
        hideProgressBar()
      }
    })
  }

  private fun withdrawZar(secretSeed: CharArray, amount: String, fee: String) {
    Horizon.getWithdrawTask(object : SuccessErrorCallback {
      override fun onSuccess() {
        val withdrawal = Withdrawal(assetCode, amount, fee, userBankAccounts[0])

        MailHelper.notifyAboutNewWithdrawal(dmcUser, withdrawal)

        finishWithToast(getString(R.string.request_sent_message,
          getString(R.string.withdrawal)))
      }

      override fun onError(error: HorizonException) {
        finishWithToast(error.localizedMessage)
        hideProgressBar()
      }
    }, assetCode, secretSeed, Constants.RTGS_ASSET_ISSUER, "", amount, fee).execute()
  }

  override fun onDestroy() {
    super.onDestroy()
    Firebase.removeUserListener(userListener)
    Firebase.removeAssetListener(assetCode, assetListener)
  }
}