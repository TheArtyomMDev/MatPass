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
package com.example.android.autofill.service.data

import android.view.autofill.AutofillId
import android.service.autofill.SaveInfo
import java.util.*

/**
 * In this simple implementation, the only view data we collect from the client are autofill hints
 * of the views in the view hierarchy, the corresponding autofill IDs, and the [SaveInfo]
 * based on the hints.
 */
class ClientViewMetadata(
    private val mAllHints: MutableList<String?>?,
    private val mSaveType: Int,
    private val mAutofillIds: Array<AutofillId?>?,
    private val mFocusedIds: Array<AutofillId?>?,
    private val mWebDomain: String?
) {
    fun getAllHints(): MutableList<String?>? {
        return mAllHints
    }

    fun getAutofillIds(): Array<AutofillId?>? {
        return mAutofillIds
    }

    fun getFocusedIds(): Array<AutofillId?>? {
        return mFocusedIds
    }

    fun getSaveType(): Int {
        return mSaveType
    }

    fun getWebDomain(): String? {
        return mWebDomain
    }

    override fun toString(): String {
        return "ClientViewMetadata{" +
                "mAllHints=" + mAllHints +
                ", mSaveType=" + mSaveType +
                ", mAutofillIds=" + Arrays.toString(mAutofillIds) +
                ", mWebDomain='" + mWebDomain + '\'' +
                ", mFocusedIds=" + Arrays.toString(mFocusedIds) +
                '}'
    }
}