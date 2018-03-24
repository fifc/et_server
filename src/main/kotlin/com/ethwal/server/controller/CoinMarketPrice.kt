package com.ethwal.server.controller

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
        val price_eur: String? = null,
        val h24_volume_eur: String? = null,
        val market_cap_eur: String? = null
)