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
import android.content.IntentSender
import android.service.autofill.FillContext
import android.service.autofill.AutofillService
import android.service.autofill.FillRequest
import android.service.autofill.FillCallback
import android.service.autofill.SaveRequest
import android.service.autofill.SaveCallback
import com.google.gson.GsonBuilder
import android.os.*
import com.example.android.autofill.service.data.*
import com.example.android.autofill.service.data.adapter.DatasetAdapter
import com.example.android.autofill.service.data.adapter.ResponseAdapter
import com.example.android.autofill.service.data.source.DefaultFieldTypesSource
import com.example.android.autofill.service.data.source.PackageVerificationDataSource
import com.example.android.autofill.service.data.source.local.DefaultFieldTypesLocalJsonSource
import com.example.android.autofill.service.data.source.local.DigitalAssetLinksRepository
import com.example.android.autofill.service.data.source.local.LocalAutofillDataSource
import com.example.android.autofill.service.data.source.local.SharedPrefsPackageVerificationRepository
import com.example.android.autofill.service.data.source.local.dao.AutofillDao
import com.example.android.autofill.service.data.source.local.db.AutofillDatabase
import com.example.android.autofill.service.model.DalCheck
import com.example.android.autofill.service.model.DalInfo
import com.example.android.autofill.service.model.DatasetWithFilledAutofillFields
import com.example.android.autofill.service.model.FieldTypeWithHeuristics
import com.example.android.autofill.service.settings.MyPreferences
import com.example.android.autofill.service.util.AppExecutors
import com.example.android.autofill.service.util.Util
import java.util.HashMap
import java.util.stream.Collectors

