package com.example.android.autofill.service

object W3cHints {
    // Optional W3C prefixes
    val PREFIX_SECTION: String? = "section-"
    val SHIPPING: String? = "shipping"
    val BILLING: String? = "billing"

    // W3C prefixes below...
    val PREFIX_HOME: String? = "home"
    val PREFIX_WORK: String? = "work"
    val PREFIX_FAX: String? = "fax"
    val PREFIX_PAGER: String? = "pager"

    // ... require those suffix
    val TEL: String? = "tel"
    val TEL_COUNTRY_CODE: String? = "tel-country-code"
    val TEL_NATIONAL: String? = "tel-national"
    val TEL_AREA_CODE: String? = "tel-area-code"
    val TEL_LOCAL: String? = "tel-local"
    val TEL_LOCAL_PREFIX: String? = "tel-local-prefix"
    val TEL_LOCAL_SUFFIX: String? = "tel-local-suffix"
    val TEL_EXTENSION: String? = "tel_extension"
    val EMAIL: String? = "email"
    val IMPP: String? = "impp"
}