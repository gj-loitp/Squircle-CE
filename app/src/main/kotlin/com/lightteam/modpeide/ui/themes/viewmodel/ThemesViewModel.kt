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

package com.lightteam.modpeide.ui.themes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lightteam.modpeide.R
import com.lightteam.modpeide.data.converter.ThemeConverter
import com.lightteam.modpeide.data.feature.scheme.Theme
import com.lightteam.modpeide.data.storage.keyvalue.PreferenceHandler
import com.lightteam.modpeide.data.utils.extensions.schedulersIoToMain
import com.lightteam.modpeide.database.AppDatabase
import com.lightteam.modpeide.database.entity.theme.ThemeEntity
import com.lightteam.modpeide.domain.feature.theme.Meta
import com.lightteam.modpeide.domain.feature.theme.Property
import com.lightteam.modpeide.domain.providers.rx.SchedulersProvider
import com.lightteam.modpeide.ui.base.viewmodel.BaseViewModel
import com.lightteam.modpeide.ui.themes.adapters.item.PropertyItem
import com.lightteam.modpeide.utils.event.SingleLiveEvent
import io.reactivex.Completable
import io.reactivex.rxkotlin.subscribeBy
import java.util.*

class ThemesViewModel(
    private val schedulersProvider: SchedulersProvider,
    private val preferenceHandler: PreferenceHandler,
    private val appDatabase: AppDatabase
) : BaseViewModel() {

    companion object {
        private const val FALLBACK_META = "" // empty string
        private const val FALLBACK_COLOR = "#000000"
    }

    val themesEvent: SingleLiveEvent<List<Theme>> = SingleLiveEvent()

    val validationEvent: SingleLiveEvent<Boolean> = SingleLiveEvent()
    val metaEvent: SingleLiveEvent<Meta> = SingleLiveEvent()
    val propertiesEvent: SingleLiveEvent<List<PropertyItem>> = SingleLiveEvent()

    val selectEvent: SingleLiveEvent<String> = SingleLiveEvent()
    val insertEvent: SingleLiveEvent<String> = SingleLiveEvent()
    val removeEvent: SingleLiveEvent<String> = SingleLiveEvent()

    // region PROPERTIES

    private var textColor: String = FALLBACK_COLOR
    private var backgroundColor: String = FALLBACK_COLOR
    private var gutterColor: String = FALLBACK_COLOR
    private var gutterDividerColor: String = FALLBACK_COLOR
    private var gutterCurrentLineNumberColor: String = FALLBACK_COLOR
    private var gutterTextColor: String = FALLBACK_COLOR
    private var selectedLineColor: String = FALLBACK_COLOR
    private var selectionColor: String = FALLBACK_COLOR
    private var suggestionMatchColor: String = FALLBACK_COLOR
    private var searchBackgroundColor: String = FALLBACK_COLOR
    private var delimitersBackgroundColor: String = FALLBACK_COLOR
    private var numberColor: String = FALLBACK_COLOR
    private var operatorColor: String = FALLBACK_COLOR
    private var bracketColor: String = FALLBACK_COLOR
    private var keywordColor: String = FALLBACK_COLOR
    private var typeColor: String = FALLBACK_COLOR
    private var langConstColor: String = FALLBACK_COLOR
    private var methodColor: String = FALLBACK_COLOR
    private var stringColor: String = FALLBACK_COLOR
    private var commentColor: String = FALLBACK_COLOR

    // endregion PROPERTIES

    fun fetchThemes() {
        appDatabase.themeDao().loadAll()
            .map { it.map(ThemeConverter::toModel) }
            .schedulersIoToMain(schedulersProvider)
            .subscribeBy { themesEvent.value = it }
            .disposeOnViewModelDestroy()
    }

    fun selectTheme(theme: Theme) {
        preferenceHandler.getColorScheme().set(theme.uuid)
        selectEvent.value = theme.name
    }

    fun removeTheme(theme: Theme) {
        Completable
            .fromAction {
                appDatabase.themeDao().delete(ThemeConverter.toEntity(theme))
            }
            .schedulersIoToMain(schedulersProvider)
            .subscribeBy {
                removeEvent.value = theme.name
                fetchThemes() // Update list
            }
            .disposeOnViewModelDestroy()
    }

    fun validateInput(name: String, author: String, description: String) {
        val isNameValid = name.trim().isNotBlank()
        val isAuthorValid = author.trim().isNotBlank()
        val isDescriptionValid = description.trim().isNotBlank()
        validationEvent.value = isNameValid && isAuthorValid && isDescriptionValid
    }

    fun fetchProperties(uuid: String?) {
        appDatabase.themeDao().load(uuid ?: "unknown")
            .schedulersIoToMain(schedulersProvider)
            .subscribeBy(
                onSuccess = {
                    loadProperties(it)
                },
                onError = {
                    loadProperties(null)
                }
            )
            .disposeOnViewModelDestroy()
    }

    fun insertTheme(meta: Meta, properties: List<PropertyItem>) {
        for (property in properties) {
            when (property.propertyKey) {
                Property.TEXT_COLOR -> textColor = property.propertyValue
                Property.BACKGROUND_COLOR -> backgroundColor = property.propertyValue
                Property.GUTTER_COLOR -> gutterColor = property.propertyValue
                Property.GUTTER_DIVIDER_COLOR -> gutterDividerColor = property.propertyValue
                Property.GUTTER_CURRENT_LINE_NUMBER_COLOR -> gutterCurrentLineNumberColor = property.propertyValue
                Property.GUTTER_TEXT_COLOR -> gutterTextColor = property.propertyValue
                Property.SELECTED_LINE_COLOR -> selectedLineColor = property.propertyValue
                Property.SELECTION_COLOR -> selectionColor = property.propertyValue
                Property.SUGGESTION_MATCH_COLOR -> suggestionMatchColor = property.propertyValue
                Property.SEARCH_BACKGROUND_COLOR -> searchBackgroundColor = property.propertyValue
                Property.DELIMITERS_BACKGROUND_COLOR -> delimitersBackgroundColor = property.propertyValue
                Property.NUMBER_COLOR -> numberColor = property.propertyValue
                Property.OPERATOR_COLOR -> operatorColor = property.propertyValue
                Property.BRACKET_COLOR -> bracketColor = property.propertyValue
                Property.KEYWORD_COLOR -> keywordColor = property.propertyValue
                Property.TYPE_COLOR -> typeColor = property.propertyValue
                Property.LANG_CONST_COLOR -> langConstColor = property.propertyValue
                Property.METHOD_COLOR -> methodColor = property.propertyValue
                Property.STRING_COLOR -> stringColor = property.propertyValue
                Property.COMMENT_COLOR -> commentColor = property.propertyValue
            }
        }
        Completable
            .fromAction {
                val themeEntity = ThemeEntity(
                    uuid = meta.uuid,
                    name = meta.name,
                    author = meta.author,
                    description = meta.description,
                    isExternal = meta.isExternal,
                    isPaid = meta.isPaid,
                    textColor = textColor,
                    backgroundColor = backgroundColor,
                    gutterColor = gutterColor,
                    gutterDividerColor = gutterDividerColor,
                    gutterCurrentLineNumberColor = gutterCurrentLineNumberColor,
                    gutterTextColor = gutterTextColor,
                    selectedLineColor = selectedLineColor,
                    selectionColor = selectionColor,
                    suggestionMatchColor = suggestionMatchColor,
                    searchBackgroundColor = searchBackgroundColor,
                    delimitersBackgroundColor = delimitersBackgroundColor,
                    numberColor = numberColor,
                    operatorColor = operatorColor,
                    bracketColor = bracketColor,
                    keywordColor = keywordColor,
                    typeColor = typeColor,
                    langConstColor = langConstColor,
                    methodColor = methodColor,
                    stringColor = stringColor,
                    commentColor = commentColor
                )
                appDatabase.themeDao().insert(themeEntity)
            }
            .schedulersIoToMain(schedulersProvider)
            .subscribeBy {
                insertEvent.value = meta.name
                textColor = FALLBACK_COLOR
                backgroundColor = FALLBACK_COLOR
                gutterColor = FALLBACK_COLOR
                gutterDividerColor = FALLBACK_COLOR
                gutterCurrentLineNumberColor = FALLBACK_COLOR
                gutterTextColor = FALLBACK_COLOR
                selectedLineColor = FALLBACK_COLOR
                selectionColor = FALLBACK_COLOR
                suggestionMatchColor = FALLBACK_COLOR
                searchBackgroundColor = FALLBACK_COLOR
                delimitersBackgroundColor = FALLBACK_COLOR
                numberColor = FALLBACK_COLOR
                operatorColor = FALLBACK_COLOR
                bracketColor = FALLBACK_COLOR
                keywordColor = FALLBACK_COLOR
                typeColor = FALLBACK_COLOR
                langConstColor = FALLBACK_COLOR
                methodColor = FALLBACK_COLOR
                stringColor = FALLBACK_COLOR
                commentColor = FALLBACK_COLOR
            }
            .disposeOnViewModelDestroy()
    }

    private fun loadProperties(themeEntity: ThemeEntity?) {
        metaEvent.value = Meta(
            uuid = themeEntity?.uuid ?: UUID.randomUUID().toString(),
            name = themeEntity?.name ?: FALLBACK_META,
            author = themeEntity?.author ?: FALLBACK_META,
            description = themeEntity?.description ?: FALLBACK_META,
            isExternal = themeEntity?.isExternal ?: true,
            isPaid = themeEntity?.isPaid ?: true
        )
        propertiesEvent.value = listOf(
            PropertyItem(
                Property.TEXT_COLOR,
                themeEntity?.textColor ?: FALLBACK_COLOR,
                R.string.theme_property_text_color
            ),
            PropertyItem(
                Property.BACKGROUND_COLOR,
                themeEntity?.backgroundColor ?: FALLBACK_COLOR,
                R.string.theme_property_background_color
            ),
            PropertyItem(
                Property.GUTTER_COLOR,
                themeEntity?.gutterColor ?: FALLBACK_COLOR,
                R.string.theme_property_gutter_color
            ),
            PropertyItem(
                Property.GUTTER_DIVIDER_COLOR,
                themeEntity?.gutterDividerColor ?: FALLBACK_COLOR,
                R.string.theme_property_gutter_divider_color
            ),
            PropertyItem(
                Property.GUTTER_CURRENT_LINE_NUMBER_COLOR,
                themeEntity?.gutterCurrentLineNumberColor ?: FALLBACK_COLOR,
                R.string.theme_property_gutter_divider_current_line_number_color
            ),
            PropertyItem(
                Property.GUTTER_TEXT_COLOR,
                themeEntity?.gutterTextColor ?: FALLBACK_COLOR,
                R.string.theme_property_gutter_text_color
            ),
            PropertyItem(
                Property.SELECTED_LINE_COLOR,
                themeEntity?.selectedLineColor ?: FALLBACK_COLOR,
                R.string.theme_property_selected_line_color
            ),
            PropertyItem(
                Property.SELECTION_COLOR,
                themeEntity?.selectionColor ?: FALLBACK_COLOR,
                R.string.theme_property_selection_color
            ),
            PropertyItem(
                Property.SUGGESTION_MATCH_COLOR,
                themeEntity?.suggestionMatchColor ?: FALLBACK_COLOR,
                R.string.theme_property_suggestion_match_color
            ),
            PropertyItem(
                Property.SEARCH_BACKGROUND_COLOR,
                themeEntity?.searchBackgroundColor ?: FALLBACK_COLOR,
                R.string.theme_property_search_background_color
            ),
            PropertyItem(
                Property.DELIMITERS_BACKGROUND_COLOR,
                themeEntity?.delimitersBackgroundColor ?: FALLBACK_COLOR,
                R.string.theme_property_delimiters_background_color
            ),
            PropertyItem(
                Property.NUMBER_COLOR,
                themeEntity?.numberColor ?: FALLBACK_COLOR,
                R.string.theme_property_numbers_color
            ),
            PropertyItem(
                Property.OPERATOR_COLOR,
                themeEntity?.operatorColor ?: FALLBACK_COLOR,
                R.string.theme_property_operators_color
            ),
            PropertyItem(
                Property.BRACKET_COLOR,
                themeEntity?.bracketColor ?: FALLBACK_COLOR,
                R.string.theme_property_brackets_color
            ),
            PropertyItem(
                Property.KEYWORD_COLOR,
                themeEntity?.keywordColor ?: FALLBACK_COLOR,
                R.string.theme_property_keywords_color
            ),
            PropertyItem(
                Property.TYPE_COLOR,
                themeEntity?.typeColor ?: FALLBACK_COLOR,
                R.string.theme_property_types_color
            ),
            PropertyItem(
                Property.LANG_CONST_COLOR,
                themeEntity?.langConstColor ?: FALLBACK_COLOR,
                R.string.theme_property_lang_const_color
            ),
            PropertyItem(
                Property.METHOD_COLOR,
                themeEntity?.methodColor ?: FALLBACK_COLOR,
                R.string.theme_property_methods_color
            ),
            PropertyItem(
                Property.STRING_COLOR,
                themeEntity?.stringColor ?: FALLBACK_COLOR,
                R.string.theme_property_strings_color
            ),
            PropertyItem(
                Property.COMMENT_COLOR,
                themeEntity?.commentColor ?: FALLBACK_COLOR,
                R.string.theme_property_comments_color
            )
        )
    }

    class Factory(
        private val schedulersProvider: SchedulersProvider,
        private val preferenceHandler: PreferenceHandler,
        private val appDatabase: AppDatabase
    ) : ViewModelProvider.NewInstanceFactory() {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return when {
                modelClass === ThemesViewModel::class.java ->
                    ThemesViewModel(
                        schedulersProvider,
                        preferenceHandler,
                        appDatabase
                    ) as T
                else -> null as T
            }
        }
    }
}