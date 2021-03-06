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

interface PackageVerificationDataSource {
    /**
     * Verifies that the signatures in the passed `Context` match what is currently in
     * storage. If there are no current signatures in storage for this packageName, it will store
     * the signatures from the passed `Context`.
     *
     * @return `true` if signatures for this packageName are not currently in storage
     * or if the signatures in the passed `Context` match what is currently in storage;
     * `false` if the signatures in the passed `Context` do not match what is
     * currently in storage or if an `Exception` was thrown while generating the signatures.
     */
    open fun putPackageSignatures(packageName: String?): Boolean

    /**
     * Clears all signature data currently in storage.
     */
    open fun clear()
}