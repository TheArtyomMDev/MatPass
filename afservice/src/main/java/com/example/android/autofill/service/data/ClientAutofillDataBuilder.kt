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

import androidx.annotation.RequiresApi
import android.os.Build
import com.example.android.autofill.service.ClientParser
import com.example.android.autofill.service.ClientParser.NodeProcessor
import android.app.assist.AssistStructure.ViewNode
import com.example.android.autofill.service.AutofillHints
import android.view.View
import com.example.android.autofill.service.model.AutofillDataset
import com.example.android.autofill.service.model.DatasetWithFilledAutofillFields
import com.example.android.autofill.service.model.FieldTypeWithHeuristics
import com.example.android.autofill.service.model.FilledAutofillField
import com.example.android.autofill.service.util.Util
import com.google.common.collect.ImmutableList
import java.util.*

class ClientAutofillDataBuilder(
    private val mFieldTypesByAutofillHint: HashMap<String?, FieldTypeWithHeuristics?>?,
    private val mPackageName: String?, private val mClientParser: ClientParser?
) : AutofillDataBuilder {
    override fun buildDatasetsByPartition(datasetNumber: Int): MutableList<DatasetWithFilledAutofillFields?>? {
        val listBuilder = ImmutableList.Builder<DatasetWithFilledAutofillFields?>()
        for (partition in AutofillHints.PARTITIONS!!) {
            val autofillDataset = mPackageName?.let {
                AutofillDataset(UUID.randomUUID().toString(),
                    "dataset-$datasetNumber.$partition", it)
            }
            val dataset = buildDatasetForPartition(autofillDataset, partition)
            if (dataset != null && dataset.filledAutofillFields != null) {
                listBuilder.add(dataset)
            }
        }
        return listBuilder.build()
    }

    /**
     * Parses a client view structure and build a dataset (in the form of a
     * [DatasetWithFilledAutofillFields]) from the view metadata found.
     */
    private fun buildDatasetForPartition(
        dataset: AutofillDataset?,
        partition: Int
    ): DatasetWithFilledAutofillFields? {
        val datasetWithFilledAutofillFields = DatasetWithFilledAutofillFields()
        datasetWithFilledAutofillFields.autofillDataset = dataset
        mClientParser!!.parse(object : NodeProcessor {
            override fun processNode(node: ViewNode?) {
                parseAutofillFields(node,
                    datasetWithFilledAutofillFields,
                    partition)
            }
        }
        )
        return datasetWithFilledAutofillFields
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun parseAutofillFields(
        viewNode: ViewNode?,
        datasetWithFilledAutofillFields: DatasetWithFilledAutofillFields?, partition: Int
    ) {
        val hints = viewNode!!.getAutofillHints()
        if (hints == null || hints.size == 0) {
            return
        }
        val autofillValue = viewNode.getAutofillValue()
        var textValue: String? = null
        var dateValue: Long? = null
        var toggleValue: Boolean? = null
        var autofillOptions: Array<CharSequence?>? = null
        var listIndex: Int? = null
        if (autofillValue != null) {
            if (autofillValue.isText) {
                // Using toString of AutofillValue.getTextValue in order to save it to
                // SharedPreferences.
                textValue = autofillValue.textValue.toString()
            } else if (autofillValue.isDate) {
                dateValue = autofillValue.dateValue
            } else if (autofillValue.isList) {
                autofillOptions = viewNode.getAutofillOptions()
                listIndex = autofillValue.listValue
            } else if (autofillValue.isToggle) {
                toggleValue = autofillValue.toggleValue
            }
        }
        if (datasetWithFilledAutofillFields != null) {
            appendViewMetadata(datasetWithFilledAutofillFields,
                hints, partition, textValue, dateValue, toggleValue,
                autofillOptions, listIndex)
        }
    }

    private fun appendViewMetadata(
        datasetWithFilledAutofillFields: DatasetWithFilledAutofillFields,
        hints: Array<String?>,
        partition: Int,
        textValue: String?,
        dateValue: Long?,
        toggleValue: Boolean?,
        autofillOptions: Array<CharSequence?>?,
        listIndex: Int?
    ) {
        var textValue = textValue
        for (i in hints.indices) {
            val hint = hints[i]
            // Then check if the "actual" hint is supported.
            val fieldTypeWithHeuristics = mFieldTypesByAutofillHint!!.get(hint)
            if (fieldTypeWithHeuristics != null) {
                val fieldType = fieldTypeWithHeuristics.fieldType
                if (!AutofillHints.matchesPartition(fieldType!!.getPartition(), partition)) {
                    continue
                }
                // Only add the field if the hint is supported by the type.
                if (textValue != null) {
                    if (!fieldType.getAutofillTypes().ints!!.contains(View.AUTOFILL_TYPE_TEXT)) {
                        Util.loge("Text is invalid type for hint '%s'", hint)
                    }
                }
                if (autofillOptions != null && listIndex != null && autofillOptions.size > listIndex) {
                    if (!fieldType.getAutofillTypes().ints!!.contains(View.AUTOFILL_TYPE_LIST)) {
                        Util.loge("List is invalid type for hint '%s'", hint)
                    }
                    textValue = autofillOptions[listIndex].toString()
                }
                if (dateValue != null) {
                    if (!fieldType.getAutofillTypes().ints!!.contains(View.AUTOFILL_TYPE_DATE)) {
                        Util.loge("Date is invalid type for hint '%s'", hint)
                    }
                }
                if (toggleValue != null) {
                    if (!fieldType.getAutofillTypes().ints!!.contains(View.AUTOFILL_TYPE_TOGGLE)) {
                        Util.loge("Toggle is invalid type for hint '%s'", hint)
                    }
                }
                val datasetId = datasetWithFilledAutofillFields.autofillDataset!!.getId()
                datasetWithFilledAutofillFields.add(FilledAutofillField(datasetId,
                    fieldType.getTypeName(), textValue, dateValue, toggleValue))
            } else {
                Util.loge("Invalid hint: %s", hint)
            }
        }
    }
}