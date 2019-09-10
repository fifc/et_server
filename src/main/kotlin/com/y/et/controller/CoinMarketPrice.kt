package com.y.et.controller

data class CoinMarketPrice (
        val id: String? = null,
        val name: String? = null,
        var symbol: String? = null,
        val rank: String? = null,
        val price_usd: String? = null,
        val price_btc: String? = null,
        val h24_volume_usd: String? = null,
        val market_cap_usd: String? = null,
        val available_supply: String? = null,
        val total_supply: String? = null,
        val max_supply: String? = null,
        val percent_change_1h: String? = null,
        val percent_change_24h: String? = null,
        val percent_change_7d: String? = null,
        val last_updated: String? = null,
        val price_cny: String? = null,
        val h24_volume_cny: String? = null,
        val market_cap_cny: String? = null
)
