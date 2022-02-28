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
package com.example.android.autofill.service

import androidx.annotation.RequiresApi
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.os.Bundle
import android.app.assist.AssistStructure
import android.app.Activity
import android.view.autofill.AutofillManager
import android.app.PendingIntent
import android.content.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.android.autofill.service.data.ClientViewMetadata
import com.example.android.autofill.service.data.ClientViewMetadataBuilder
import com.example.android.autofill.service.data.DataCallback
import com.example.android.autofill.service.data.adapter.DatasetAdapter
import com.example.android.autofill.service.data.adapter.ResponseAdapter
import com.example.android.autofill.service.data.source.DefaultFieldTypesSource
import com.example.android.autofill.service.data.source.local.DefaultFieldTypesLocalJsonSource
import com.example.android.autofill.service.data.source.local.LocalAutofillDataSource
import com.example.android.autofill.service.data.source.local.dao.AutofillDao
import com.example.android.autofill.service.data.source.local.db.AutofillDatabase
import com.example.android.autofill.service.model.*
import com.example.android.autofill.service.settings.MyPreferences
import com.example.android.autofill.service.util.AppExecutors
import com.example.android.autofill.service.util.Util
import com.google.common.collect.ImmutableList
import java.util.*

/**
 * When the user long-presses on an autofillable field and selects "Autofill", this activity is
 * launched to allow the user to select the dataset.
 */
