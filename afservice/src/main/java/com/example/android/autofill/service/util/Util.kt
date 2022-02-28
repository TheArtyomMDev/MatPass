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
package com.example.android.autofill.service.util

import androidx.annotation.RequiresApi
import android.os.Build
import android.app.assist.AssistStructure.ViewNode
import android.view.autofill.AutofillValue
import android.service.autofill.SaveInfo
import android.os.Bundle
import android.app.assist.AssistStructure
import android.service.autofill.FillContext
import android.util.Log
import android.view.View
import com.google.common.base.Joiner
import java.lang.StringBuilder
import java.util.*

@RequiresApi(api = Build.VERSION_CODES.O)
object Util {
    val EXTRA_DATASET_NAME: String? = "dataset_name"
    val EXTRA_FOR_RESPONSE: String? = "for_response"
    val AUTOFILL_ID_FILTER: NodeFilter? =
        object : NodeFilter {
            override fun matches(node: ViewNode?, id: Any?): Boolean {
                return id == node?.getAutofillId()
            }
        }
    private val TAG: String? = "AutofillSample"
    var sLoggingLevel: LogLevel? = LogLevel.Off
    private fun bundleToString(builder: StringBuilder?, data: Bundle?) {
        val keySet = data!!.keySet()
        builder!!.append("[Bundle with ").append(keySet.size).append(" keys:")
        for (key in keySet) {
            builder.append(' ').append(key).append('=')
            val value = data.get(key)
            if (value is Bundle) {
                bundleToString(builder, value as Bundle?)
            } else {
                builder.append(if (value is Array<*>) Arrays.toString(value as Array<Any?>?) else value)
            }
        }
        builder.append(']')
    }

    fun bundleToString(data: Bundle?): String? {
        if (data == null) {
            return "N/A"
        }
        val builder = StringBuilder()
        bundleToString(builder, data)
        return builder.toString()
    }

    fun getTypeAsString(type: Int): String? {
        when (type) {
            View.AUTOFILL_TYPE_TEXT -> return "TYPE_TEXT"
            View.AUTOFILL_TYPE_LIST -> return "TYPE_LIST"
            View.AUTOFILL_TYPE_NONE -> return "TYPE_NONE"
            View.AUTOFILL_TYPE_TOGGLE -> return "TYPE_TOGGLE"
            View.AUTOFILL_TYPE_DATE -> return "TYPE_DATE"
        }
        return "UNKNOWN_TYPE"
    }

    private fun getAutofillValueAndTypeAsString(value: AutofillValue?): String? {
        if (value == null) return "null"
        val builder = StringBuilder(value.toString()).append('(')
        if (value.isText) {
            builder.append("isText")
        } else if (value.isDate) {
            builder.append("isDate")
        } else if (value.isToggle) {
            builder.append("isToggle")
        } else if (value.isList) {
            builder.append("isList")
        }
        return builder.append(')').toString()
    }

