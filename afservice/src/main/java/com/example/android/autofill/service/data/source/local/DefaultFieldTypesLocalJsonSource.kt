/*
 * Copyright (C) 2018 The Android Open Source Project
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

import com.google.gson.Gson
import com.example.android.autofill.service.R
import android.content.res.Resources
import com.example.android.autofill.service.data.source.DefaultFieldTypesSource
import com.example.android.autofill.service.model.DefaultFieldTypeWithHints
import com.example.android.autofill.service.util.Util
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class DefaultFieldTypesLocalJsonSource private constructor(
    private val mResources: Resources?,
    private val mGson: Gson?
) : DefaultFieldTypesSource {
    override fun getDefaultFieldTypes(): MutableList<DefaultFieldTypeWithHints?>? {
        val fieldTypeListType = TypeToken.getParameterized(
            MutableList::class.java,
            DefaultFieldTypeWithHints::class.java).type
        val `is` = mResources!!.openRawResource(R.raw.default_field_types)
        var fieldTypes: MutableList<DefaultFieldTypeWithHints?>? = null
        try {
            BufferedReader(InputStreamReader(`is`, "UTF-8")).use { reader ->
                fieldTypes = mGson!!.fromJson(reader, fieldTypeListType)
            }
        } catch (e: IOException) {
            Util.loge(e, "Exception during deserialization of FieldTypes.")
        }
        return fieldTypes
    }

    companion object {
        private var sInstance: DefaultFieldTypesLocalJsonSource? = null
        fun getInstance(resources: Resources?, gson: Gson?): DefaultFieldTypesLocalJsonSource? {
            if (sInstance == null) {
                sInstance = DefaultFieldTypesLocalJsonSource(resources, gson)
            }
            return sInstance
        }
    }
}