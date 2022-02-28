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
package com.example.android.autofill.service.data.source

import com.example.android.autofill.service.data.DataCallback
import com.example.android.autofill.service.model.*
import java.util.HashMap

interface AutofillDataSource {
    /**
     * Asynchronously gets saved list of [DatasetWithFilledAutofillFields] that contains some
     * objects that can autofill fields with these `autofillHints`.
     */
    open fun getAutofillDatasets(
        allAutofillHints: MutableList<String?>?,
        datasetsCallback: DataCallback<MutableList<DatasetWithFilledAutofillFields?>?>?
    )

    open fun getAllAutofillDatasets(
        datasetsCallback: DataCallback<MutableList<DatasetWithFilledAutofillFields?>?>?
    )

    /**
     * Asynchronously gets a saved [DatasetWithFilledAutofillFields] for a specific
     * `datasetName` that contains some objects that can autofill fields with these
     * `autofillHints`.
     */
    open fun getAutofillDataset(
        allAutofillHints: MutableList<String?>?,
        datasetName: String?, datasetsCallback: DataCallback<DatasetWithFilledAutofillFields?>?
    )

    /**
     * Stores a collection of Autofill fields.
     */
    open fun saveAutofillDatasets(datasetsWithFilledAutofillFields: MutableList<DatasetWithFilledAutofillFields?>?)
    open fun saveResourceIdHeuristic(resourceIdHeuristic: ResourceIdHeuristic?)

    /**
     * Gets all autofill field types.
     */
    open fun getFieldTypes(fieldTypesCallback: DataCallback<MutableList<FieldTypeWithHeuristics?>?>?)

    /**
     * Gets all autofill field types.
     */
    open fun getFieldType(typeName: String?, fieldTypeCallback: DataCallback<FieldType?>?)
    open fun getFieldTypeByAutofillHints(
        fieldTypeMapCallback: DataCallback<HashMap<String?, FieldTypeWithHeuristics?>?>?
    )

    open fun getFilledAutofillField(
        datasetId: String?,
        fieldTypeName: String?,
        fieldCallback: DataCallback<FilledAutofillField?>?
    )

    /**
     * Clears all data.
     */
    open fun clear()
}