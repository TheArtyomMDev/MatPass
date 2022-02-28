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
package com.example.android.autofill.service.data.source.local

import android.content.SharedPreferences
import android.service.autofill.Dataset
import com.example.android.autofill.service.data.DataCallback
import com.example.android.autofill.service.data.source.AutofillDataSource
import com.example.android.autofill.service.data.source.local.dao.AutofillDao
import com.example.android.autofill.service.model.*
import com.example.android.autofill.service.util.AppExecutors
import com.example.android.autofill.service.util.Util
import java.util.HashMap
import java.util.stream.Collectors

class LocalAutofillDataSource private constructor(
    private val mSharedPreferences: SharedPreferences?, private val mAutofillDao: AutofillDao,
    private val mAppExecutors: AppExecutors?
) : AutofillDataSource {
    override fun getAutofillDatasets(
        allAutofillHints: MutableList<String?>?,
        datasetsCallback: DataCallback<MutableList<DatasetWithFilledAutofillFields?>?>?
    ) {
        mAppExecutors!!.diskIO()!!.execute {
            val typeNames = getFieldTypesForAutofillHints(allAutofillHints)
                ?.stream()!!
                .map { obj: FieldTypeWithHeuristics? -> obj!!.getFieldType() }
                .map { obj: FieldType? -> obj!!.getTypeName() }
                .collect(Collectors.toList())
            println("---$typeNames")
            println("!!!!   ${mAutofillDao.getDatasets(typeNames)}")
            val datasetsWithFilledAutofillFields = mAutofillDao.getDatasets(typeNames)

            mAppExecutors.mainThread()
                ?.execute { datasetsCallback!!.onLoaded(datasetsWithFilledAutofillFields) }
        }
    }

    override fun getAllAutofillDatasets(
        datasetsCallback: DataCallback<MutableList<DatasetWithFilledAutofillFields?>?>?
    ) {
        mAppExecutors!!.diskIO()!!.execute {
            val datasetsWithFilledAutofillFields = mAutofillDao!!.getAllDatasets()
            mAppExecutors.mainThread()
                ?.execute { datasetsCallback!!.onLoaded(datasetsWithFilledAutofillFields) }
        }
    }

    override fun getAutofillDataset(
        allAutofillHints: MutableList<String?>?, datasetName: String?,
        datasetsCallback: DataCallback<DatasetWithFilledAutofillFields?>?
    ) {
        mAppExecutors!!.diskIO()!!.execute {

            // Room does not support TypeConverters for collections.
            val autofillDatasetFields =
                mAutofillDao!!.getDatasetsWithName(allAutofillHints, datasetName)
            if (autofillDatasetFields != null && !autofillDatasetFields.isEmpty()) {
                if (autofillDatasetFields.size > 1) {
                    Util.logw("More than 1 dataset with name %s", datasetName)
                }
                val dataset = autofillDatasetFields[0]
                mAppExecutors.mainThread()!!.execute { datasetsCallback!!.onLoaded(dataset) }
            } else {
                mAppExecutors.mainThread()
                    ?.execute { datasetsCallback!!.onDataNotAvailable("No data found.") }
            }
        }
    }

    override fun saveAutofillDatasets(datasetsWithFilledAutofillFields: MutableList<DatasetWithFilledAutofillFields?>?) {
        mAppExecutors!!.diskIO()!!.execute {
            for (datasetWithFilledAutofillFields in datasetsWithFilledAutofillFields!!) {
                val filledAutofillFields = datasetWithFilledAutofillFields!!.filledAutofillFields
                val autofillDataset = datasetWithFilledAutofillFields.autofillDataset
                mAutofillDao!!.insertAutofillDataset(autofillDataset)
                mAutofillDao.insertFilledAutofillFields(filledAutofillFields)
            }
        }
        incrementDatasetNumber()
    }

    override fun saveResourceIdHeuristic(resourceIdHeuristic: ResourceIdHeuristic?) {
        mAppExecutors!!.diskIO()
            ?.execute { mAutofillDao.insertResourceIdHeuristic(resourceIdHeuristic) }
    }

    override fun getFieldTypes(fieldTypesCallback: DataCallback<MutableList<FieldTypeWithHeuristics?>?>?) {
        mAppExecutors!!.diskIO()!!.execute {
            val fieldTypeWithHints = mAutofillDao.getFieldTypesWithHints()
            mAppExecutors.mainThread()!!.execute {
                if (fieldTypeWithHints != null) {
                    fieldTypesCallback!!.onLoaded(fieldTypeWithHints)
                } else {
                    fieldTypesCallback!!.onDataNotAvailable("Field Types not found.")
                }
            }
        }
    }

    override fun getFieldTypeByAutofillHints(
        fieldTypeMapCallback: DataCallback<HashMap<String?, FieldTypeWithHeuristics?>?>?
    ) {
        mAppExecutors!!.diskIO()!!.execute {
            val hintMap = getFieldTypeByAutofillHints()
            mAppExecutors.mainThread()!!.execute {
                if (hintMap != null) {
                    fieldTypeMapCallback!!.onLoaded(hintMap)
                } else {
                    fieldTypeMapCallback!!.onDataNotAvailable("FieldTypes not found")
                }
            }
        }
    }

    override fun getFilledAutofillField(
        datasetId: String?,
        fieldTypeName: String?,
        fieldCallback: DataCallback<FilledAutofillField?>?
    ) {
        mAppExecutors!!.diskIO()!!.execute {
            val filledAutofillField = mAutofillDao!!.getFilledAutofillField(datasetId, fieldTypeName)
            mAppExecutors.mainThread()!!.execute { fieldCallback!!.onLoaded(filledAutofillField) }
        }
    }

    override fun getFieldType(
        fieldTypeName: String?,
        fieldTypeCallback: DataCallback<FieldType?>?
    ) {
        mAppExecutors!!.diskIO()!!.execute {
            val fieldType = mAutofillDao!!.getFieldType(fieldTypeName)
            mAppExecutors.mainThread()!!.execute { fieldTypeCallback!!.onLoaded(fieldType) }
        }
    }

    fun getAutofillDatasetWithId(
        datasetId: String?,
        callback: DataCallback<DatasetWithFilledAutofillFields?>?
    ) {
        mAppExecutors!!.diskIO()!!.execute {
            val dataset = mAutofillDao!!.getAutofillDatasetWithId(datasetId)
            mAppExecutors.mainThread()!!.execute { callback!!.onLoaded(dataset) }
        }
    }

    private fun getFieldTypeByAutofillHints(): HashMap<String?, FieldTypeWithHeuristics?>? {
        val hintMap = HashMap<String?, FieldTypeWithHeuristics?>()
        val fieldTypeWithHints = mAutofillDao!!.getFieldTypesWithHints()
        return if (fieldTypeWithHints != null) {
            for (fieldType in fieldTypeWithHints) {
                for (hint in fieldType!!.autofillHints!!) {
                    hintMap[hint!!.mAutofillHint] = fieldType
                }
            }
            hintMap
        } else {
            null
        }
    }

    private fun getFieldTypesForAutofillHints(autofillHints: MutableList<String?>?): MutableList<FieldTypeWithHeuristics?>? {
        return mAutofillDao!!.getFieldTypesForAutofillHints(autofillHints)
    }

    override fun clear() {
        mAppExecutors!!.diskIO()!!.execute {
            mAutofillDao!!.clearAll()
            mSharedPreferences!!.edit().putInt(DATASET_NUMBER_KEY, 0).apply()
        }
    }

    /**
     * For simplicity, [Dataset]s will be named in the form `dataset-X.P` where
     * `X` means this was the Xth group of datasets saved, and `P` refers to the dataset
     * partition number. This method returns the appropriate `X`.
     */
    fun getDatasetNumber(): Int {
        return mSharedPreferences!!.getInt(DATASET_NUMBER_KEY, 0)
    }

    /**
     * Every time a dataset is saved, this should be called to increment the dataset number.
     * (only important for this service's dataset naming scheme).
     */
    private fun incrementDatasetNumber() {
        mSharedPreferences!!.edit().putInt(DATASET_NUMBER_KEY, getDatasetNumber() + 1).apply()
    }

    companion object {
        val SHARED_PREF_KEY: String? = ("com.example.android.autofill"
                + ".service.datasource.LocalAutofillDataSource")
        private val DATASET_NUMBER_KEY: String? = "datasetNumber"
        private val sLock: Any? = Any()
        private var sInstance: LocalAutofillDataSource? = null
        fun getInstance(
            sharedPreferences: SharedPreferences?,
            autofillDao: AutofillDao?, appExecutors: AppExecutors?
        ): LocalAutofillDataSource? {
            synchronized(sLock!!) {
                if (sInstance == null) {
                    sInstance = LocalAutofillDataSource(sharedPreferences, autofillDao!!,
                        appExecutors)
                }
                return sInstance
            }
        }

        fun clearInstance() {
            synchronized(sLock!!) { sInstance = null }
        }
    }
}