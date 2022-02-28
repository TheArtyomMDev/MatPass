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

import androidx.annotation.RequiresApi
import android.os.Build
import com.example.android.autofill.service.model.FieldType
import com.example.android.autofill.service.model.FieldTypeWithHeuristics
import com.example.android.autofill.service.model.FilledAutofillField
import com.example.android.autofill.service.util.Util
import java.util.*
import java.util.stream.Collectors

object AutofillHints {
    const val PARTITION_ALL = -1
    const val PARTITION_OTHER = 0
    const val PARTITION_ADDRESS = 1
    const val PARTITION_EMAIL = 2
    const val PARTITION_CREDIT_CARD = 3
    val PARTITIONS: IntArray? = intArrayOf(
        PARTITION_OTHER, PARTITION_ADDRESS, PARTITION_EMAIL, PARTITION_CREDIT_CARD
    )

    fun generateFakeField(
        fieldTypeWithHeuristics: FieldTypeWithHeuristics?, packageName: String?, seed: Int,
        datasetId: String?
    ): FilledAutofillField? {
        val fakeData = fieldTypeWithHeuristics?.fieldType?.getFakeData()
        val fieldTypeName = fieldTypeWithHeuristics?.fieldType?.getTypeName()
        var text: String? = null
        var date: Long? = null
        val toggle: Boolean? = null
        if (fakeData?.strictExampleSet != null && fakeData?.strictExampleSet!!.strings != null && fakeData.strictExampleSet!!.strings?.size!! > 0 &&
            !fakeData.strictExampleSet!!.strings?.get(0)?.isEmpty()!!
        ) {
            val choices = fakeData.strictExampleSet!!.strings
            text = choices!![seed % choices.size]
        } else if (fakeData?.textTemplate != null) {
            text = fakeData.textTemplate!!.replace("seed", "" + seed)
                .replace("curr_time", "" + Calendar.getInstance().timeInMillis)
        } else if (fakeData?.dateTemplate != null) {
            if (fakeData.dateTemplate!!.contains("curr_time")) {
                date = Calendar.getInstance().timeInMillis
            }
        }
        return datasetId?.let { FilledAutofillField(it, fieldTypeName!!, text, date, toggle) }
    }

    fun getFieldTypeNameFromAutofillHints(
        fieldTypesByAutofillHint: HashMap<String?, FieldTypeWithHeuristics?>?,
        hints: MutableList<String?>
    ): String? {
        return getFieldTypeNameFromAutofillHints(fieldTypesByAutofillHint, hints, PARTITION_ALL)
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    fun getFieldTypeNameFromAutofillHints(
        fieldTypesByAutofillHint: HashMap<String?, FieldTypeWithHeuristics?>?,
        hints: MutableList<String?>, partition: Int
    ): String? {
        val fieldTypeNames = removePrefixes(hints)
            ?.stream()
            ?.filter { key: String? -> fieldTypesByAutofillHint!!.containsKey(key) }
            ?.map { key: String? -> fieldTypesByAutofillHint!!.get(key) }
            ?.filter { obj: FieldTypeWithHeuristics? -> Objects.nonNull(obj) }
            ?.filter { fieldTypeWithHints: FieldTypeWithHeuristics? ->
                matchesPartition(fieldTypeWithHints!!.fieldType!!.getPartition(),
                    partition)
            }
            ?.map { obj: FieldTypeWithHeuristics? -> obj?.getFieldType() }
            ?.map { obj: FieldType? -> obj?.getTypeName() }
            ?.collect(Collectors.toList())
        return if (fieldTypeNames != null && fieldTypeNames.size > 0) {
            fieldTypeNames[0]
        } else {
            null
        }
    }

    fun matchesPartition(partition: Int, otherPartition: Int): Boolean {
        return partition == PARTITION_ALL || otherPartition == PARTITION_ALL || partition == otherPartition
    }

    private fun removePrefixes(hints: MutableList<String?>): MutableList<String?>? {
        val hintsWithoutPrefixes: MutableList<String?> = ArrayList()
        var nextHint: String? = null
        var i = 0
        while (i < hints.size) {
            var hint = hints[i]
            if (i < hints.size - 1) {
                nextHint = hints[i + 1]
            }
            // First convert the compound W3C autofill hints
            if (isW3cSectionPrefix(hint!!) && i < hints.size - 1) {
                i++
                hint = hints[i]
                Util.logd("Hint is a W3C section prefix; using %s instead", hint)
                if (i < hints.size - 1) {
                    nextHint = hints[i + 1]
                }
            }
            if (isW3cTypePrefix(hint!!) && nextHint != null && isW3cTypeHint(nextHint)) {
                hint = nextHint
                i++
                Util.logd("Hint is a W3C type prefix; using %s instead", hint)
            }
            if (isW3cAddressType(hint) && nextHint != null) {
                hint = nextHint
                i++
                Util.logd("Hint is a W3C address prefix; using %s instead", hint)
            }
            hintsWithoutPrefixes.add(hint)
            i++
        }
        return hintsWithoutPrefixes
    }

    private fun isW3cSectionPrefix(hint: String): Boolean {
        return hint.startsWith(W3cHints.PREFIX_SECTION!!)
    }

    private fun isW3cAddressType(hint: String): Boolean {
        when (hint) {
            W3cHints.SHIPPING, W3cHints.BILLING -> return true
        }
        return false
    }

    private fun isW3cTypePrefix(hint: String): Boolean {
        when (hint) {
            W3cHints.PREFIX_WORK, W3cHints.PREFIX_FAX, W3cHints.PREFIX_HOME, W3cHints.PREFIX_PAGER -> return true
        }
        return false
    }

    private fun isW3cTypeHint(hint: String): Boolean {
        when (hint) {
            W3cHints.TEL, W3cHints.TEL_COUNTRY_CODE, W3cHints.TEL_NATIONAL, W3cHints.TEL_AREA_CODE, W3cHints.TEL_LOCAL, W3cHints.TEL_LOCAL_PREFIX, W3cHints.TEL_LOCAL_SUFFIX, W3cHints.TEL_EXTENSION, W3cHints.EMAIL, W3cHints.IMPP -> return true
        }
        Util.logw("Invalid W3C type hint: %s", hint)
        return false
    }
}