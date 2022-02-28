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
package com.example.android.autofill.service

import android.widget.RemoteViews
import androidx.annotation.DrawableRes

/**
 * This is a class containing helper methods for building Autofill Datasets and Responses.
 */
object RemoteViewsHelper {
    fun viewsWithAuth(packageName: String?, text: String?): RemoteViews? {
        return simpleRemoteViews(packageName, text, R.drawable.ic_lock_black_24dp)
    }

    fun viewsWithNoAuth(packageName: String?, text: String?): RemoteViews? {
        return simpleRemoteViews(packageName, text, R.drawable.ic_person_black_24dp)
    }

    private fun simpleRemoteViews(
        packageName: String?, remoteViewsText: String?,
        @DrawableRes drawableId: Int
    ): RemoteViews? {
        val presentation = RemoteViews(packageName,
            R.layout.multidataset_service_list_item)
        presentation.setTextViewText(R.id.text, remoteViewsText)
        presentation.setImageViewResource(R.id.icon, drawableId)
        return presentation
    }
}