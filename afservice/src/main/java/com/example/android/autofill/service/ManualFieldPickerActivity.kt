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

import android.os.Bundle
import android.app.Activity
import android.content.*
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.android.autofill.service.data.DataCallback
import com.example.android.autofill.service.data.source.DefaultFieldTypesSource
import com.example.android.autofill.service.data.source.local.DefaultFieldTypesLocalJsonSource
import com.example.android.autofill.service.data.source.local.LocalAutofillDataSource
import com.example.android.autofill.service.data.source.local.dao.AutofillDao
import com.example.android.autofill.service.data.source.local.db.AutofillDatabase
import com.example.android.autofill.service.model.DatasetWithFilledAutofillFields
import com.example.android.autofill.service.model.FilledAutofillField
import com.example.android.autofill.service.util.AppExecutors

class ManualFieldPickerActivity : AppCompatActivity() {
    private var mLocalAutofillDataSource: LocalAutofillDataSource? = null
    private var mRecyclerView: RecyclerView? = null
    private var mListTitle: TextView? = null
    private var mDataset: DatasetWithFilledAutofillFields? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field_picker)
        val sharedPreferences = getSharedPreferences(
            LocalAutofillDataSource.Companion.SHARED_PREF_KEY, MODE_PRIVATE)
        val defaultFieldTypesSource: DefaultFieldTypesLocalJsonSource? =
            DefaultFieldTypesLocalJsonSource.Companion.getInstance(
                resources,
                GsonBuilder().create())
        val autofillDao: AutofillDao? = AutofillDatabase.Companion.getInstance(this,
            defaultFieldTypesSource, AppExecutors())?.autofillDao()
        val datasetId = intent.getStringExtra(EXTRA_DATASET_ID)
        mRecyclerView = findViewById(R.id.fieldsList)
        mRecyclerView?.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        mListTitle = findViewById(R.id.listTitle)
        mLocalAutofillDataSource = LocalAutofillDataSource.Companion.getInstance(sharedPreferences,
            autofillDao, AppExecutors())
        mLocalAutofillDataSource?.getAutofillDatasetWithId(datasetId,
            object : DataCallback<DatasetWithFilledAutofillFields?> {
                override fun onLoaded(dataset: DatasetWithFilledAutofillFields?) {
                    mDataset = dataset
                    if (mDataset != null) {
                        onLoadedDataset()
                    }
                }

                override fun onDataNotAvailable(msg: String?, vararg params: Any?) {}
            })
    }

    fun onSelectedDataset(field: FilledAutofillField?) {
        val data = Intent()
            .putExtra(EXTRA_SELECTED_FIELD_DATASET_ID, field?.getDatasetId())
            .putExtra(EXTRA_SELECTED_FIELD_TYPE_NAME, field?.getFieldTypeName())
        setResult(RESULT_OK, data)
        finish()
    }

    fun onLoadedDataset() {
        val fieldsAdapter = FieldsAdapter(this, mDataset?.filledAutofillFields)
        mRecyclerView?.adapter = fieldsAdapter
        mListTitle?.text = getString(R.string.manual_data_picker_title,
            mDataset?.autofillDataset?.getDatasetName())
    }

    class FieldsAdapter(
        private val mActivity: Activity?,
        private val mFields: MutableList<FilledAutofillField?>?
    ) : RecyclerView.Adapter<FieldViewHolder?>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FieldViewHolder {
            return FieldViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.dataset_field, parent, false), mActivity)
        }

        override fun onBindViewHolder(holder: FieldViewHolder, position: Int) {
            val field = mFields!!.get(position)
            holder!!.bind(field)
        }

        override fun getItemCount(): Int {
            return mFields!!.size
        }
    }

    class FieldViewHolder(private val mRootView: View?, activity: Activity?) :
        RecyclerView.ViewHolder(
            mRootView!!) {
        private val mFieldTypeText: TextView?
        private val mActivity: Activity?
        fun bind(field: FilledAutofillField?) {
            mFieldTypeText?.text = field!!.getFieldTypeName()
            mRootView?.setOnClickListener(View.OnClickListener { view: View? ->
                (mActivity as ManualFieldPickerActivity?)?.onSelectedDataset(field)
            })
        }

        init {
            mFieldTypeText = itemView.findViewById(R.id.fieldType)
            mActivity = activity
        }
    }

    companion object {
        private val EXTRA_DATASET_ID: String? = "extra_dataset_id"
        val EXTRA_SELECTED_FIELD_DATASET_ID: String? = "selected_field_dataset_id"
        val EXTRA_SELECTED_FIELD_TYPE_NAME: String? = "selected_field_type_name"
        fun getIntent(originContext: Context?, datasetId: String?): Intent? {
            val intent = Intent(originContext, ManualFieldPickerActivity::class.java)
            intent.putExtra(EXTRA_DATASET_ID, datasetId)
            return intent
        }
    }
}