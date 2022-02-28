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
package com.example.android.autofill.service.model

import androidx.room.*

@Entity(primaryKeys = ["datasetId", "fieldTypeName"],
    foreignKeys = [ForeignKey(entity = AutofillDataset::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("datasetId"),
        onDelete = ForeignKey.CASCADE), ForeignKey(entity = FieldType::class,
        parentColumns = arrayOf("typeName"),
        childColumns = arrayOf("fieldTypeName"),
        onDelete = ForeignKey.CASCADE)])
class FilledAutofillField(
    @field:ColumnInfo(name = "datasetId") private val mDatasetId: String,
    @field:ColumnInfo(
        name = "fieldTypeName") private val mFieldTypeName: String,
    @field:ColumnInfo(name = "textValue") private val mTextValue: String?,
    @field:ColumnInfo(
        name = "dateValue") private val mDateValue: Long?,
    @field:ColumnInfo(name = "toggleValue") private val mToggleValue: Boolean?
) {
    @Ignore
    constructor(
        datasetId: String,
        fieldTypeName: String, textValue: String?, dateValue: Long?
    ) : this(datasetId, fieldTypeName, textValue, dateValue, null) {
    }

    @Ignore
    constructor(
        datasetId: String, fieldTypeName: String,
        textValue: String?
    ) : this(datasetId, fieldTypeName, textValue, null, null) {
    }

    @Ignore
    constructor(
        datasetId: String, fieldTypeName: String,
        dateValue: Long?
    ) : this(datasetId, fieldTypeName, null, dateValue, null) {
    }

    @Ignore
    constructor(
        datasetId: String, fieldTypeName: String,
        toggleValue: Boolean?
    ) : this(datasetId, fieldTypeName, null, null, toggleValue) {
    }

    @Ignore
    constructor(datasetId: String, fieldTypeName: String) : this(datasetId,
        fieldTypeName,
        null,
        null,
        null) {
    }

    fun getDatasetId(): String {
        return mDatasetId
    }

    fun getTextValue(): String? {
        return mTextValue
    }

    fun getDateValue(): Long? {
        return mDateValue
    }

    fun getToggleValue(): Boolean? {
        return mToggleValue
    }

    fun getFieldTypeName(): String {
        return mFieldTypeName
    }

    fun isNull(): Boolean {
        return mTextValue == null && mDateValue == null && mToggleValue == null
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as FilledAutofillField?
        if (if (mTextValue != null) mTextValue != that!!.mTextValue else that!!.mTextValue != null) return false
        if (if (mDateValue != null) mDateValue != that.mDateValue else that.mDateValue != null) return false
        return if (if (mToggleValue != null) mToggleValue != that.mToggleValue else that.mToggleValue != null) false else mFieldTypeName == that.mFieldTypeName
    }

    override fun hashCode(): Int {
        var result = mTextValue?.hashCode() ?: 0
        result = 31 * result + (mDateValue?.hashCode() ?: 0)
        result = 31 * result + (mToggleValue?.hashCode() ?: 0)
        result = 31 * result + mFieldTypeName.hashCode()
        return result
    }
}