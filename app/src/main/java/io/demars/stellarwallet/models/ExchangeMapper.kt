package io.demars.stellarwallet.models

import io.demars.stellarwallet.mvvm.exchange.ExchangeEntity

class ExchangeMapper {
    companion object {
        fun toExchangeEntities(exchanges : List<ExchangeApiModel>): List<ExchangeEntity> {
            return exchanges.map { toExchangeEntity(it) }
        }

        fun toExchangeEntity(exchange : ExchangeApiModel) : ExchangeEntity {
            return ExchangeEntity(exchange.name, exchange.address, exchange.memo)
        }
    }
}
