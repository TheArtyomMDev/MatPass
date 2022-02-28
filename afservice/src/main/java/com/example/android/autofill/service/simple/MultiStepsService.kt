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
import android.app.assist.AssistStructure.ViewNode
import android.view.autofill.AutofillId
import android.service.autofill.FillResponse
import android.service.autofill.SaveInfo
import android.app.assist.AssistStructure
import android.service.autofill.AutofillService
import android.service.autofill.FillRequest
import android.service.autofill.FillCallback
import android.service.autofill.SaveRequest
import android.service.autofill.SaveCallback
import android.widget.Toast
import android.os.*
import android.util.ArrayMap
import android.util.Log
import android.view.View

/**
 * A basic service used to demonstrate multi-steps workflows (such as
 * `MultipleStepsSignInActivity` and `MultipleStepsCreditCardActivity`) by saving the
 * save type from previous requests in the client state bundle that's passed along to next requests.
 *
 *
 * This class should **not** be used as a reference for real autofill service
 * implementations because it lacks fundamental security requirements such as data partitioning and
 * package verification &mdashthese requirements are fullfilled by [MyAutofillService].
 */
@RequiresApi(api = Build.VERSION_CODES.O)
abstract class MultiStepsService : AutofillService() {
    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        var saveType = SaveInfo.SAVE_DATA_TYPE_GENERIC
        var clientState = request.getClientState()
        if (clientState != null) {
            saveType = clientState.getInt(SAVE_TYPE_KEY, saveType)
        }
        Log.d(TAG, "onFillRequest(): saveType=$saveType")

        // Find autofillable fields
        val structure: AssistStructure = BasicService.Companion.getLatestAssistStructure(request)
        val fields = getAutofillableFields(structure)
        Log.d(TAG, "autofillable fields:$fields")
        if (fields.isEmpty()) {
            toast("No autofill hints found")
            callback.onSuccess(null)
            return
        }
        val ids = fields.values
        val requiredIds = arrayOfNulls<AutofillId?>(ids.size)
        ids.toTypedArray<AutofillId?>()
        for (i in 0 until fields.size) {
            val hint = fields.keyAt(i)
            when (hint) {
                View.AUTOFILL_HINT_USERNAME -> saveType =
                    saveType or SaveInfo.SAVE_DATA_TYPE_USERNAME
                View.AUTOFILL_HINT_EMAIL_ADDRESS -> saveType =
                    saveType or SaveInfo.SAVE_DATA_TYPE_EMAIL_ADDRESS
                View.AUTOFILL_HINT_PASSWORD -> saveType =
                    saveType or SaveInfo.SAVE_DATA_TYPE_PASSWORD
                View.AUTOFILL_HINT_CREDIT_CARD_NUMBER, View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE, View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY, View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH, View.AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR, View.AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE -> saveType =
                    saveType or SaveInfo.SAVE_DATA_TYPE_CREDIT_CARD
                View.AUTOFILL_HINT_POSTAL_ADDRESS, View.AUTOFILL_HINT_POSTAL_CODE -> saveType =
                    saveType or SaveInfo.SAVE_DATA_TYPE_ADDRESS
                else -> Log.d(TAG, "Ignoring hint '$hint'")
            }
        }
        Log.d(TAG, "new saveType=$saveType")
        if (clientState == null) {
            // Initial request
            clientState = Bundle()
        }
        // NOTE: to simplify, we're saving just the saveType, but a real service implementation
        // would have to save the previous values as well, so they can be used later (for example,
        // it would have to save the username in the first request so it's used to save the
        // username + password combo in the second request.
        clientState.putInt(SAVE_TYPE_KEY, saveType)

        // Create response...
        callback.onSuccess(FillResponse.Builder()
            .setClientState(clientState)
            .setSaveInfo(SaveInfo.Builder(saveType, requiredIds).build())
            .build())
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        Log.d(TAG, "onSaveRequest()")
        toast("Save not supported")
        callback.onSuccess()
    }

    private fun getAutofillableFields(structure: AssistStructure): ArrayMap<String?, AutofillId?> {
        val fields = ArrayMap<String?, AutofillId?>()
        val nodes = structure.windowNodeCount
        for (i in 0 until nodes) {
            val node = structure.getWindowNodeAt(i).rootViewNode
            addAutofillableFields(fields, node)
        }
        return fields
    }

    private fun addAutofillableFields(
        fields: MutableMap<String?, AutofillId?>,
        node: ViewNode
    ) {
        val hints = node.autofillHints
        if (hints != null) {
            // We're simple, we only care about the first hint
            val hint = hints[0]
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

    private fun toast(message: CharSequence) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    companion object {
        private val TAG: String? = "MultiStepsService"
        private val SAVE_TYPE_KEY: String? = "saveType"
    }
}