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

import com.example.android.autofill.service.ClientParser
import com.example.android.autofill.service.ClientParser.NodeProcessor
import android.app.assist.AssistStructure.ViewNode
import android.view.autofill.AutofillId
import android.util.MutableInt
import com.example.android.autofill.service.model.FieldTypeWithHeuristics
import com.example.android.autofill.service.util.Util
import java.lang.StringBuilder
import java.util.ArrayList
import java.util.HashMap

class ClientViewMetadataBuilder(
    private val mClientParser: ClientParser?,
    private val mFieldTypesByAutofillHint: HashMap<String?, FieldTypeWithHeuristics?>?
) {
    fun buildClientViewMetadata(): ClientViewMetadata? {
        val allHints: MutableList<String?> = ArrayList()
        val saveType = MutableInt(0)
        val autofillIds: MutableList<AutofillId?> = ArrayList()
        val webDomainBuilder = StringBuilder()
        val focusedAutofillIds: MutableList<AutofillId?> = ArrayList()
        mClientParser!!.parse(object : NodeProcessor {
            override fun processNode(node: ViewNode?) {
                parseNode(node,
                    allHints,
                    saveType,
                    autofillIds,
                    focusedAutofillIds)
            }
        })
        mClientParser.parse(object : NodeProcessor {
            override fun processNode(node: ViewNode?) {
                parseWebDomain(node,
                    webDomainBuilder)
            }
        })
        val webDomain = webDomainBuilder.toString()
        val autofillIdsArray = autofillIds.toTypedArray()
        val focusedIds = focusedAutofillIds.toTypedArray()
        return ClientViewMetadata(allHints, saveType.value, autofillIdsArray, focusedIds, webDomain)
    }

    private fun parseWebDomain(viewNode: ViewNode?, validWebDomain: StringBuilder?) {
        val webDomain = viewNode!!.getWebDomain()
        if (webDomain != null) {
            Util.logd("child web domain: %s", webDomain)
            if (validWebDomain!!.length > 0) {
                if (webDomain != validWebDomain.toString()) {
                    throw SecurityException("Found multiple web domains: valid= "
                            + validWebDomain + ", child=" + webDomain)
                }
            } else {
                validWebDomain.append(webDomain)
            }
        }
    }

    private fun parseNode(
        root: ViewNode?, allHints: MutableList<String?>?,
        autofillSaveType: MutableInt?, autofillIds: MutableList<AutofillId?>?,
        focusedAutofillIds: MutableList<AutofillId?>?
    ) {
        val hints = root!!.getAutofillHints()
        if (hints != null) {
            for (hint in hints) {
                val fieldTypeWithHints = mFieldTypesByAutofillHint!!.get(hint)
                if (fieldTypeWithHints != null && fieldTypeWithHints.fieldType != null) {
                    allHints!!.add(hint)
                    autofillSaveType!!.value =
                        autofillSaveType.value or fieldTypeWithHints.fieldType!!.getSaveInfo()
                    autofillIds!!.add(root.getAutofillId())
                }
            }
        }
        if (root.isFocused()) {
            focusedAutofillIds!!.add(root.getAutofillId())
        }
    }
}