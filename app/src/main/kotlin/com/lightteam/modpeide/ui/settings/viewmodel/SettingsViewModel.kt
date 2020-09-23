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

package com.lightteam.modpeide.ui.settings.viewmodel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import com.f2prateek.rx.preferences2.Preference
import com.lightteam.modpeide.R
import com.lightteam.modpeide.data.converter.ReleaseConverter
import com.lightteam.modpeide.data.settings.SettingsManager
import com.lightteam.modpeide.data.utils.extensions.schedulersIoToMain
import com.lightteam.modpeide.domain.model.changelog.ReleaseModel
import com.lightteam.modpeide.domain.providers.rx.SchedulersProvider
import com.lightteam.modpeide.ui.base.viewmodel.BaseViewModel
import com.lightteam.modpeide.ui.settings.adapters.item.PreferenceItem
import com.lightteam.modpeide.utils.event.SingleLiveEvent
import io.reactivex.rxkotlin.subscribeBy

class SettingsViewModel @ViewModelInject constructor(
    private val schedulersProvider: SchedulersProvider,
    private val settingsManager: SettingsManager
) : BaseViewModel() {

    val fullscreenEvent: SingleLiveEvent<Boolean> = SingleLiveEvent() // Полноэкранный режим

    val headersEvent: MutableLiveData<List<PreferenceItem>> = MutableLiveData()
    val changelogEvent: MutableLiveData<List<ReleaseModel>> = MutableLiveData()

    var keyboardPreset: Preference<String> = settingsManager.getKeyboardPreset()

    fun fetchHeaders() {
        headersEvent.value = listOf(
            PreferenceItem(
                R.string.pref_header_application_title,
                R.string.pref_header_application_summary,
                R.id.applicationFragment
            ),
            PreferenceItem(
                R.string.pref_header_editor_title,
                R.string.pref_header_editor_summary,
                R.id.editorFragment
            ),
            PreferenceItem(
                R.string.pref_header_codeStyle_title,
                R.string.pref_header_codeStyle_summary,
                R.id.codeStyleFragment
            ),
            PreferenceItem(
                R.string.pref_header_files_title,
                R.string.pref_header_files_summary,
                R.id.filesFragment
            ),
            PreferenceItem(
                R.string.pref_header_about_title,
                R.string.pref_header_about_summary,
                R.id.aboutFragment
            )
        )
    }

    fun fetchChangeLog(changelog: String) {
        changelogEvent.value = ReleaseConverter.toReleaseModels(changelog)
    }

    fun observeSettings() {
        settingsManager.getFullscreenMode()
            .asObservable()
            .schedulersIoToMain(schedulersProvider)
            .subscribeBy { fullscreenEvent.value = it }
            .disposeOnViewModelDestroy()
    }
}