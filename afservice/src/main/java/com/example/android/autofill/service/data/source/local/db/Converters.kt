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

import androidx.room.TypeConverter
import java.util.*
import java.util.stream.Collectors

/**
 * Type converter for Room database.
 */
object Converters {
    /**
     * If database returns a [String] containing a comma delimited list of ints, this converts
     * the [String] to an [IntList].
     */
    @kotlin.jvm.JvmStatic
    @TypeConverter
    open fun storedStringToIntList(value: String): IntList? {
        var x = value
        if (x.endsWith(",") )
            x = x.substring(0, x.length - 1)

        val strings = listOf(*x.split(",").toTypedArray())
        println(x)
        val ints = strings.stream().map { s: String -> s.toInt() }.collect(Collectors.toList())
        return IntList(ints)
    }

    /**
     * Converts the [IntList] back into a String containing a comma delimited list of
     * ints.
     */
    @kotlin.jvm.JvmStatic
    @TypeConverter
    fun intListToStoredString(list: IntList?): String? {
        val stringBuilder = StringBuilder()
        for (integer in list!!.ints!!) {
            stringBuilder.append(integer).append(",")
        }
        return stringBuilder.toString()
    }

    /**
     * If database returns a [String] containing a comma delimited list of Strings, this
     * converts the [String] to a [StringList].
     */
    @kotlin.jvm.JvmStatic
    @TypeConverter
    fun storedStringToStringList(value: String?): StringList? {
        val strings = Arrays.asList(*value!!.split("\\s*,\\s*").toTypedArray())
        return StringList(strings)
    }

    /**
     * Converts the [StringList] back into a [String] containing a comma delimited
     * list of [String]s.
     */
    @kotlin.jvm.JvmStatic
    @TypeConverter
    fun stringListToStoredString(list: StringList?): String? {
        val stringBuilder = StringBuilder()
        for (string in list!!.strings!!) {
            stringBuilder.append(string).append(",")
        }
        return stringBuilder.toString()
    }

    /**
     * Wrapper class for `List<Integer>` so it can work with Room type converters.
     */
    class IntList(val ints: MutableList<Int?>?)

    /**
     * Wrapper class for `List<String>` so it can work with Room type converters.
     */
    class StringList(val strings: MutableList<String?>?)
}