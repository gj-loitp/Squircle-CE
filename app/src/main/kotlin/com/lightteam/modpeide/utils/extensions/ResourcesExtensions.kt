/*
 * Licensed to the Light Team Software (Light Team) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The Light Team licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lightteam.modpeide.utils.extensions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.lightteam.modpeide.BaseApplication
import java.io.BufferedReader

const val FIRA_CODE = "fira_code"
const val SOURCE_CODE_PRO = "source_code_pro"
const val ANONYMOUS_PRO = "anonymous_pro"
const val JETBRAINS_MONO = "jetbrains_mono"
const val DROID_SANS_MONO = "droid_sans_mono"
const val DEJAVU_SANS_MONO = "dejavu_sans_mono"

private const val PATH_FIRA_CODE = "fonts/fira_code.ttf"
private const val PATH_SOURCE_CODE_PRO = "fonts/source_code_pro.ttf"
private const val PATH_ANONYMOUS_PRO = "fonts/anonymous_pro.ttf"
private const val PATH_JETBRAINS_MONO = "fonts/jetbrains_mono.ttf"
private const val PATH_DROID_SANS_MONO = "fonts/droid_sans_mono.ttf"
private const val PATH_DEJAVU_SANS_MONO = "fonts/dejavu_sans_mono.ttf"

fun Context.isUltimate(): Boolean {
    return when (packageName) {
        BaseApplication.STANDARD -> false
        BaseApplication.ULTIMATE -> true
        else -> false
    }
}

fun Context.getDrawableCompat(@DrawableRes drawable: Int): Drawable {
    return ContextCompat.getDrawable(this, drawable) as Drawable
}

fun Context.getColour(@ColorRes colorRes: Int): Int {
    return ContextCompat.getColor(this, colorRes)
}

fun Context.getScaledDensity(): Float {
    return resources.displayMetrics.scaledDensity
}

fun Context.getRawFileText(@RawRes resId: Int): String {
    val inputStream = resources.openRawResource(resId)
    return inputStream.bufferedReader().use(BufferedReader::readText)
}

fun Context.createTypefaceFromAssets(typeface: String): Typeface {
    val path = when (typeface) {
        FIRA_CODE -> PATH_FIRA_CODE
        SOURCE_CODE_PRO -> PATH_SOURCE_CODE_PRO
        ANONYMOUS_PRO -> PATH_ANONYMOUS_PRO
        JETBRAINS_MONO -> PATH_JETBRAINS_MONO
        DROID_SANS_MONO -> PATH_DROID_SANS_MONO
        DEJAVU_SANS_MONO -> PATH_DEJAVU_SANS_MONO
        else -> PATH_JETBRAINS_MONO
    }
    return Typeface.createFromAsset(assets, path)
}

fun String.clipText(context: Context?) = clip(context, ClipData.newPlainText("Text", this))

private fun clip(context: Context?, data: ClipData) {
    context?.getSystemService<ClipboardManager>()?.setPrimaryClip(data)
}