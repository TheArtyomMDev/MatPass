/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.autofill.service.model

import com.example.android.autofill.service.data.source.local.DigitalAssetLinksRepository

class DalInfo(webDomain: String?, packageName: String?) {
    private val mWebDomain: String?
    private val mPackageName: String?
    fun getWebDomain(): String? {
        return mWebDomain
    }

    fun getPackageName(): String? {
        return mPackageName
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val dalInfo = o as DalInfo?
        if (if (mWebDomain != null) mWebDomain != dalInfo!!.mWebDomain else dalInfo!!.mWebDomain != null) return false
        return if (mPackageName != null) mPackageName == dalInfo.mPackageName else dalInfo.mPackageName == null
    }

    override fun hashCode(): Int {
        var result = mWebDomain?.hashCode() ?: 0
        result = 31 * result + (mPackageName?.hashCode() ?: 0)
        return result
    }

    init {
        val canonicalDomain: String =
            DigitalAssetLinksRepository.Companion.getCanonicalDomain(webDomain)!!
        val fullDomain: String
        fullDomain = if (!webDomain!!.startsWith("http:") && !webDomain.startsWith("https:")) {
            // Unfortunately AssistStructure.ViewNode does not tell what the domain is, so let's
            // assume it's https
            "https://$canonicalDomain"
        } else {
            canonicalDomain
        }
        mWebDomain = fullDomain
        mPackageName = packageName
    }
}