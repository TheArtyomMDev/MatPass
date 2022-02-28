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

import androidx.annotation.RequiresApi
import android.os.Build
import android.app.assist.AssistStructure.ViewNode
import android.app.assist.AssistStructure
import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableList

/**
 * Wrapper for [AssistStructure] to make it easy to parse.
 */
class ClientParser(structures: MutableList<AssistStructure?>) {
    private val mStructures: MutableList<AssistStructure?>?

    constructor(structure: AssistStructure) : this(ImmutableList.of<AssistStructure?>(structure)) {}

    /**
     * Traverses through the [AssistStructure] and does something at each [ViewNode].
     *
     * @param processor contains action to be performed on each [ViewNode].
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun parse(processor: NodeProcessor?) {
        if (mStructures != null) {
            for (structure in mStructures) {
                val nodes = structure?.getWindowNodeCount()
                for (i in 0 until nodes!!) {
                    val viewNode = structure.getWindowNodeAt(i).rootViewNode
                    traverseRoot(viewNode, processor)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun traverseRoot(viewNode: ViewNode?, processor: NodeProcessor?) {
        processor?.processNode(viewNode)
        val childrenSize = viewNode?.childCount
        if (childrenSize != null) {
            if (childrenSize > 0) {
                for (i in 0 until childrenSize) {
                    traverseRoot(viewNode.getChildAt(i), processor)
                }
            }
        }
    }

    interface NodeProcessor {
        open fun processNode(node: ViewNode?)
    }

    init {
        Preconditions.checkNotNull(structures)
        mStructures = structures
    }
}