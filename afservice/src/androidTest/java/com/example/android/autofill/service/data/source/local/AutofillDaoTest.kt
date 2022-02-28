/*
 * Copyright 2017, The Android Open Source Project
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

import android.arch.persistence.room.Room
import android.view.View
import com.example.android.autofill.service.data.source.local.db.AutofillDatabase
import com.example.android.autofill.service.model.AutofillDataset
import com.example.android.autofill.service.model.DatasetWithFilledAutofillFields
import com.example.android.autofill.service.model.FilledAutofillField
import com.google.common.collect.ImmutableList
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import java.util.function.Function

@RunWith(AndroidJUnit4::class)
class AutofillDaoTest {
    private val mDataset: AutofillDataset? = AutofillDataset(UUID.randomUUID().toString(),
        "dataset-1", InstrumentationRegistry.getContext().getPackageName())
    private val mUsernameField: FilledAutofillField? =
        FilledAutofillField(mDataset.getId(), View.AUTOFILL_HINT_USERNAME, "login")
    private val mPasswordField: FilledAutofillField? =
        FilledAutofillField(mDataset.getId(), View.AUTOFILL_HINT_PASSWORD, "password")
    private var mDatabase: AutofillDatabase? = null
    @Before
    fun setup() {
        // using an in-memory database because the information stored here disappears when the
        // process is killed
        mDatabase = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getContext(),
            AutofillDatabase::class.java).build()
    }

    @After
    fun closeDb() {
        mDatabase.close()
    }

    @Test
    fun insertFilledAutofillFieldAndGet() {
        val datasetWithFilledAutofillFields = DatasetWithFilledAutofillFields()
        datasetWithFilledAutofillFields.autofillDataset = mDataset
        datasetWithFilledAutofillFields.filledAutofillFields =
            Arrays.asList(mUsernameField, mPasswordField)
        datasetWithFilledAutofillFields.filledAutofillFields
            .sort(Comparator.comparing(Function { obj: FilledAutofillField? -> obj.getFieldTypeName() }))

        // When inserting a page's autofill fields.
        mDatabase.autofillDao().insertAutofillDataset(mDataset)
        mDatabase.autofillDao().insertFilledAutofillFields(
            datasetWithFilledAutofillFields.filledAutofillFields)

        // Represents all hints of all fields on page.
        val allHints: MutableList<String?>? = ImmutableList.of(View.AUTOFILL_HINT_USERNAME,
            View.AUTOFILL_HINT_PASSWORD)
        val loadedDatasets = mDatabase.autofillDao()
            .getDatasets(allHints)
        loadedDatasets[0].filledAutofillFields.sort(
            Comparator.comparing(Function { obj: FilledAutofillField? -> obj.getFieldTypeName() }))
        MatcherAssert.assertThat(loadedDatasets, Matchers.contains(datasetWithFilledAutofillFields))
        MatcherAssert.assertThat(loadedDatasets, Matchers.hasSize(1))
    }
}