@RequiresApi(api = Build.VERSION_CODES.O)
class MyAutofillService : AutofillService() {
    private var mLocalAutofillDataSource: LocalAutofillDataSource? = null
    private var mDalRepository: DigitalAssetLinksRepository? = null
    private var mPackageVerificationRepository: PackageVerificationDataSource? = null
    private var mAutofillDataBuilder: AutofillDataBuilder? = null
    private var mResponseAdapter: ResponseAdapter? = null
    private var mClientViewMetadata: ClientViewMetadata? = null
    private var mPreferences: MyPreferences? = null
    override fun onCreate() {
        super.onCreate()
        mPreferences = MyPreferences.Companion.getInstance(this)
        Util.setLoggingLevel(mPreferences?.getLoggingLevel())
        val localAfDataSourceSharedPrefs =
            getSharedPreferences(LocalAutofillDataSource.Companion.SHARED_PREF_KEY, MODE_PRIVATE)
        val defaultFieldTypesSource: DefaultFieldTypesLocalJsonSource? =
            DefaultFieldTypesLocalJsonSource.Companion.getInstance(
                resources,
                GsonBuilder().create())
        val autofillDao: AutofillDao? = AutofillDatabase.Companion.getInstance(this,
            defaultFieldTypesSource, AppExecutors())?.autofillDao()
        mLocalAutofillDataSource =
            LocalAutofillDataSource.Companion.getInstance(localAfDataSourceSharedPrefs,
                autofillDao, AppExecutors())
        mDalRepository = DigitalAssetLinksRepository.Companion.getInstance(
            packageManager)
        mPackageVerificationRepository =
            SharedPrefsPackageVerificationRepository.Companion.getInstance(this)
    }

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal, callback: FillCallback
    ) {
        val fillContexts = request.fillContexts
        val structures = fillContexts.stream().map { obj: FillContext? -> obj?.getStructure() }
            .collect(Collectors.toList())
        val latestStructure = fillContexts[fillContexts.size - 1].structure
        val parser = ClientParser(structures)

        // Check user's settings for authenticating Responses and Datasets.
        val responseAuth = mPreferences?.isResponseAuth()
        val datasetAuth = mPreferences?.isDatasetAuth()
        val manual = request.flags and FillRequest.FLAG_MANUAL_REQUEST != 0
        mLocalAutofillDataSource?.getFieldTypeByAutofillHints(
            object : DataCallback<HashMap<String?, FieldTypeWithHeuristics?>?> {
                override fun onLoaded(fieldTypesByAutofillHint: HashMap<String?, FieldTypeWithHeuristics?>?) {
                    val datasetAdapter = DatasetAdapter(parser)
                    val clientViewMetadataBuilder =
                        ClientViewMetadataBuilder(parser, fieldTypesByAutofillHint)
                    mClientViewMetadata = clientViewMetadataBuilder.buildClientViewMetadata()
                    mResponseAdapter = ResponseAdapter(this@MyAutofillService,
                        mClientViewMetadata, packageName, datasetAdapter)
                    val packageName = latestStructure.activityComponent.packageName
                    if (!mPackageVerificationRepository?.putPackageSignatures(packageName)!!) {
                        callback.onFailure(getString(R.string.invalid_package_signature))
                        return
                    }
                    if (Util.logVerboseEnabled()) {
                        Util.logv("onFillRequest(): clientState=%s",
                            Util.bundleToString(request.clientState))
                        Util.dumpStructure(latestStructure)
                    }
                    cancellationSignal.setOnCancelListener { Util.logw("Cancel autofill not implemented in this sample.") }
                    fetchDataAndGenerateResponse(fieldTypesByAutofillHint, responseAuth!!,
                        datasetAuth!!, manual, callback)
                }

                override fun onDataNotAvailable(msg: String?, vararg params: Any?) {}
            })
    }

    private fun fetchDataAndGenerateResponse(
        fieldTypesByAutofillHint: HashMap<String?, FieldTypeWithHeuristics?>?,
        responseAuth: Boolean,
        datasetAuth: Boolean,
        manual: Boolean,
        callback: FillCallback?
    ) {
        if (responseAuth) {
            // If the entire Autofill Response is authenticated, AuthActivity is used
            // to generate Response.
            val sender: IntentSender? = AuthActivity.Companion.getAuthIntentSenderForResponse(this)
            val remoteViews = RemoteViewsHelper.viewsWithAuth(packageName,
                getString(R.string.autofill_sign_in_prompt))
            val response = mResponseAdapter?.buildResponse(sender, remoteViews)
            if (response != null) {
                callback?.onSuccess(response)
            }
        } else {
            mLocalAutofillDataSource?.getAutofillDatasets(mClientViewMetadata?.getAllHints(),
                object : DataCallback<MutableList<DatasetWithFilledAutofillFields?>?> {
                    override fun onLoaded(datasets: MutableList<DatasetWithFilledAutofillFields?>?) {
                        if ((datasets == null || datasets.isEmpty()) && manual) {
                            val sender: IntentSender? =
                                ManualActivity.Companion.getManualIntentSenderForResponse(this@MyAutofillService)
                            val remoteViews = RemoteViewsHelper.viewsWithNoAuth(
                                packageName,
                                getString(R.string.autofill_manual_prompt))
                            val response = mResponseAdapter?.buildManualResponse(sender,
                                remoteViews)
                            if (response != null) {
                                callback?.onSuccess(response)
                            }
                        } else {
                            val response = mResponseAdapter?.buildResponse(
                                fieldTypesByAutofillHint, datasets, datasetAuth)
                            callback?.onSuccess(response)
                        }
                    }

                    override fun onDataNotAvailable(msg: String?, vararg params: Any?) {
                        Util.logw(msg, *params)
                        callback?.onFailure(msg?.let { String.format(it, *params) })
                    }
                })
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        val fillContexts = request.fillContexts
        val structures = fillContexts.stream().map { obj: FillContext? -> obj?.getStructure() }
            .collect(Collectors.toList())
        val latestStructure = fillContexts[fillContexts.size - 1].structure
        val parser = ClientParser(structures)
        mLocalAutofillDataSource?.getFieldTypeByAutofillHints(
            object : DataCallback<HashMap<String?, FieldTypeWithHeuristics?>?> {
                override fun onLoaded(
                    fieldTypesByAutofillHint: HashMap<String?, FieldTypeWithHeuristics?>?
                ) {
                    mAutofillDataBuilder = ClientAutofillDataBuilder(
                        fieldTypesByAutofillHint, packageName, parser)
                    val clientViewMetadataBuilder =
                        ClientViewMetadataBuilder(parser, fieldTypesByAutofillHint)
                    mClientViewMetadata = clientViewMetadataBuilder.buildClientViewMetadata()
                    val packageName = latestStructure.activityComponent.packageName
                    if (!mPackageVerificationRepository?.putPackageSignatures(packageName)!!) {
                        callback.onFailure(getString(R.string.invalid_package_signature))
                        return
                    }
                    if (Util.logVerboseEnabled()) {
                        Util.logv("onSaveRequest(): clientState=%s",
                            Util.bundleToString(request.clientState))
                    }
                    Util.dumpStructure(latestStructure)
                    checkWebDomainAndBuildAutofillData(packageName, callback)
                }

                override fun onDataNotAvailable(msg: String?, vararg params: Any?) {
                    Util.loge("Should not happen - could not find field types.")
                }
            })
    }

    private fun checkWebDomainAndBuildAutofillData(packageName: String?, callback: SaveCallback?) {
        val webDomain: String?
        webDomain = try {
            mClientViewMetadata?.getWebDomain()
        } catch (e: SecurityException) {
            Util.logw(e.message)
            callback?.onFailure(getString(R.string.security_exception))
            return
        }
        if (webDomain != null && webDomain.length > 0) {
            val req = mPreferences?.getDalCheckRequirement()
            mDalRepository?.checkValid(req, DalInfo(webDomain, packageName),
                object : DataCallback<DalCheck?> {
                    override fun onLoaded(dalCheck: DalCheck?) {
                        if (dalCheck != null) {
                            if (dalCheck.linked) {
                                Util.logd("Domain %s is valid for %s", webDomain, packageName)
                                buildAndSaveAutofillData()
                            } else {
                                Util.loge("Could not associate web domain %s with app %s",
                                    webDomain, packageName)
                                callback?.onFailure(getString(R.string.dal_exception))
                            }
                        }
                    }

                    override fun onDataNotAvailable(msg: String?, vararg params: Any?) {
                        Util.logw(msg, *params)
                        callback?.onFailure(getString(R.string.dal_exception))
                    }
                })
        } else {
            Util.logd("no web domain")
            buildAndSaveAutofillData()
        }
    }

    private fun buildAndSaveAutofillData() {
        val datasetNumber = mLocalAutofillDataSource?.getDatasetNumber()
        val datasetsWithFilledAutofillFields =
            datasetNumber?.let { mAutofillDataBuilder?.buildDatasetsByPartition(it) }
        mLocalAutofillDataSource?.saveAutofillDatasets(datasetsWithFilledAutofillFields)
    }

    override fun onConnected() {
        Util.logd("onConnected")
    }

    override fun onDisconnected() {
        Util.logd("onDisconnected")
    }
}