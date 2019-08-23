package io.demars.stellarwallet.models.ui

import com.brandongogetap.stickyheaders.exposed.StickyHeader

class OrderBookStickyHeader(type: OrderBookAdapterTypes): OrderBook(type = type), StickyHeader