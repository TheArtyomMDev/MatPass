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

import android.arch.persistence.room.Room
import android.content.Context
import com.example.android.autofill.service.data.source.local.db.AutofillDatabase
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class LocalDataSourceTest {
    private var mLocalDataSource: LocalAutofillDataSource? = null
    private var mDatabase: AutofillDatabase? = null
    @Before
    fun setup() {
        // using an in-memory database for testing, since it doesn't survive killing the process
        mDatabase = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getContext(),
            AutofillDatabase::class.java)
            .build()
        val tasksDao = mDatabase.autofillDao()
        val sharedPreferences: SharedPreferences = InstrumentationRegistry.getContext()
            .getSharedPreferences(LocalAutofillDataSource.Companion.SHARED_PREF_KEY,
                Context.MODE_PRIVATE)
        // Make sure that we're not keeping a reference to the wrong instance.
        LocalAutofillDataSource.Companion.clearInstance()
        mLocalDataSource = LocalAutofillDataSource.Companion.getInstance(sharedPreferences,
            tasksDao, SingleExecutors())
    }

    @After
    fun cleanUp() {
        try {
            mDatabase.close()
        } finally {
            LocalAutofillDataSource.Companion.clearInstance()
        }
    }
}