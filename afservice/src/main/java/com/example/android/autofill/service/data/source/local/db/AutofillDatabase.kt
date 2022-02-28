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
package com.example.android.autofill.service.data.source.local.db

import androidx.annotation.RequiresApi
import android.os.Build
import androidx.room.Database
import androidx.room.TypeConverters
import androidx.room.RoomDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.*
import com.example.android.autofill.service.data.source.DefaultFieldTypesSource
import com.example.android.autofill.service.data.source.local.dao.AutofillDao
import com.example.android.autofill.service.model.*
import com.example.android.autofill.service.util.AppExecutors
import java.util.ArrayList
import java.util.stream.Collectors

@Database(entities = [FilledAutofillField::class, AutofillDataset::class, FieldType::class, AutofillHint::class, ResourceIdHeuristic::class],
    version = 1)
@TypeConverters(
    Converters::class)
abstract class AutofillDatabase : RoomDatabase() {
    @RequiresApi(api = Build.VERSION_CODES.N)
    private fun saveDefaultFieldTypes(defaultFieldTypes: MutableList<DefaultFieldTypeWithHints?>?) {
        val storedFieldTypes: MutableList<FieldType?> = ArrayList()
        val storedAutofillHints: MutableList<AutofillHint?> = ArrayList()
        if (defaultFieldTypes != null) {
            for (defaultType in defaultFieldTypes) {
                val defaultFieldType = defaultType!!.fieldType
                val autofillHints = defaultType.autofillHints
                val autofillTypes = Converters.IntList(defaultFieldType!!.autofillTypes)
                val defaultFakeData = defaultType.fieldType!!.fakeData
                val fakeData = FakeData(Converters.StringList(
                    defaultFakeData!!.strictExampleSet), defaultFakeData.textTemplate,
                    defaultFakeData.dateTemplate)
                val storedFieldType = defaultFieldType.typeName?.let {
                    FieldType(it, autofillTypes,
                        defaultFieldType.saveInfo, defaultFieldType.partition, fakeData)
                }
                storedFieldTypes.add(storedFieldType)
                storedAutofillHints.addAll(autofillHints!!.stream()
                    .map { autofillHint: String? ->
                        AutofillHint(autofillHint!!,
                            storedFieldType!!.getTypeName())
                    }.collect(Collectors.toList()))
            }
        }
        val autofillDao = autofillDao()
        autofillDao!!.insertFieldTypes(storedFieldTypes)
        autofillDao.insertAutofillHints(storedAutofillHints)
    }

    abstract fun autofillDao(): AutofillDao?

    companion object {
        private val sLock: Any? = Any()
        private var sInstance: AutofillDatabase? = null
        fun getInstance(
            context: Context?,
            defaultFieldTypesSource: DefaultFieldTypesSource?,
            appExecutors: AppExecutors?
        ): AutofillDatabase? {
            if (sInstance == null) {
                synchronized(sLock!!) {
                    if (sInstance == null) {
                        sInstance = Room.databaseBuilder(context!!.applicationContext,
                            AutofillDatabase::class.java, "AutofillSample.db")
                            .addCallback(object : Callback() {
                                @RequiresApi(api = Build.VERSION_CODES.N)
                                override fun onCreate(db: SupportSQLiteDatabase) {
                                    appExecutors!!.diskIO()!!.execute {
                                        val fieldTypes =
                                            defaultFieldTypesSource!!.getDefaultFieldTypes()
                                        val autofillDatabase =
                                            getInstance(context, defaultFieldTypesSource,
                                                appExecutors)
                                        autofillDatabase?.saveDefaultFieldTypes(fieldTypes)
                                    }
                                }

                                override fun onOpen(db: SupportSQLiteDatabase) {
                                    super.onOpen(db)
                                }
                            })
                            .build()
                    }
                }
            }
            return sInstance
        }
    }
}