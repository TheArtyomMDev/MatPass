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
package com.example.android.autofill.service

import com.example.android.autofill.service.model.FilledAutofillField
import java.util.*

/**
 * Holds the properties associated with an autofill hint in this Autofill Service.
 */
class AutofillHintProperties(
    private val mAutofillHint: String?, private val mSaveType: Int, private val mPartition: Int,
    private val mFakeFieldGenerator: FakeFieldGenerator?, vararg validTypes: Int?
) {
    private val mValidTypes: MutableSet<Int?>?

    /**
     * Generates dummy autofill field data that is relevant to the autofill hint.
     */
    fun generateFakeField(seed: Int, datasetId: String?): FilledAutofillField? {
        return mFakeFieldGenerator?.generate(seed, datasetId)
    }

    /**
     * Returns autofill hint associated with these properties. If you save a field that uses a W3C
     * hint, there is a chance this will return a different but analogous hint, when applicable.
     * For example, W3C has hint 'email' and [android.view.View] has hint 'emailAddress', so
     * the W3C hint should map to the hint defined in [android.view.View] ('emailAddress').
     */
    fun getAutofillHint(): String? {
        return mAutofillHint
    }

    /**
     * Returns how this hint maps to a [android.service.autofill.SaveInfo] type.
     */
    fun getSaveType(): Int {
        return mSaveType
    }

    /**
     * Returns which data partition this autofill hint should be a part of. See partitions defined
     * in [AutofillHints].
     */
    fun getPartition(): Int {
        return mPartition
    }

    /**
     * Sometimes, data for a hint should only be stored as a certain AutofillValue type. For
     * example, it is recommended that data representing a Credit Card Expiration date, annotated
     * with the hint [View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE], should
     * only be stored as [View.AUTOFILL_TYPE_DATE].
     */
    fun isValidType(type: Int): Boolean {
        return mValidTypes!!.contains(type)
    }

    fun getTypes(): MutableSet<Int?>? {
        return mValidTypes
    }

    init {
        mValidTypes = HashSet(Arrays.asList(*validTypes))
    }
}