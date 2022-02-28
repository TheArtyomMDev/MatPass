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

import android.os.*
import androidx.annotation.VisibleForTesting
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Global executor pools for the whole application.
 *
 *
 * Grouping tasks like this avoids the effects of task starvation (e.g. disk reads don't wait behind
 * webservice requests).
 */
open class AppExecutors @VisibleForTesting internal constructor(
    private val diskIO: Executor?,
    private val networkIO: Executor?,
    private val mainThread: Executor?
) {
    constructor() : this(DiskIOThreadExecutor(), Executors.newFixedThreadPool(THREAD_COUNT),
        MainThreadExecutor()) {
    }

    fun diskIO(): Executor? {
        return diskIO
    }

    fun networkIO(): Executor? {
        return networkIO
    }

    fun mainThread(): Executor? {
        return mainThread
    }

    private class MainThreadExecutor : Executor {
        private val mainThreadHandler: Handler? = Handler(Looper.getMainLooper())
        override fun execute(command: Runnable) {
            mainThreadHandler?.post(command)
        }
    }

    /**
     * Executor that runs a task on a new background thread.
     */
    private class DiskIOThreadExecutor : Executor {
        private val mDiskIO: Executor?
        override fun execute(command: Runnable) {
            mDiskIO?.execute(command)
        }

        init {
            mDiskIO = Executors.newSingleThreadExecutor()
        }
    }

    companion object {
        private const val THREAD_COUNT = 3
    }
}