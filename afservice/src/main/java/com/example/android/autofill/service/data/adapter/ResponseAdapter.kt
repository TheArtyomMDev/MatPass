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

import android.widget.RemoteViews
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import com.example.android.autofill.service.RemoteViewsHelper
import com.example.android.autofill.service.AuthActivity
import android.service.autofill.SaveInfo
import android.content.*
import com.example.android.autofill.service.data.ClientViewMetadata
import com.example.android.autofill.service.model.DatasetWithFilledAutofillFields
import com.example.android.autofill.service.model.FieldType
import com.example.android.autofill.service.model.FieldTypeWithHeuristics
import com.example.android.autofill.service.model.FilledAutofillField
import java.util.HashMap

class ResponseAdapter(
    private val mContext: Context?, private val mClientViewMetadata: ClientViewMetadata?,
    private val mPackageName: String?, private val mDatasetAdapter: DatasetAdapter?
) {
    fun buildResponseForFocusedNode(
        datasetName: String?, field: FilledAutofillField?,
        fieldType: FieldType?
    ): FillResponse? {
        val responseBuilder = FillResponse.Builder()
        val remoteViews = RemoteViewsHelper.viewsWithNoAuth(
            mPackageName, datasetName)
        val dataset = mDatasetAdapter!!.buildDatasetForFocusedNode(field, fieldType, remoteViews)
        return if (dataset != null) {
            responseBuilder.addDataset(dataset)
            responseBuilder.build()
        } else {
            null
        }
    }

    /**
     * Wraps autofill data in a Response object (essentially a series of Datasets) which can then
     * be sent back to the client View.
     */
    fun buildResponse(
        fieldTypesByAutofillHint: HashMap<String?, FieldTypeWithHeuristics?>?,
        datasets: MutableList<DatasetWithFilledAutofillFields?>?, datasetAuth: Boolean
    ): FillResponse? {
        val responseBuilder = FillResponse.Builder()
        if (datasets != null) {
            for (datasetWithFilledAutofillFields in datasets) {
                if (datasetWithFilledAutofillFields != null) {
                    var dataset: Dataset?
                    val datasetName = datasetWithFilledAutofillFields.autofillDataset
                        ?.getDatasetName()
                    dataset = if (datasetAuth) {
                        val intentSender: IntentSender? =
                            AuthActivity.Companion.getAuthIntentSenderForDataset(
                                mContext, datasetName)
                        val remoteViews = RemoteViewsHelper.viewsWithAuth(
                            mPackageName, datasetName)
                        mDatasetAdapter!!.buildDataset(fieldTypesByAutofillHint,
                            datasetWithFilledAutofillFields, remoteViews, intentSender)
                    } else {
                        val remoteViews = RemoteViewsHelper.viewsWithNoAuth(
                            mPackageName, datasetName)
                        mDatasetAdapter!!.buildDataset(fieldTypesByAutofillHint,
                            datasetWithFilledAutofillFields, remoteViews)
                    }
                    if (dataset != null) {
                        responseBuilder.addDataset(dataset)
                    }
                }
            }
        }
        val saveType = mClientViewMetadata!!.getSaveType()
        val autofillIds = mClientViewMetadata.getAutofillIds()
        return if (autofillIds != null && autofillIds.size > 0) {
            val saveInfo = SaveInfo.Builder(saveType, autofillIds).build()
            responseBuilder.setSaveInfo(saveInfo)
            responseBuilder.build()
        } else {
            null
        }
    }

    fun buildResponse(sender: IntentSender?, remoteViews: RemoteViews?): FillResponse? {
        val responseBuilder = FillResponse.Builder()
        val saveType = mClientViewMetadata!!.getSaveType()
        val autofillIds = mClientViewMetadata.getAutofillIds()
        return if (autofillIds != null && autofillIds.size > 0) {
            val saveInfo = SaveInfo.Builder(saveType, autofillIds).build()
            responseBuilder.setSaveInfo(saveInfo)
            responseBuilder.setAuthentication(autofillIds, sender, remoteViews)
            responseBuilder.build()
        } else {
            null
        }
    }

    fun buildManualResponse(sender: IntentSender?, remoteViews: RemoteViews?): FillResponse? {
        val responseBuilder = FillResponse.Builder()
        val saveType = mClientViewMetadata!!.getSaveType()
        val focusedIds = mClientViewMetadata.getFocusedIds()
        return if (focusedIds != null && focusedIds.size > 0) {
            val saveInfo = SaveInfo.Builder(saveType, focusedIds).build()
            responseBuilder.setSaveInfo(saveInfo)
                .setAuthentication(focusedIds, sender, remoteViews)
                .build()
        } else {
            null
        }
    }
}