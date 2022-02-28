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

@Entity(primaryKeys = ["id"])
class AutofillDataset(
    @field:ColumnInfo(name = "id") private val mId: String,
    @field:ColumnInfo(name = "datasetName") private val mDatasetName: String,
    @field:ColumnInfo(name = "packageName") private val mPackageName: String?
) {
    fun getId(): String {
        return mId
    }

    fun getDatasetName(): String {
        return mDatasetName
    }

    fun getPackageName(): String? {
        return mPackageName
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as AutofillDataset?
        if (mId != that!!.mId) return false
        return if (mDatasetName != that.mDatasetName) false else mPackageName == that.mPackageName
    }

    override fun hashCode(): Int {
        var result = mId.hashCode()
        result = 31 * result + mDatasetName.hashCode()
        result = 31 * result + mPackageName.hashCode()
        return result
    }
}