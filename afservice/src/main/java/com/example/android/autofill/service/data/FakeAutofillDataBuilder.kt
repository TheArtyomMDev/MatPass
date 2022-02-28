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
package com.example.android.autofill.service.data

import com.example.android.autofill.service.AutofillHints
import com.example.android.autofill.service.model.AutofillDataset
import com.example.android.autofill.service.model.DatasetWithFilledAutofillFields
import com.example.android.autofill.service.model.FieldTypeWithHeuristics
import com.google.common.collect.ImmutableList
import java.util.*

class FakeAutofillDataBuilder(
    private val mFieldTypesWithHints: MutableList<FieldTypeWithHeuristics?>?,
    private val mPackageName: String?, private val mSeed: Int
) : AutofillDataBuilder {
    override fun buildDatasetsByPartition(datasetNumber: Int): MutableList<DatasetWithFilledAutofillFields?>? {
        val listBuilder = ImmutableList.Builder<DatasetWithFilledAutofillFields?>()
        for (partition in AutofillHints.PARTITIONS!!) {
            val autofillDataset = mPackageName?.let {
                AutofillDataset(UUID.randomUUID().toString(),
                    "dataset-$datasetNumber.$partition", it)
            }
            val datasetWithFilledAutofillFields =
                buildCollectionForPartition(autofillDataset, partition)
            if (datasetWithFilledAutofillFields != null && datasetWithFilledAutofillFields.filledAutofillFields != null &&
                !datasetWithFilledAutofillFields.filledAutofillFields!!.isEmpty()
            ) {
                listBuilder.add(datasetWithFilledAutofillFields)
            }
        }
        return listBuilder.build()
    }

    private fun buildCollectionForPartition(
        dataset: AutofillDataset?, partition: Int
    ): DatasetWithFilledAutofillFields? {
        val datasetWithFilledAutofillFields = DatasetWithFilledAutofillFields()
        datasetWithFilledAutofillFields.autofillDataset = dataset
        if (mFieldTypesWithHints != null) {
            for (fieldTypeWithHeuristics in mFieldTypesWithHints) {
                if (AutofillHints.matchesPartition(
                        fieldTypeWithHeuristics!!.getFieldType()!!.getPartition(), partition)
                ) {
                    val fakeField =
                        AutofillHints.generateFakeField(fieldTypeWithHeuristics, mPackageName,
                            mSeed, dataset!!.getId())
                    datasetWithFilledAutofillFields.add(fakeField)
                }
            }
        }
        return datasetWithFilledAutofillFields
    }
}