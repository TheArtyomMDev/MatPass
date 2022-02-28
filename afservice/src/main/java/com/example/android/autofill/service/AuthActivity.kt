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
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.os.Bundle
import android.app.assist.AssistStructure
import android.widget.Toast
import android.view.autofill.AutofillManager
import android.app.PendingIntent
import android.content.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import android.widget.EditText
import android.view.View
import com.example.android.autofill.service.data.ClientViewMetadata
import com.example.android.autofill.service.data.ClientViewMetadataBuilder
import com.example.android.autofill.service.data.DataCallback
import com.example.android.autofill.service.data.adapter.DatasetAdapter
import com.example.android.autofill.service.data.adapter.ResponseAdapter
import com.example.android.autofill.service.data.source.DefaultFieldTypesSource
import com.example.android.autofill.service.data.source.local.DefaultFieldTypesLocalJsonSource
import com.example.android.autofill.service.data.source.local.DigitalAssetLinksRepository
import com.example.android.autofill.service.data.source.local.LocalAutofillDataSource
import com.example.android.autofill.service.data.source.local.dao.AutofillDao
import com.example.android.autofill.service.data.source.local.db.AutofillDatabase
import com.example.android.autofill.service.model.DatasetWithFilledAutofillFields
import com.example.android.autofill.service.model.FieldTypeWithHeuristics
import com.example.android.autofill.service.settings.MyPreferences
import com.example.android.autofill.service.util.AppExecutors
import com.example.android.autofill.service.util.Util
import java.util.HashMap

/**
 * This Activity controls the UI for logging in to the Autofill service.
 * It is launched when an Autofill Response or specific Dataset within the Response requires
 * authentication to access. It bundles the result in an Intent.
 */
