package io.demars.stellarwallet.models

import com.brandongogetap.stickyheaders.exposed.StickyHeader

class OrderBookStickyHeader(type: OrderBookAdapterTypes): OrderBook(type = type), StickyHeader