package io.demars.stellarwallet.views

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.widget.AdapterView
import io.demars.stellarwallet.R
import android.widget.ArrayAdapter
import androidx.appcompat.widget.SearchView
import android.view.WindowManager
import android.widget.ListView
import io.demars.stellarwallet.utils.ViewUtils

@SuppressLint("InflateParams")
class SearchableListDialog(context: Context) : AlertDialog(context), SearchView.OnQueryTextListener {

  private var searchView: SearchView? = null
  private var listListView: ListView? = null
  private var currentList: List<String> = ArrayList()
  private var filteredList: List<String> = ArrayList()
  private var queryHint: String = ""

  private var adapter: ArrayAdapter<String>? = null
  private var listener: OnItemClick? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.dialog_searchable)

    window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
      WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
    window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

    searchView = findViewById(R.id.searchView)
    searchView?.setOnQueryTextListener(this)
    searchView?.isIconified = false
    searchView?.queryHint = queryHint
    listListView = findViewById(R.id.listView)
  }

  fun showForList(list: List<String>, queryHint:String,  itemClick: OnItemClick) {
    searchView?.setQuery("", false)
    searchView?.queryHint = queryHint

    this.queryHint = queryHint
    this.currentList = list
    this.filteredList = list
    this.listener = itemClick

    show()
    populateList()

    ViewUtils.showKeyboard(context, searchView)
  }

  private fun populateList() {
      adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, filteredList)
      listListView?.adapter = adapter
      listListView?.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
        listener?.itemClicked(filteredList[position])
    }
  }

  private fun filterList(query: String?) {
    filteredList = if (query?.trim().isNullOrEmpty()) {
      currentList
    } else {
      currentList.filter {
        it.contains(query?.trim() ?: "", true)
      }
    }
  }

  override fun onQueryTextSubmit(query: String?): Boolean {
    filterList(query)
    populateList()
    return true
  }

  override fun onQueryTextChange(newText: String?): Boolean {
    filterList(newText)
    populateList()
    return true
  }

  interface OnItemClick {
    fun itemClicked(item: String)
  }
}