class AuthActivity : AppCompatActivity() {
    private var mLocalAutofillDataSource: LocalAutofillDataSource? = null
    private var mDalRepository: DigitalAssetLinksRepository? = null
    private var mMasterPassword: EditText? = null
    private var mDatasetAdapter: DatasetAdapter? = null
    private var mResponseAdapter: ResponseAdapter? = null
    private var mClientViewMetadata: ClientViewMetadata? = null
    private var mPackageName: String? = null
    private var mReplyIntent: Intent? = null
    private var mPreferences: MyPreferences? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.multidataset_service_auth_activity)
        val sharedPreferences =
            getSharedPreferences(LocalAutofillDataSource.Companion.SHARED_PREF_KEY, MODE_PRIVATE)
        val defaultFieldTypesSource: DefaultFieldTypesLocalJsonSource? =
            DefaultFieldTypesLocalJsonSource.Companion.getInstance(
                resources,
                GsonBuilder().create())
        val autofillDao: AutofillDao? = AutofillDatabase.Companion.getInstance(this,
            defaultFieldTypesSource, AppExecutors())?.autofillDao()
        mLocalAutofillDataSource = LocalAutofillDataSource.Companion.getInstance(sharedPreferences,
            autofillDao, AppExecutors())
        mDalRepository = DigitalAssetLinksRepository.Companion.getInstance(
            packageManager)
        mMasterPassword = findViewById(R.id.master_password)
        mPackageName = packageName
        mPreferences = MyPreferences.Companion.getInstance(this)
        findViewById<View?>(R.id.login).setOnClickListener { login() }
        findViewById<View?>(R.id.cancel).setOnClickListener {
            onFailure()
            finish()
        }
    }

    private fun login() {
        val password = mMasterPassword?.text
        val correctPassword: String? =
            MyPreferences.Companion.getInstance(this@AuthActivity)?.getMasterPassword()
        if (password.toString() == correctPassword) {
            onSuccess()
        } else {
            Toast.makeText(this, "Password incorrect", Toast.LENGTH_SHORT).show()
            onFailure()
        }
    }

    override fun finish() {
        if (mReplyIntent != null) {
            setResult(RESULT_OK, mReplyIntent)
        } else {
            setResult(RESULT_CANCELED)
        }
        super.finish()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun onFailure() {
        Util.logw("Failed auth.")
        mReplyIntent = null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun onSuccess() {
        val intent = intent
        val forResponse = intent.getBooleanExtra(Util.EXTRA_FOR_RESPONSE, true)
        val structure =
            intent.getParcelableExtra<AssistStructure?>(AutofillManager.EXTRA_ASSIST_STRUCTURE)
        val clientParser = structure?.let { ClientParser(it) }
        mReplyIntent = Intent()
        mLocalAutofillDataSource?.getFieldTypeByAutofillHints(
            object : DataCallback<HashMap<String?, FieldTypeWithHeuristics?>?> {
                override fun onLoaded(fieldTypesByAutofillHint: HashMap<String?, FieldTypeWithHeuristics?>?) {
                    val builder = ClientViewMetadataBuilder(clientParser,
                        fieldTypesByAutofillHint)
                    mClientViewMetadata = builder.buildClientViewMetadata()
                    mDatasetAdapter = DatasetAdapter(clientParser)
                    mResponseAdapter = ResponseAdapter(this@AuthActivity,
                        mClientViewMetadata, mPackageName, mDatasetAdapter)
                    if (forResponse) {
                        fetchAllDatasetsAndSetIntent(fieldTypesByAutofillHint)
                    } else {
                        val datasetName = intent.getStringExtra(Util.EXTRA_DATASET_NAME)
                        fetchDatasetAndSetIntent(fieldTypesByAutofillHint, datasetName)
                    }
                }

                override fun onDataNotAvailable(msg: String?, vararg params: Any?) {}
            })
    }

    private fun fetchDatasetAndSetIntent(
        fieldTypesByAutofillHint: HashMap<String?, FieldTypeWithHeuristics?>?, datasetName: String?
    ) {
        mLocalAutofillDataSource?.getAutofillDataset(mClientViewMetadata?.getAllHints(),
            datasetName, object : DataCallback<DatasetWithFilledAutofillFields?> {
                override fun onLoaded(dataset: DatasetWithFilledAutofillFields?) {
                    val datasetName = dataset?.autofillDataset?.getDatasetName()
                    val remoteViews = RemoteViewsHelper.viewsWithNoAuth(
                        mPackageName, datasetName)
                    setDatasetIntent(mDatasetAdapter?.buildDataset(fieldTypesByAutofillHint,
                        dataset, remoteViews))
                    finish()
                }

                override fun onDataNotAvailable(msg: String?, vararg params: Any?) {
                    Util.logw(msg, *params)
                    finish()
                }
            })
    }

    private fun fetchAllDatasetsAndSetIntent(
        fieldTypesByAutofillHint: HashMap<String?, FieldTypeWithHeuristics?>?
    ) {
        mLocalAutofillDataSource?.getAutofillDatasets(mClientViewMetadata?.getAllHints(),
            object : DataCallback<MutableList<DatasetWithFilledAutofillFields?>?> {
                override fun onLoaded(datasets: MutableList<DatasetWithFilledAutofillFields?>?) {
                    val datasetAuth = mPreferences?.isDatasetAuth()
                    val fillResponse = datasetAuth?.let {
                        mResponseAdapter?.buildResponse(
                            fieldTypesByAutofillHint, datasets, it)
                    }
                    setResponseIntent(fillResponse)
                    finish()
                }

                override fun onDataNotAvailable(msg: String?, vararg params: Any?) {
                    Util.logw(msg, *params)
                    finish()
                }
            })
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun setResponseIntent(fillResponse: FillResponse?) {
        mReplyIntent?.putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, fillResponse)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun setDatasetIntent(dataset: Dataset?) {
        mReplyIntent?.putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset)
    }

    companion object {
        // Unique id for dataset intents.
        private var sDatasetPendingIntentId = 0
        fun getAuthIntentSenderForResponse(context: Context?): IntentSender? {
            val intent = Intent(context, AuthActivity::class.java)
            return PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT).intentSender
        }

        fun getAuthIntentSenderForDataset(
            originContext: Context?,
            datasetName: String?
        ): IntentSender? {
            val intent = Intent(originContext, AuthActivity::class.java)
            intent.putExtra(Util.EXTRA_DATASET_NAME, datasetName)
            intent.putExtra(Util.EXTRA_FOR_RESPONSE, false)
            return PendingIntent.getActivity(originContext, ++sDatasetPendingIntentId, intent,
                PendingIntent.FLAG_CANCEL_CURRENT).intentSender
        }
    }
}