class ManualActivity : AppCompatActivity() {
    private var mLocalAutofillDataSource: LocalAutofillDataSource? = null
    private var mDatasetAdapter: DatasetAdapter? = null
    private var mResponseAdapter: ResponseAdapter? = null
    private var mClientViewMetadata: ClientViewMetadata? = null
    private var mPackageName: String? = null
    private var mReplyIntent: Intent? = null
    private var mPreferences: MyPreferences? = null
    private var mAllDatasets: MutableList<DatasetWithFilledAutofillFields?>? = null
    private var mRecyclerView: RecyclerView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.multidataset_service_manual_activity)
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
        mPackageName = packageName
        mPreferences = MyPreferences.Companion.getInstance(this)
        mRecyclerView = findViewById(R.id.suggestionsList)
        mRecyclerView?.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        mLocalAutofillDataSource?.getAllAutofillDatasets(
            object : DataCallback<MutableList<DatasetWithFilledAutofillFields?>?> {
                override fun onLoaded(datasets: MutableList<DatasetWithFilledAutofillFields?>?) {
                    mAllDatasets = datasets
                    buildAdapter()
                }

                override fun onDataNotAvailable(msg: String?, vararg params: Any?) {}
            })
    }

    private fun buildAdapter() {
        val datasetIds: MutableList<String?> = ArrayList()
        val datasetNames: MutableList<String?> = ArrayList()
        val allFieldTypes: MutableList<MutableList<String?>?> = ArrayList()
        for (dataset in mAllDatasets!!) {
            val datasetName: String = dataset?.autofillDataset!!.getDatasetName()
            val datasetId: String = dataset?.autofillDataset!!.getId()
            val fieldTypes: MutableList<String?> = ArrayList()
            for (filledAutofillField in dataset?.filledAutofillFields!!) {
                fieldTypes.add(filledAutofillField?.getFieldTypeName())
            }
            datasetIds.add(datasetId)
            datasetNames.add(datasetName)
            allFieldTypes.add(fieldTypes)
        }
        val adapter = AutofillDatasetsAdapter(datasetIds, datasetNames,
            allFieldTypes, this)
        mRecyclerView?.setAdapter(adapter)
    }

    override fun finish() {
        if (mReplyIntent != null) {
            setResult(RESULT_OK, mReplyIntent)
        } else {
            setResult(RESULT_CANCELED)
        }
        super.finish()
    }

    private fun onFieldSelected(field: FilledAutofillField?, fieldType: FieldType?) {
        val datasetWithFilledAutofillFields = DatasetWithFilledAutofillFields()
        val newDatasetId = UUID.randomUUID().toString()
        val copyOfField = FilledAutofillField(newDatasetId,
            field!!.getFieldTypeName(), field.getTextValue(), field.getDateValue(),
            field.getToggleValue())
        val datasetName = "dataset-manual"
        val autofillDataset = mPackageName?.let { AutofillDataset(newDatasetId, datasetName, it) }
        datasetWithFilledAutofillFields.filledAutofillFields = ImmutableList.of(copyOfField)
        datasetWithFilledAutofillFields.autofillDataset = autofillDataset
        val intent = intent
        val structure =
            intent.getParcelableExtra<AssistStructure?>(AutofillManager.EXTRA_ASSIST_STRUCTURE)
        val clientParser = structure?.let { ClientParser(it) }
        mReplyIntent = Intent()
        mLocalAutofillDataSource?.getFieldTypeByAutofillHints(
            object : DataCallback<HashMap<String?, FieldTypeWithHeuristics?>?> {
                @RequiresApi(api = Build.VERSION_CODES.O)
                override fun onLoaded(fieldTypesByAutofillHint: HashMap<String?, FieldTypeWithHeuristics?>?) {
                    val builder = ClientViewMetadataBuilder(clientParser,
                        fieldTypesByAutofillHint)
                    mClientViewMetadata = builder.buildClientViewMetadata()
                    mDatasetAdapter = DatasetAdapter(clientParser)
                    mResponseAdapter = ResponseAdapter(this@ManualActivity,
                        mClientViewMetadata, mPackageName, mDatasetAdapter)
                    val fillResponse = mResponseAdapter!!.buildResponseForFocusedNode(
                        datasetName, field, fieldType)
                    setResponseIntent(fillResponse)
                    finish()
                }

                override fun onDataNotAvailable(msg: String?, vararg params: Any?) {}
            })
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != RC_SELECT_FIELD || resultCode != RESULT_OK) {
            Util.logd("Ignoring requestCode == %d | resultCode == %d", requestCode,
                resultCode)
            return
        }
        val datasetId =
            data?.getStringExtra(ManualFieldPickerActivity.Companion.EXTRA_SELECTED_FIELD_DATASET_ID)
        val fieldTypeName =
            data?.getStringExtra(ManualFieldPickerActivity.Companion.EXTRA_SELECTED_FIELD_TYPE_NAME)
        mLocalAutofillDataSource?.getFilledAutofillField(datasetId,
            fieldTypeName,
            object : DataCallback<FilledAutofillField?> {
                override fun onLoaded(field: FilledAutofillField?) {
                    mLocalAutofillDataSource!!.getFieldType(field?.getFieldTypeName(),
                        object : DataCallback<FieldType?> {
                            override fun onLoaded(fieldType: FieldType?) {
                                onFieldSelected(field, fieldType)
                            }

                            override fun onDataNotAvailable(msg: String?, vararg params: Any?) {}
                        })
                }

                override fun onDataNotAvailable(msg: String?, vararg params: Any?) {}
            })
    }

    private fun updateHeuristics() {
//        TODO: update heuristics in data source; something like:
//        mLocalAutofillDataSource.getAutofillDataset(mClientViewMetadata.getAllHints(),
//                datasetName, new DataCallback<DatasetWithFilledAutofillFields>() {
//                    @Override
//                    public void onLoaded(DatasetWithFilledAutofillFields dataset) {
//                        String datasetName = dataset.autofillDataset.getDatasetName();
//                        RemoteViews remoteViews = RemoteViewsHelper.viewsWithNoAuth(
//                                mPackageName, datasetName);
//                        setDatasetIntent(mDatasetAdapter.buildDataset(fieldTypesByAutofillHint,
//                                dataset, remoteViews));
//                        finish();
//                    }
//
//                    @Override
//                    public void onDataNotAvailable(String msg, Object... params) {
//                        logw(msg, params);
//                        finish();
//                    }
//                });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun setResponseIntent(fillResponse: FillResponse?) {
        mReplyIntent?.putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, fillResponse)
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun setDatasetIntent(dataset: Dataset?) {
        mReplyIntent?.putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset)
    }

    /**
     * Adapter for the [RecyclerView] that holds a list of datasets.
     */
    private class AutofillDatasetsAdapter(
        private val mDatasetIds: MutableList<String?>?,
        private val mDatasetNames: MutableList<String?>?,
        private val mFieldTypes: MutableList<MutableList<String?>?>?,
        private val mActivity: Activity?
    ) : RecyclerView.Adapter<DatasetViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DatasetViewHolder {
            return DatasetViewHolder.newInstance(parent, mActivity)!!
        }

        override fun onBindViewHolder(holder: DatasetViewHolder, position: Int) {
            holder.bind(mDatasetIds?.get(position), mDatasetNames?.get(position),
                mFieldTypes?.get(position))
        }

        override fun getItemCount(): Int {
            return mDatasetNames!!.size
        }
    }

    /**
     * Contains views needed in each row of the list of datasets.
     */
    private class DatasetViewHolder(private val mRootView: View?, activity: Activity?) :
        RecyclerView.ViewHolder(
            mRootView!!) {
        private val mDatasetNameText: TextView?
        private val mFieldTypesText: TextView?
        private val mActivity: Activity?
        fun bind(datasetId: String?, datasetName: String?, fieldTypes: MutableList<String?>?) {
            mDatasetNameText?.setText(datasetName)
            var firstFieldType: String? = null
            var secondFieldType: String? = null
            var numOfFieldTypes = 0
            if (fieldTypes != null) {
                numOfFieldTypes = fieldTypes.size
                if (numOfFieldTypes > 0) {
                    firstFieldType = fieldTypes[0]
                }
                if (numOfFieldTypes > 1) {
                    secondFieldType = fieldTypes[1]
                }
            }
            val fieldTypesString: String
            fieldTypesString = if (numOfFieldTypes == 1) {
                "Contains data for $firstFieldType."
            } else if (numOfFieldTypes == 2) {
                "Contains data for $firstFieldType and $secondFieldType."
            } else if (numOfFieldTypes > 2) {
                "Contains data for $firstFieldType, $secondFieldType, and more."
            } else {
                "Ignore: Contains no data."
            }
            mFieldTypesText?.setText(fieldTypesString)
            mRootView?.setOnClickListener(View.OnClickListener { view: View? ->
                val intent: Intent? =
                    ManualFieldPickerActivity.Companion.getIntent(mActivity, datasetId)
                mActivity?.startActivityForResult(intent, RC_SELECT_FIELD)
            })
        }

        companion object {
            fun newInstance(parent: ViewGroup?, activity: Activity?): DatasetViewHolder? {
                return DatasetViewHolder(LayoutInflater.from(parent?.getContext())
                    .inflate(R.layout.dataset_suggestion, parent, false), activity)
            }
        }

        init {
            mDatasetNameText = itemView.findViewById(R.id.datasetName)
            mFieldTypesText = itemView.findViewById(R.id.fieldTypes)
            mActivity = activity
        }
    }

    companion object {
        private const val RC_SELECT_FIELD = 1

        // Unique id for dataset intents.
        private const val sDatasetPendingIntentId = 0
        fun getManualIntentSenderForResponse(context: Context?): IntentSender? {
            val intent = Intent(context, ManualActivity::class.java)
            return PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_CANCEL_CURRENT).intentSender
        }
    }
}