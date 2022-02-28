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
package com.example.android.autofill.service.data.source.local

import android.content.pm.PackageManager
import android.content.*
import com.example.android.autofill.service.data.source.PackageVerificationDataSource
import com.example.android.autofill.service.util.SecurityHelper
import com.example.android.autofill.service.util.Util
import java.lang.Exception

class SharedPrefsPackageVerificationRepository private constructor(context: Context?) :
    PackageVerificationDataSource {
    private val mSharedPrefs: SharedPreferences?
    private val mContext: Context?
    override fun clear() {
        mSharedPrefs!!.edit().clear().apply()
    }

    override fun putPackageSignatures(packageName: String?): Boolean {
        val hash: String
        try {
            val pm = mContext!!.getPackageManager()
            val packageInfo = pm.getPackageInfo(packageName!!, PackageManager.GET_SIGNATURES)
            hash = SecurityHelper.Companion.getFingerprint(packageInfo, packageName)!!
            Util.logd("Hash for %s: %s", packageName, hash)
        } catch (e: Exception) {
            Util.logw(e, "Error getting hash for %s.", packageName)
            return false
        }
        if (!containsSignatureForPackage(packageName)) {
            // Storage does not yet contain signature for this package name.
            mSharedPrefs!!.edit().putString(packageName, hash).apply()
            return true
        }
        return containsMatchingSignatureForPackage(packageName, hash)
    }

    private fun containsSignatureForPackage(packageName: String?): Boolean {
        return mSharedPrefs!!.contains(packageName)
    }

    private fun containsMatchingSignatureForPackage(
        packageName: String?,
        hash: String?
    ): Boolean {
        return hash == mSharedPrefs!!.getString(packageName, null)
    }

    companion object {
        private val SHARED_PREF_KEY: String? = ("com.example.android.autofill.service"
                + ".datasource.PackageVerificationDataSource")
        private var sInstance: PackageVerificationDataSource? = null
        fun getInstance(context: Context?): PackageVerificationDataSource? {
            if (sInstance == null) {
                sInstance = SharedPrefsPackageVerificationRepository(
                    context!!.getApplicationContext())
            }
            return sInstance
        }
    }

    init {
        mSharedPrefs = context!!.getApplicationContext()
            .getSharedPreferences(SHARED_PREF_KEY, Context.MODE_PRIVATE)
        mContext = context.getApplicationContext()
    }
}