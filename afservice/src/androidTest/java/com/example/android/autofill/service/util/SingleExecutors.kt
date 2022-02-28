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
package com.example.android.autofill.service.util

import androidx.test.filters.LargeTest
import android.content.SharedPreferences
import androidx.annotation.RequiresApi
import android.os.Build
import androidx.room.Database
import androidx.room.TypeConverters
import androidx.room.RoomDatabase
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.Dao
import androidx.room.OnConflictStrategy
import android.content.pm.PackageManager
import com.google.common.net.InternetDomainName
import retrofit2.Retrofit
import com.google.gson.Gson
import com.example.android.autofill.service.R
import retrofit2.http.GET
import com.example.android.autofill.service.ClientParser
import android.widget.RemoteViews
import android.service.autofill.Dataset
import kotlin.jvm.JvmOverloads
import android.content.IntentSender
import android.util.MutableBoolean
import com.example.android.autofill.service.ClientParser.NodeProcessor
import android.app.assist.AssistStructure.ViewNode
import com.example.android.autofill.service.AutofillHints
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.service.autofill.FillResponse
import com.example.android.autofill.service.RemoteViewsHelper
import com.example.android.autofill.service.AuthActivity
import android.service.autofill.SaveInfo
import android.util.MutableInt
import android.os.Bundle
import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.WindowNode
import android.view.ViewStructure.HtmlInfo
import android.service.autofill.FillContext
import android.os.Looper
import kotlin.Throws
import androidx.room.ColumnInfo
import androidx.room.Embedded
import android.service.autofill.AutofillService
import android.service.autofill.FillRequest
import android.service.autofill.FillCallback
import android.service.autofill.SaveRequest
import android.service.autofill.SaveCallback
import android.widget.Toast
import android.app.Activity
import android.content.Intent
import android.view.autofill.AutofillManager
import android.os.Parcelable
import android.app.PendingIntent
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.GsonBuilder
import android.widget.CompoundButton
import android.widget.RadioGroup
import android.content.DialogInterface
import android.widget.NumberPicker
import android.view.LayoutInflater
import com.google.android.material.snackbar.Snackbar
import android.widget.EditText
import android.view.ViewGroup
import android.widget.TextView
import android.text.Editable
import com.example.android.autofill.service.W3cHints
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.android.autofill.service.ManualActivity.AutofillDatasetsAdapter
import com.example.android.autofill.service.ManualActivity
import com.example.android.autofill.service.ManualFieldPickerActivity
import com.example.android.autofill.service.ManualActivity.DatasetViewHolder
import androidx.annotation.DrawableRes
import com.example.android.autofill.service.FakeFieldGenerator
import com.example.android.autofill.service.ManualFieldPickerActivity.FieldsAdapter
import com.example.android.autofill.service.ManualFieldPickerActivity.FieldViewHolder
import java.util.concurrent.Executor

/**
 * Allow instant execution of tasks.
 */
object SingleExecutors : AppExecutors() {
    private val sInstance: Executor? = Executor { obj: Runnable? -> obj.run() }
}