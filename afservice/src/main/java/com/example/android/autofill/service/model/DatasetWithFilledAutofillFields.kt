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
import java.util.ArrayList

class DatasetWithFilledAutofillFields {
    @kotlin.jvm.JvmField
    @Embedded
    var autofillDataset: AutofillDataset? = null

    @kotlin.jvm.JvmField
    @Relation(parentColumn = "id", entityColumn = "datasetId", entity = FilledAutofillField::class)
    var filledAutofillFields: MutableList<FilledAutofillField?>? = null
    fun add(filledAutofillField: FilledAutofillField?) {
        if (filledAutofillFields == null) {
            filledAutofillFields = ArrayList()
        }
        filledAutofillFields!!.add(filledAutofillField)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as DatasetWithFilledAutofillFields?
        if (if (autofillDataset != null) autofillDataset != that!!.autofillDataset else that!!.autofillDataset != null) return false
        return if (filledAutofillFields != null) filledAutofillFields == that.filledAutofillFields else that.filledAutofillFields == null
    }

    override fun hashCode(): Int {
        var result = if (autofillDataset != null) autofillDataset.hashCode() else 0
        result =
            31 * result + if (filledAutofillFields != null) filledAutofillFields.hashCode() else 0
        return result
    }
}