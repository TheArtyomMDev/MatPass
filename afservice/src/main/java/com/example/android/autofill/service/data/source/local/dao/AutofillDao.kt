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
package com.example.android.autofill.service.data.source.local.dao

import androidx.room.*
import com.example.android.autofill.service.model.*

@Dao
interface AutofillDao {
    /**
     * Fetches a list of datasets associated to autofill fields on the page.
     *
     * @param allAutofillHints Filtering parameter; represents all of the hints associated with
     * all of the views on the page.
     */
    @Query("SELECT DISTINCT id, datasetName FROM FilledAutofillField, AutofillDataset" +
            " WHERE AutofillDataset.id = FilledAutofillField.datasetId" +
            " AND FilledAutofillField.fieldTypeName IN (:allAutofillHints)")
    open fun getDatasets(allAutofillHints: MutableList<String?>?): MutableList<DatasetWithFilledAutofillFields?>?

    @Query("SELECT DISTINCT id, datasetName FROM FilledAutofillField, AutofillDataset" +
            " WHERE AutofillDataset.id = FilledAutofillField.datasetId")
    open fun getAllDatasets(): MutableList<DatasetWithFilledAutofillFields?>?

    /**
     * Fetches a list of datasets associated to autofill fields. It should only return a dataset
     * if that dataset has an autofill field associate with the view the user is focused on, and
     * if that dataset's name matches the name passed in.
     *
     * @param fieldTypes Filtering parameter; represents all of the field types associated with
     * all of the views on the page.
     * @param datasetName      Filtering parameter; only return datasets with this name.
     */
    @Query("SELECT DISTINCT id, datasetName FROM FilledAutofillField, AutofillDataset" +
            " WHERE AutofillDataset.id = FilledAutofillField.datasetId" +
            " AND AutofillDataset.datasetName = (:datasetName)" +
            " AND FilledAutofillField.fieldTypeName IN (:fieldTypes)")
    open fun getDatasetsWithName(
        fieldTypes: MutableList<String?>?, datasetName: String?
    ): MutableList<DatasetWithFilledAutofillFields?>?

    @Query("SELECT DISTINCT typeName, autofillTypes, saveInfo, partition, strictExampleSet, " +
            "textTemplate, dateTemplate" +
            " FROM FieldType, AutofillHint" +
            " WHERE FieldType.typeName = AutofillHint.fieldTypeName" +
            " UNION " +
            "SELECT DISTINCT typeName, autofillTypes, saveInfo, partition, strictExampleSet, " +
            "textTemplate, dateTemplate" +
            " FROM FieldType, ResourceIdHeuristic" +
            " WHERE FieldType.typeName = ResourceIdHeuristic.fieldTypeName")
    open fun getFieldTypesWithHints(): MutableList<FieldTypeWithHeuristics?>?

    @Query("SELECT DISTINCT typeName, autofillTypes, saveInfo, partition, strictExampleSet, " +
            "textTemplate, dateTemplate" +
            " FROM FieldType, AutofillHint" +
            " WHERE FieldType.typeName = AutofillHint.fieldTypeName" +
            " AND AutofillHint.autofillHint IN (:autofillHints)" +
            " UNION " +
            "SELECT DISTINCT typeName, autofillTypes, saveInfo, partition, strictExampleSet, " +
            "textTemplate, dateTemplate" +
            " FROM FieldType, ResourceIdHeuristic" +
            " WHERE FieldType.typeName = ResourceIdHeuristic.fieldTypeName")
    open fun getFieldTypesForAutofillHints(autofillHints: MutableList<String?>?): MutableList<FieldTypeWithHeuristics?>?

    @Query("SELECT DISTINCT id, datasetName FROM FilledAutofillField, AutofillDataset" +
            " WHERE AutofillDataset.id = FilledAutofillField.datasetId" +
            " AND AutofillDataset.id = (:datasetId)")
    open fun getAutofillDatasetWithId(datasetId: String?): DatasetWithFilledAutofillFields?

    @Query("SELECT * FROM FilledAutofillField" +
            " WHERE FilledAutofillField.datasetId = (:datasetId)" +
            " AND FilledAutofillField.fieldTypeName = (:fieldTypeName)")
    open fun getFilledAutofillField(
        datasetId: String?,
        fieldTypeName: String?
    ): FilledAutofillField?

    @Query("SELECT * FROM FieldType" +
            " WHERE FieldType.typeName = (:fieldTypeName)")
    open fun getFieldType(fieldTypeName: String?): FieldType?

    /**
     * @param autofillFields Collection of autofill fields to be saved to the db.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    open fun insertFilledAutofillFields(autofillFields: MutableCollection<FilledAutofillField?>?)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    open fun insertAutofillDataset(datasets: AutofillDataset?)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    open fun insertAutofillHints(autofillHints: MutableList<AutofillHint?>?)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    open fun insertResourceIdHeuristic(resourceIdHeuristic: ResourceIdHeuristic?)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    open fun insertFieldTypes(fieldTypes: MutableList<FieldType?>?)
    @Query("DELETE FROM AutofillDataset")
    open fun clearAll()
}