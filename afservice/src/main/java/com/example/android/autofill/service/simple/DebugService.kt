/*
 * Copyright (C) 2018 The Android Open Source Project
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
package com.example.android.autofill.service.simple

import androidx.annotation.RequiresApi
import android.widget.RemoteViews
import android.service.autofill.Dataset
import android.app.assist.AssistStructure.ViewNode
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.service.autofill.FillResponse
import android.service.autofill.SaveInfo
import android.app.assist.AssistStructure
import android.service.autofill.AutofillService
import android.service.autofill.FillRequest
import android.service.autofill.FillCallback
import android.service.autofill.SaveRequest
import android.service.autofill.SaveCallback
import android.widget.Toast
import android.content.*
import android.os.*
import android.text.TextUtils
import android.util.ArrayMap
import android.util.Log
import android.view.View
import com.example.android.autofill.service.settings.MyPreferences

/**
 * A basic service that provides autofill data for pretty much any input field, even those not
 * annotated with autfoill hints.
 *
 *
 * The goal of this class is to provide a simple autofill service implementation that can be used
 * to debug how other apps interact with autofill, it should **not** be used as a
 * reference for real autofill service implementations because it lacks fundamental security
 * requirements such as data partitioning and package verification &mdashthese requirements are
 * fullfilled by [MyAutofillService].
 */
@RequiresApi(api = Build.VERSION_CODES.O)
class DebugService : AutofillService() {
    private var mAuthenticateResponses = false
    private var mAuthenticateDatasets = false
    private var mNumberDatasets = 0
    override fun onConnected() {
        super.onConnected()

        // TODO(b/114236837): use its own preferences?
        val pref: MyPreferences? = MyPreferences.Companion.getInstance(
            applicationContext)
        mAuthenticateResponses = pref!!.isResponseAuth()
        mAuthenticateDatasets = pref.isDatasetAuth()
        mNumberDatasets = pref.getNumberDatasets(4)
        Log.d(TAG, "onConnected(): numberDatasets=" + mNumberDatasets
                + ", authResponses=" + mAuthenticateResponses
                + ", authDatasets=" + mAuthenticateDatasets)
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        Log.d(TAG, "onFillRequest()")

        // Find autofillable fields
        val structure: AssistStructure = BasicService.Companion.getLatestAssistStructure(request)
        val fields = getAutofillableFields(structure)
        Log.d(TAG, "autofillable fields:$fields")
        if (fields.isEmpty()) {
            toast("No autofill hints found")
            callback.onSuccess(null)
            return
        }

        // Create response...
        val response: FillResponse?
        if (mAuthenticateResponses) {
            val size = fields.size
            val hints = arrayOfNulls<String?>(size)
            val ids = arrayOfNulls<AutofillId?>(size)
            for (i in 0 until size) {
                hints[i] = fields.keyAt(i)
                ids[i] = fields.valueAt(i)
            }
            val authentication: IntentSender? =
                SimpleAuthActivity.Companion.newIntentSenderForResponse(this, hints,
                    ids, mAuthenticateDatasets)
            val presentation: RemoteViews = BasicService.Companion.newDatasetPresentation(
                packageName,
                "Tap to auth response")
            response = FillResponse.Builder()
                .setAuthentication(ids, authentication, presentation).build()
        } else {
            response = createResponse(this, fields, mNumberDatasets, mAuthenticateDatasets)
        }

        // ... and return it
        callback.onSuccess(response)
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        Log.d(TAG, "onSaveRequest()")
        toast("Save not supported")
        callback.onSuccess()
    }

    /**
     * Parses the [AssistStructure] representing the activity being autofilled, and returns a
     * map of autofillable fields (represented by their autofill ids) mapped by the hint associate
     * with them.
     *
     *
     * An autofillable field is a [ViewNode] whose [.getHint] metho
     */
    private fun getAutofillableFields(structure: AssistStructure): ArrayMap<String?, AutofillId?> {
        val fields = ArrayMap<String?, AutofillId?>()
        val nodes = structure.windowNodeCount
        for (i in 0 until nodes) {
            val node = structure.getWindowNodeAt(i).rootViewNode
            addAutofillableFields(fields, node)
        }
        return fields
    }

    /**
     * Adds any autofillable view from the [ViewNode] and its descendants to the map.
     */
    private fun addAutofillableFields(
        fields: MutableMap<String?, AutofillId?>,
        node: ViewNode
    ) {
        val hint = getHint(node)
        if (hint != null) {
            val id = node.autofillId
            if (!fields.containsKey(hint)) {
                Log.v(TAG, "Setting hint '$hint' on $id")
                fields[hint] = id
            } else {
                Log.v(TAG, "Ignoring hint '" + hint + "' on " + id
                        + " because it was already set")
            }
        }
        val childrenSize = node.childCount
        for (i in 0 until childrenSize) {
            addAutofillableFields(fields, node.getChildAt(i))
        }
    }