    fun dumpStructure(structure: AssistStructure?) {
        if (logVerboseEnabled()) {
            val nodeCount = structure?.getWindowNodeCount()
            logv("dumpStructure(): component=%s numberNodes=%d",
                structure?.getActivityComponent(), nodeCount)
            for (i in 0 until nodeCount!!) {
                logv("node #%d", i)
                val node = structure.getWindowNodeAt(i)
                dumpNode(StringBuilder(), "  ", node.rootViewNode, 0)
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun dumpNode(
        builder: StringBuilder?,
        prefix: String?,
        node: ViewNode?,
        childNumber: Int
    ) {
        builder?.append(prefix)
            ?.append("child #")?.append(childNumber)?.append("\n")
        if (node != null) {
            builder?.append(prefix)
                ?.append("autoFillId: ")?.append(node?.getAutofillId())
                ?.append("\tidEntry: ")?.append(node.getIdEntry())
                ?.append("\tid: ")?.append(node.getId())
                ?.append("\tclassName: ")?.append(node.getClassName())
                ?.append('\n')
        }
        builder?.append(prefix)
            ?.append("focused: ")?.append(node?.isFocused())
            ?.append("\tvisibility")?.append(node?.getVisibility())
            ?.append("\tchecked: ")?.append(node?.isChecked())
            ?.append("\twebDomain: ")?.append(node?.getWebDomain())
            ?.append("\thint: ")?.append(node?.getHint())
            ?.append('\n')
        val htmlInfo = node?.getHtmlInfo()
        if (htmlInfo != null) {
            builder?.append(prefix)
                ?.append("HTML TAG: ")?.append(htmlInfo.tag)
                ?.append(" attrs: ")?.append(htmlInfo.attributes)
                ?.append('\n')
        }
        val afHints = node?.getAutofillHints()
        val options = node?.getAutofillOptions()
        builder?.append(prefix)?.append("afType: ")?.append(getTypeAsString(node!!.getAutofillType()))
            ?.append("\tafValue:")
            ?.append(getAutofillValueAndTypeAsString(node.getAutofillValue()))
            ?.append("\tafOptions:")?.append(if (options == null) "N/A" else Arrays.toString(options))
            ?.append("\tafHints: ")?.append(if (afHints == null) "N/A" else Arrays.toString(afHints))
            ?.append("\tinputType:")?.append(node.getInputType())
            ?.append('\n')
        val numberChildren = node?.getChildCount()
        builder?.append(prefix)?.append("# children: ")?.append(numberChildren)
            ?.append("\ttext: ")?.append(node?.getText())
            ?.append('\n')
        val prefix2 = "$prefix  "
        for (i in 0 until numberChildren!!) {
            dumpNode(builder, prefix2, node.getChildAt(i), i)
        }
        logv(builder.toString())
    }

    fun getSaveTypeAsString(type: Int): String? {
        val types: MutableList<String?> = ArrayList()
        if (type and SaveInfo.SAVE_DATA_TYPE_ADDRESS != 0) {
            types.add("ADDRESS")
        }
        if (type and SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD != 0) {
            types.add("CREDIT_CARD")
        }
        if (type and SaveInfo.SAVE_DATA_TYPE_EMAIL_ADDRESS != 0) {
            types.add("EMAIL_ADDRESS")
        }
        if (type and SaveInfo.SAVE_DATA_TYPE_USERNAME != 0) {
            types.add("USERNAME")
        }
        if (type and SaveInfo.SAVE_DATA_TYPE_PASSWORD != 0) {
            types.add("PASSWORD")
        }
        return if (types.isEmpty()) {
            "UNKNOWN($type)"
        } else Joiner.on('|').join(types)
    }

    /**
     * Gets a node if it matches the filter criteria for the given id.
     */
    fun findNodeByFilter(
        contexts: MutableList<FillContext?>, id: Any,
        filter: NodeFilter
    ): ViewNode? {
        for (context in contexts) {
            val node = findNodeByFilter(context!!.getStructure(), id, filter)
            if (node != null) {
                return node
            }
        }
        return null
    }

    /**
     * Gets a node if it matches the filter criteria for the given id.
     */
    fun findNodeByFilter(
        structure: AssistStructure, id: Any,
        filter: NodeFilter
    ): ViewNode? {
        logv("Parsing request for activity %s", structure.activityComponent)
        val nodes = structure.windowNodeCount
        for (i in 0 until nodes) {
            val windowNode = structure.getWindowNodeAt(i)
            val rootNode = windowNode.rootViewNode
            val node = findNodeByFilter(rootNode, id, filter)
            if (node != null) {
                return node
            }
        }
        return null
    }

    /**
     * Gets a node if it matches the filter criteria for the given id.
     */
    fun findNodeByFilter(
        node: ViewNode, id: Any,
        filter: NodeFilter
    ): ViewNode? {
        if (filter.matches(node, id)) {
            return node
        }
        val childrenSize = node.childCount
        if (childrenSize > 0) {
            for (i in 0 until childrenSize) {
                val found = findNodeByFilter(node.getChildAt(i), id, filter)
                if (found != null) {
                    return found
                }
            }
        }
        return null
    }

    fun logd(message: String?, vararg params: Any?) {
        if (logDebugEnabled()) {
            Log.d(TAG, String.format(message!!, *params))
        }
    }

    fun logv(message: String?, vararg params: Any?) {
        if (logVerboseEnabled()) {
            Log.v(TAG, String.format(message!!, *params))
        }
    }

    fun logDebugEnabled(): Boolean {
        return sLoggingLevel!!.ordinal >= LogLevel.Debug.ordinal
    }

    fun logVerboseEnabled(): Boolean {
        return sLoggingLevel!!.ordinal >= LogLevel.Verbose.ordinal
    }

    fun logw(message: String?, vararg params: Any?) {
        Log.w(TAG, String.format(message!!, *params))
    }

    fun logw(throwable: Throwable?, message: String?, vararg params: Any?) {
        Log.w(TAG, String.format(message!!, *params), throwable)
    }

    fun loge(message: String?, vararg params: Any?) {
        Log.e(TAG, String.format(message!!, *params))
    }

    fun loge(throwable: Throwable?, message: String?, vararg params: Any?) {
        Log.e(TAG, String.format(message!!, *params), throwable)
    }

    fun setLoggingLevel(level: LogLevel?) {
        sLoggingLevel = level
    }

    /**
     * Helper method for getting the index of a CharSequence object in an array.
     */
    fun indexOf(array: Array<CharSequence?>, charSequence: CharSequence?): Int {
        var index = -1
        if (charSequence == null) {
            return index
        }
        for (i in array.indices) {
            if (charSequence == array[i]) {
                index = i
                break
            }
        }
        return index
    }

    enum class LogLevel {
        Off, Debug, Verbose
    }

    enum class DalCheckRequirement {
        Disabled, LoginOnly, AllUrls
    }

    /**
     * Helper interface used to filter Assist nodes.
     */
    interface NodeFilter {
        /**
         * Returns whether the node passes the filter for such given id.
         */
        open fun matches(node: ViewNode?, id: Any?): Boolean
    }
}