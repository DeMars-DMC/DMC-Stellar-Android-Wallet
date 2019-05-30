package io.demars.stellarwallet.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import io.demars.stellarwallet.R
import io.demars.stellarwallet.firebase.DmcUser
import io.demars.stellarwallet.firebase.Firebase
import io.demars.stellarwallet.interfaces.AfterTextChanged
import io.demars.stellarwallet.models.BankAccount
import io.demars.stellarwallet.utils.AssetUtils
import io.demars.stellarwallet.utils.StringFormat
import io.demars.stellarwallet.utils.ViewUtils
import io.demars.stellarwallet.views.pin.PinLockView
import kotlinx.android.synthetic.main.activity_deposit.*
import kotlinx.android.synthetic.main.activity_deposit.amountTextView
import kotlinx.android.synthetic.main.activity_deposit.numberKeyboard
import kotlinx.android.synthetic.main.activity_deposit.toolbar

class DepositActivity : BaseActivity(), PinLockView.DialerListener {
  companion object {
    private const val ARG_ASSET_CODE = "ARG_ASSET_CODE"
    private const val RC_BANK_ACCOUNT = 111
    fun newInstance(context: Context, assetCode: String): Intent {
      val intent = Intent(context, DepositActivity::class.java)
      intent.putExtra(ARG_ASSET_CODE, assetCode)
      return intent
    }
  }

  private var assetCode = ""
  private var dmcUser: DmcUser? = null
  private var bankAccounts = ArrayList<BankAccount>()
  private var bankToAdd = BankAccount()
  private var amount = 0.0
  private var maxAmount = 0.0
  private var amountText = ""
  private var maxAmountText = "0.0"
  private val userListener = object : ValueEventListener {
    override fun onDataChange(dataSnapshot: DataSnapshot) {
      dataSnapshot.getValue(DmcUser::class.java)?.let { user ->
        onUserFetched(user)
      } ?: onError("Can't find user. Please try again")
    }

    override fun onCancelled(error: DatabaseError) {
      onError(error.message)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_deposit)
    checkIntent()
    setupUI()

    Firebase.getCurrentUser()?.let { _ ->
      Firebase.getUser(userListener)
    } ?: onError("Can't find user. Please try again")
  }

  private fun checkIntent() {
    intent?.extras?.let {
      assetCode = it.getString(ARG_ASSET_CODE, "")
      maxAmount = 5000.0 // later we can change it to constants
      maxAmountText = "5000.00"
    }
  }

  private fun setupUI() {
    setSupportActionBar(toolbar)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)
    supportActionBar?.title = getString(R.string.pattern_deposit, assetCode)

    numberKeyboard.mDialerListener = this

    submitButton.setOnClickListener {
      submitDeposit()
    }
  }

  private fun onUserFetched(user: DmcUser) {
    dmcUser = user
    bankAccounts = user.banksZAR
    val fullNameString = "${user.first_name} ${user.last_name}"
    nameText?.text = fullNameString
    phoneText?.text = user.phone
    updateView()
  }

  private fun updateView() {
    if (bankAccounts.isEmpty()) {
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
    val selectedBankAccount = bankAccounts[0]
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

    bankPicker?.setOnClickListener {
      val banks = resources.getStringArray(R.array.banks_zar)
      val branchCodes = resources.getStringArray(R.array.branches_zar)
      AlertDialog.Builder(this)
        .setTitle(R.string.select_bank)
        .setItems(banks) { _, which ->
          val bankName = banks[which]
          val branchCode = branchCodes[which]
          bankPicker?.setTextColor(Color.BLACK)
          bankPicker?.text = bankName

          bankToAdd.name = bankName
          bankToAdd.branch = branchCode

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

          checkAddBankButton()
        }.show()
    }

    addBankButton.setOnClickListener {
      addBank()
    }
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
    depositLimitText?.visibility = View.VISIBLE
    amountTextView?.visibility = View.VISIBLE
  }

  private fun hideAmountInput() {
    amountTextView?.visibility = View.GONE
    depositLimitText?.visibility = View.GONE
    submitButton?.visibility = View.GONE
    numberKeyboard?.visibility = View.GONE
  }


  private fun addBank() {
    if (!bankToAdd.isValid()) return

    ViewUtils.hideKeyboard(this)

    bankAccounts.add(bankToAdd)
    updateView()

    dmcUser?.let {
      Firebase.getBanksZARRef(it.uid).setValue(bankAccounts)
    }
  }

  private fun submitDeposit() {

  }

  //region Dialer
  override fun onDial(number: Int) {
    if (amountText.isEmpty() && number == 0) {
      return
    }
    updateAmount(amountText + number)
  }

  override fun onDelete() {
    if (amountText.isEmpty()) return

    var newAmountText: String
    if (amountText.length <= 1) {
      newAmountText = ""
    } else {
      newAmountText = amountText.substring(0, amountText.length - 1)
      if (newAmountText[newAmountText.length - 1] == '.') {
        newAmountText = newAmountText.substring(0, newAmountText.length - 1)
      }
      if ("0" == newAmountText) {
        newAmountText = ""
      }
    }
    updateAmount(newAmountText)
  }

  override fun onDeleteAll() {
    if (amountText.isEmpty()) return
    updateAmount("")
  }

  override fun onDot() {
    if (!StringFormat.hasDecimalPoint(amountText)) {
      amountText = if (amountText.isEmpty()) "0." else "$amountText."
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

  private fun showAmount(amount: String) {
    val amountToUse = when {
      amount.isEmpty() -> "0"
      else -> amount
    }

    amountTextView?.text = amountToUse
  }
  //endregion
  private fun openBankActivity() {
    // We will probably use it in future
    startActivityForResult(BankAccountActivity.newInstance(this), RC_BANK_ACCOUNT)
  }

  private fun onError(message: String) {
    dmcUser = DmcUser("dadasd", "+131231221312")
    dmcUser?.first_name = "Alex"
    dmcUser?.last_name = "Kurbetiev"
    onUserFetched(dmcUser!!)
//    ViewUtils.showToast(this, message)
//    finish()
  }

  override fun onDestroy() {
    super.onDestroy()
    Firebase.removeUserListener(userListener)
  }
}