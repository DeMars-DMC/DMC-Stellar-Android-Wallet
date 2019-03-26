package io.demars.stellarwallet.mvvm.exchange

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="exchanges")
data class ExchangeEntity(val name:String, val address:String, val memo:String) {
    @PrimaryKey(autoGenerate = true)
    var id : Long = 0
}