    protected fun getHint(node: ViewNode): String? {

        // First try the explicit autofill hints...
        val hints = node.autofillHints
        if (hints != null) {
            // We're simple, we only care about the first hint
            return hints[0].toLowerCase()
        }

        // Then try some rudimentary heuristics based on other node properties
        val viewHint = node.hint
        var hint = inferHint(node, viewHint)
        if (hint != null) {
            Log.d(TAG, "Found hint using view hint($viewHint): $hint")
            return hint
        } else if (!TextUtils.isEmpty(viewHint)) {
            Log.v(TAG, "No hint using view hint: $viewHint")
        }
        val resourceId = node.idEntry
        hint = inferHint(node, resourceId)
        if (hint != null) {
            Log.d(TAG, "Found hint using resourceId($resourceId): $hint")
            return hint
        } else if (!TextUtils.isEmpty(resourceId)) {
            Log.v(TAG, "No hint using resourceId: $resourceId")
        }
        val text = node.text
        val className: CharSequence? = node.className
        if (text != null && className != null && className.toString().contains("EditText")) {
            hint = inferHint(node, text.toString())
            if (hint != null) {
                // NODE: text should not be logged, as it could contain PII
                Log.d(TAG, "Found hint using text($text): $hint")
                return hint
            }
        } else if (!TextUtils.isEmpty(text)) {
            // NODE: text should not be logged, as it could contain PII
            Log.v(TAG, "No hint using text: $text and class $className")
        }
        return null
    }

    /**
     * Uses heuristics to infer an autofill hint from a `string`.
     *
     * @return standard autofill hint, or `null` when it could not be inferred.
     */
    protected fun inferHint(node: ViewNode?, actualHint: String?): String? {
        if (actualHint == null) return null
        val hint = actualHint.toLowerCase()
        if (hint.contains("label") || hint.contains("container")) {
            Log.v(TAG, "Ignoring 'label/container' hint: $hint")
            return null
        }
        if (hint.contains("password")) return View.AUTOFILL_HINT_PASSWORD
        if (hint.contains("username")
            || hint.contains("login") && hint.contains("id")
        ) return View.AUTOFILL_HINT_USERNAME
        if (hint.contains("email")) return View.AUTOFILL_HINT_EMAIL_ADDRESS
        if (hint.contains("name")) return View.AUTOFILL_HINT_NAME
        if (hint.contains("phone")) return View.AUTOFILL_HINT_PHONE

        // When everything else fails, return the full string - this is helpful to help app
        // developers visualize when autofill is triggered when it shouldn't (for example, in a
        // chat conversation window), so they can mark the root view of such activities with
        // android:importantForAutofill=noExcludeDescendants
        if (node!!.isEnabled() && node.getAutofillType() != View.AUTOFILL_TYPE_NONE) {
            Log.v(TAG, "Falling back to $actualHint")
            return actualHint
        }
        return null
    }

    /**
     * Displays a toast with the given message.
     */
    private fun toast(message: CharSequence) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        private val TAG: String? = "DebugService"
        fun createResponse(
            context: Context,
            fields: ArrayMap<String?, AutofillId?>, numDatasets: Int,
            authenticateDatasets: Boolean
        ): FillResponse? {
            val packageName = context.packageName
            val response = FillResponse.Builder()
            // 1.Add the dynamic datasets
            for (i in 1..numDatasets) {
                val unlockedDataset = newUnlockedDataset(fields, packageName, i)
                if (authenticateDatasets) {
                    val lockedDataset = Dataset.Builder()
                    for ((hint, id) in fields) {
                        val value = "$i-$hint"
                        val authentication: IntentSender? =
                            SimpleAuthActivity.Companion.newIntentSenderForDataset(context,
                                unlockedDataset!!)
                        val presentation: RemoteViews =
                            BasicService.Companion.newDatasetPresentation(packageName,
                                "Tap to auth $value")
                        lockedDataset.setValue(id!!, null, presentation)
                            .setAuthentication(authentication)
                    }
                    response.addDataset(lockedDataset.build())
                } else {
                    response.addDataset(unlockedDataset)
                }
            }

            // 2.Add save info
            val ids = fields.values
            val requiredIds = ids.toTypedArray()
            response.setSaveInfo( // We're simple, so we're generic
                SaveInfo.Builder(SaveInfo.SAVE_DATA_TYPE_GENERIC, requiredIds).build())

            // 3.Profit!
            return response.build()
        }

        fun newUnlockedDataset(
            fields: MutableMap<String?, AutofillId?>,
            packageName: String, i: Int
        ): Dataset? {
            val dataset = Dataset.Builder()
            for ((hint, id) in fields) {
                val value = "$i-$hint"

                // We're simple - our dataset values are hardcoded as "N-hint" (for example,
                // "1-username", "2-username") and they're displayed as such, except if they're a
                // password
                val displayValue = if (hint!!.contains("password")) "password for #$i" else value
                val presentation: RemoteViews =
                    BasicService.Companion.newDatasetPresentation(packageName, displayValue)
                dataset.setValue(id!!, AutofillValue.forText(value), presentation)
            }
            return dataset.build()
        }
    }
}