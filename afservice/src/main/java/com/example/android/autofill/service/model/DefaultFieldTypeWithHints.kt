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
package com.example.android.autofill.service.model

/**
 * JSON model class, representing an autofillable field type. It is called "Default" because only
 * default field types will be included in the packaged JSON. After the JSON is initially read and
 * written to the DB, the field types can be dynamically added, modified, and removed.
 *
 *
 * It contains all of the metadata about the field type. For example, if the field type is
 * "country", this is the JSON object associated with it:
 * <pre class="prettyprint">
 * {
 * "autofillHints": [
 * "country"
 * ],
 * "fieldType": {
 * "autofillTypes": [
 * 1,
 * 3
 * ],
 * "fakeData": {
 * "strictExampleSet": [],
 * "textTemplate": "countryseed"
 * },
 * "partition": 1,
 * "saveInfo": 2,
 * "typeName": "country"
 * }
 * }
</pre> *
 */
class DefaultFieldTypeWithHints {
    var fieldType: DefaultFieldType? = null
    var autofillHints: MutableList<String?>? = null

    class DefaultFieldType {
        var typeName: String? = null
        var autofillTypes: MutableList<Int?>? = null
        var saveInfo = 0
        var partition = 0
        var fakeData: DefaultFakeData? = null
    }

    class DefaultFakeData(
        var strictExampleSet: MutableList<String?>?, var textTemplate: String?,
        var dateTemplate: String?
    )
}