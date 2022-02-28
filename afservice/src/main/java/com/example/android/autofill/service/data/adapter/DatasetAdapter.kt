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
package com.example.android.autofill.service.data.adapter

import com.example.android.autofill.service.ClientParser
import android.widget.RemoteViews
import android.service.autofill.Dataset
import kotlin.jvm.JvmOverloads
import android.content.IntentSender
import android.util.MutableBoolean
import com.example.android.autofill.service.ClientParser.NodeProcessor
import android.app.assist.AssistStructure.ViewNode
import com.example.android.autofill.service.AutofillHints
import android.view.autofill.AutofillValue
import android.view.View
import com.example.android.autofill.service.model.DatasetWithFilledAutofillFields
import com.example.android.autofill.service.model.FieldType
import com.example.android.autofill.service.model.FieldTypeWithHeuristics
import com.example.android.autofill.service.model.FilledAutofillField
import com.example.android.autofill.service.util.Util
import java.util.*
import java.util.function.Function
import java.util.stream.Collectors

class DatasetAdapter(private val mClientParser: ClientParser?) {
    fun buildDatasetForFocusedNode(
        filledAutofillField: FilledAutofillField?,
        fieldType: FieldType?, remoteViews: RemoteViews?
    ): Dataset? {
        val datasetBuilder = remoteViews?.let { Dataset.Builder(it) }
        val setAtLeastOneValue = bindDatasetToFocusedNode(filledAutofillField,
            fieldType, datasetBuilder)
        return if (!setAtLeastOneValue) {
            null
        } else datasetBuilder!!.build()
    }
    /**
     * Wraps autofill data in a [Dataset] object with an IntentSender, which can then be
     * sent back to the client.
     */
    /**
     * Wraps autofill data in a [Dataset] object which can then be sent back to the client.
     */
    @JvmOverloads
    fun buildDataset(
        fieldTypesByAutofillHint: HashMap<String?, FieldTypeWithHeuristics?>?,
        datasetWithFilledAutofillFields: DatasetWithFilledAutofillFields?,
        remoteViews: RemoteViews?,
        intentSender: IntentSender? =
            null
    ): Dataset? {
        val datasetBuilder = remoteViews?.let { Dataset.Builder(it) }
        if (intentSender != null) {
            datasetBuilder!!.setAuthentication(intentSender)
        }
        val setAtLeastOneValue = bindDataset(fieldTypesByAutofillHint,
            datasetWithFilledAutofillFields, datasetBuilder)
        return if (!setAtLeastOneValue) {
            null
        } else datasetBuilder!!.build()
    }

    /**
     * Build an autofill [Dataset] using saved data and the client's AssistStructure.
     */
    private fun bindDataset(
        fieldTypesByAutofillHint: HashMap<String?, FieldTypeWithHeuristics?>?,
        datasetWithFilledAutofillFields: DatasetWithFilledAutofillFields?,
        datasetBuilder: Dataset.Builder?
    ): Boolean {
        val setValueAtLeastOnce = MutableBoolean(false)
        val filledAutofillFieldsByTypeName =
            datasetWithFilledAutofillFields!!.filledAutofillFields!!.stream()
                .collect(Collectors.toMap({ obj: FilledAutofillField? -> obj!!.getFieldTypeName() },
                    Function.identity()))
        mClientParser!!.parse(object : NodeProcessor {
            override fun processNode(node: ViewNode?) {
                parseAutofillFields(node, fieldTypesByAutofillHint, filledAutofillFieldsByTypeName,
                    datasetBuilder, setValueAtLeastOnce)
            }
        }
        )
        return setValueAtLeastOnce.value
    }

    private fun bindDatasetToFocusedNode(
        field: FilledAutofillField?,
        fieldType: FieldType?, builder: Dataset.Builder?
    ): Boolean {
        val setValueAtLeastOnce = MutableBoolean(false)
        mClientParser!!.parse(object : NodeProcessor {
            override fun processNode(node: ViewNode?) {
                if (node!!.isFocused() && node.getAutofillId() != null) {
                    bindValueToNode(node, field, builder, setValueAtLeastOnce)
                }
            }
        })
        return setValueAtLeastOnce.value
    }

    private fun parseAutofillFields(
        viewNode: ViewNode?,
        fieldTypesByAutofillHint: HashMap<String?, FieldTypeWithHeuristics?>?,
        filledAutofillFieldsByTypeName: MutableMap<String?, FilledAutofillField?>?,
        builder: Dataset.Builder?, setValueAtLeastOnce: MutableBoolean?
    ) {
        val rawHints = viewNode!!.getAutofillHints()
        if (rawHints == null || rawHints.size == 0) {
            Util.logv("No af hints at ViewNode - %s", viewNode.getIdEntry())
            return
        }
        val fieldTypeName = AutofillHints.getFieldTypeNameFromAutofillHints(
            fieldTypesByAutofillHint, Arrays.asList(*rawHints)) ?: return
        val field = filledAutofillFieldsByTypeName!!.get(fieldTypeName) ?: return
        bindValueToNode(viewNode, field, builder, setValueAtLeastOnce)
    }

    fun bindValueToNode(
        viewNode: ViewNode?,
        field: FilledAutofillField?, builder: Dataset.Builder?,
        setValueAtLeastOnce: MutableBoolean?
    ) {
        val autofillId = viewNode!!.getAutofillId()
        if (autofillId == null) {
            Util.logw("Autofill ID null for %s", viewNode.toString())
            return
        }
        val autofillType = viewNode!!.getAutofillType()
        when (autofillType) {
            View.AUTOFILL_TYPE_LIST -> {
                val options = viewNode.getAutofillOptions()
                var listValue = -1
                if (options != null) {
                    listValue = Util.indexOf(viewNode.getAutofillOptions()!!, field!!.getTextValue())
                }
                if (listValue != -1) {
                    builder!!.setValue(autofillId, AutofillValue.forList(listValue))
                    setValueAtLeastOnce!!.value = true
                }
            }
            View.AUTOFILL_TYPE_DATE -> {
                val dateValue = field!!.getDateValue()
                if (dateValue != null) {
                    builder!!.setValue(autofillId, AutofillValue.forDate(dateValue))
                    setValueAtLeastOnce!!.value = true
                }
            }
            View.AUTOFILL_TYPE_TEXT -> {
                val textValue = field!!.getTextValue()
                if (textValue != null) {
                    builder!!.setValue(autofillId, AutofillValue.forText(textValue))
                    setValueAtLeastOnce!!.value = true
                }
            }
            View.AUTOFILL_TYPE_TOGGLE -> {
                val toggleValue = field!!.getToggleValue()
                if (toggleValue != null) {
                    builder!!.setValue(autofillId, AutofillValue.forToggle(toggleValue))
                    setValueAtLeastOnce!!.value = true
                }
            }
            View.AUTOFILL_TYPE_NONE -> Util.logw("Invalid autofill type - %d", autofillType)
            else -> Util.logw("Invalid autofill type - %d", autofillType)
        }
    }
}