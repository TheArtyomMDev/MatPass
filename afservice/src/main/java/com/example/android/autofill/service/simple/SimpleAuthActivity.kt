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
import android.os.Build
import com.example.android.autofill.service.R
import android.service.autofill.Dataset
import android.view.autofill.AutofillId
import android.service.autofill.FillResponse
import android.os.Bundle
import android.app.Activity
import android.view.autofill.AutofillManager
import android.app.PendingIntent
import android.content.*
import android.util.ArrayMap
import android.view.View
/**
 * Activity used for autofill authentication, it simply sets the dataste upon tapping OK.
 */
// TODO(b/114236837): should display a small dialog, not take the full screen
class SimpleAuthActivity : Activity() {
    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.simple_service_auth_activity)
        findViewById<View?>(R.id.yes).setOnClickListener { onYes() }
        findViewById<View?>(R.id.no).setOnClickListener { onNo() }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun onYes() {
        val myIntent = intent
        val replyIntent = Intent()
        val dataset = myIntent.getParcelableExtra<Dataset?>(EXTRA_DATASET)
        if (dataset != null) {
            replyIntent.putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset)
        } else {
            val hints = myIntent.getStringArrayExtra(EXTRA_HINTS)
            val ids = myIntent.getParcelableArrayExtra(EXTRA_IDS)
            val authenticateDatasets = myIntent.getBooleanExtra(EXTRA_AUTH_DATASETS, false)
            val size = hints!!.size
            val fields = ArrayMap<String?, AutofillId?>(size)
            for (i in 0 until size) {
                fields[hints!!.get(i)] = ids!!.get(i) as AutofillId
            }
            val response: FillResponse? =
                DebugService.Companion.createResponse(this, fields, 1, authenticateDatasets)
            replyIntent.putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, response)
        }
        setResult(RESULT_OK, replyIntent)
        finish()
    }

    private fun onNo() {
        setResult(RESULT_CANCELED)
        finish()
    }

    companion object {
        private val EXTRA_DATASET: String? = "dataset"
        private val EXTRA_HINTS: String? = "hints"
        private val EXTRA_IDS: String? = "ids"
        private val EXTRA_AUTH_DATASETS: String? = "auth_datasets"
        private var sPendingIntentId = 0
        fun newIntentSenderForDataset(
            context: Context,
            dataset: Dataset
        ): IntentSender? {
            return newIntentSender(context, dataset, null, null, false)
        }

        fun newIntentSenderForResponse(
            context: Context,
            hints: Array<String?>, ids: Array<AutofillId?>, authenticateDatasets: Boolean
        ): IntentSender? {
            return newIntentSender(context, null, hints, ids, authenticateDatasets)
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        private fun newIntentSender(
            context: Context,
            dataset: Dataset?, hints: Array<String?>?, ids: Array<AutofillId?>?,
            authenticateDatasets: Boolean
        ): IntentSender? {
            val intent = Intent(context, SimpleAuthActivity::class.java)
            if (dataset != null) {
                intent.putExtra(EXTRA_DATASET, dataset)
            } else {
                intent.putExtra(EXTRA_HINTS, hints)
                intent.putExtra(EXTRA_IDS, ids)
                intent.putExtra(EXTRA_AUTH_DATASETS, authenticateDatasets)
            }
            return PendingIntent.getActivity(context, ++sPendingIntentId, intent,
                PendingIntent.FLAG_CANCEL_CURRENT).intentSender
        }
    }
}