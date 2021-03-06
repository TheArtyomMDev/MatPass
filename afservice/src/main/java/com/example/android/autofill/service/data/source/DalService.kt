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
package com.example.android.autofill.service.data.source

import retrofit2.http.GET
import com.example.android.autofill.service.model.DalCheck
import retrofit2.Call
import retrofit2.http.Query

interface DalService {
    @GET("/v1/assetlinks:check")
    open fun check(
        @Query("source.web.site") webDomain: String?,
        @Query("relation") permission: String?,
        @Query("target.android_app.package_name") packageName: String?,
        @Query("target.android_app.certificate.sha256_fingerprint") fingerprint: String?
    ): Call<DalCheck?>?
}