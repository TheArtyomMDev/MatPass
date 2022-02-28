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
package com.example.android.autofill.service.settings

import androidx.annotation.RequiresApi
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.content.*
import com.example.android.autofill.service.util.Util

class MyPreferences private constructor(context: Context?) {
    private val mPrefs: SharedPreferences?

    /**
     * Gets whether [FillResponse]s should require authentication.
     */
    fun isResponseAuth(): Boolean {
        return mPrefs!!.getBoolean(RESPONSE_AUTH_KEY, false)
    }

    /**
     * Enables/disables authentication for the entire autofill [FillResponse].
     */
    fun setResponseAuth(responseAuth: Boolean) {
        mPrefs?.edit()?.putBoolean(RESPONSE_AUTH_KEY, responseAuth)?.apply()
    }

    /**
     * Gets whether [Dataset]s should require authentication.
     */
    fun isDatasetAuth(): Boolean {
        return mPrefs!!.getBoolean(DATASET_AUTH_KEY, false)
    }

    /**
     * Enables/disables authentication for individual autofill [Dataset]s.
     */
    fun setDatasetAuth(datasetAuth: Boolean) {
        mPrefs!!.edit().putBoolean(DATASET_AUTH_KEY, datasetAuth).apply()
    }

    /**
     * Gets autofill master username.
     */
    fun getMasterPassword(): String? {
        return mPrefs!!.getString(MASTER_PASSWORD_KEY, null)
    }

    /**
     * Sets autofill master password.
     */
    fun setMasterPassword(masterPassword: String) {
        mPrefs!!.edit().putString(MASTER_PASSWORD_KEY, masterPassword).apply()
    }

    fun clearCredentials() {
        mPrefs!!.edit().remove(MASTER_PASSWORD_KEY).apply()
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun getLoggingLevel(): Util.LogLevel? {
        return Util.LogLevel.values()[mPrefs!!.getInt(LOGGING_LEVEL, Util.LogLevel.Off.ordinal)]
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun setLoggingLevel(level: Util.LogLevel?) {
        mPrefs!!.edit().putInt(LOGGING_LEVEL, level!!.ordinal).apply()
        Util.setLoggingLevel(level)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun getDalCheckRequirement(): Util.DalCheckRequirement? {
        return Util.DalCheckRequirement.values()[mPrefs!!.getInt(DAL_CHECK_REQUIRED,
            Util.DalCheckRequirement.AllUrls.ordinal)]
    }

    fun setDalCheckRequired(level: Util.DalCheckRequirement?) {
        mPrefs!!.edit().putInt(DAL_CHECK_REQUIRED, level!!.ordinal).apply()
    }

    fun getNumberDatasets(defaultNumber: Int): Int {
        return mPrefs!!.getInt(NUMBER_DATASETS, defaultNumber)
    }

    fun setNumberDatasets(number: Int) {
        mPrefs!!.edit().putInt(NUMBER_DATASETS, number).apply()
    }

    companion object {
        private val RESPONSE_AUTH_KEY: String? = "response_auth"
        private val DATASET_AUTH_KEY: String? = "dataset_auth"
        private val MASTER_PASSWORD_KEY: String? = "master_password"
        private val LOGGING_LEVEL: String? = "logging_level"
        private val DAL_CHECK_REQUIRED: String? = "dal_check_required"
        private val NUMBER_DATASETS: String? = "number_datasets"
        private var sInstance: MyPreferences? = null
        fun getInstance(context: Context?): MyPreferences? {
            if (sInstance == null) {
                sInstance = MyPreferences(context)
            }
            return sInstance
        }
    }

    init {
        mPrefs = context!!.getApplicationContext().getSharedPreferences("my-settings",
            Context.MODE_PRIVATE)
    }
}