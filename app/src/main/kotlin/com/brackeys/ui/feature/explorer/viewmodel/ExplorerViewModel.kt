/*
 * Copyright 2020 Brackeys IDE contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.brackeys.ui.feature.explorer.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.brackeys.ui.R
import com.brackeys.ui.data.settings.SettingsManager
import com.brackeys.ui.data.utils.containsFileModel
import com.brackeys.ui.data.utils.replaceList
import com.brackeys.ui.domain.repository.explorer.ExplorerRepository
import com.brackeys.ui.feature.base.viewmodel.BaseViewModel
import com.brackeys.ui.feature.explorer.utils.Operation
import com.brackeys.ui.filesystem.base.exception.*
import com.brackeys.ui.filesystem.base.model.FileModel
import com.brackeys.ui.filesystem.base.model.FileTree
import com.brackeys.ui.filesystem.base.model.PropertiesModel
import com.brackeys.ui.utils.event.SingleLiveEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ExplorerViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val explorerRepository: ExplorerRepository
) : BaseViewModel() {

    companion object {
        private const val TAG = "ExplorerViewModel"
    }

    // region EVENTS

    val toastEvent = SingleLiveEvent<Int>() // Отображение сообщений
    val showAppBarEvent = MutableLiveData<Boolean>() // Отображение вкладок
    val allowPasteFiles = MutableLiveData<Boolean>() // Отображение кнопки "Вставить"
    val stateLoadingFiles = MutableLiveData(true) // Индикатор загрузки файлов
    val stateNothingFound = MutableLiveData(false) // Сообщение об отсутствии файлов

    val filesUpdateEvent = SingleLiveEvent<Unit>() // Запрос на обновление списка файлов
    val selectAllEvent = SingleLiveEvent<Unit>() // Выделить все файлы
    val deselectAllEvent = SingleLiveEvent<Unit>() // Сбросить выделение со всех файлов
    val createEvent = SingleLiveEvent<Unit>() // Создать файл
    val copyEvent = SingleLiveEvent<Unit>() // Скопировать выделенные файлы
    val deleteEvent = SingleLiveEvent<Unit>() // Удалить выделенные файлы
    val cutEvent = SingleLiveEvent<Unit>() // Вырезать выделенные файлы
    val pasteEvent = SingleLiveEvent<Unit>() // Вставить скопированные файлы
    val openAsEvent = SingleLiveEvent<Unit>() // Открыть файл как
    val renameEvent = SingleLiveEvent<Unit>() // Переименовать файл
    val propertiesEvent = SingleLiveEvent<Unit>() // Свойства файла
    val copyPathEvent = SingleLiveEvent<Unit>() // Скопировать путь к файлу
    val archiveEvent = SingleLiveEvent<Unit>() // Архивация файлов в .zip

    val tabsEvent = MutableLiveData<List<FileModel>>() // Список вкладок
    val selectionEvent = MutableLiveData<List<FileModel>>() // Список выделенных файлов
    val progressEvent = SingleLiveEvent<Int>() // Прогресс выполнения операции
    val filesEvent = SingleLiveEvent<FileTree>() // Список файлов
    val searchEvent = SingleLiveEvent<List<FileModel>>() // Отфильтрованый список файлов
    val clickEvent = SingleLiveEvent<FileModel>() // Имитация нажатия на файл
    val propertiesOfEvent = SingleLiveEvent<PropertiesModel>() // Свойства файла

    // endregion EVENTS

    val tabsList = mutableListOf<FileModel>()
    val tempFiles = mutableListOf<FileModel>()

    var operation = Operation.COPY
    var currentJob: Job? = null

    var showHidden: Boolean
        get() = settingsManager.filterHidden
        set(value) {
            settingsManager.filterHidden = value
            filesUpdateEvent.call()
        }

    val viewMode: Int
        get() = Integer.parseInt(settingsManager.viewMode)

    var sortMode: Int
        get() = Integer.parseInt(settingsManager.sortMode)
        set(value) {
            settingsManager.sortMode = value.toString()
            filesUpdateEvent.call()
        }

    private val searchList = mutableListOf<FileModel>()

    fun provideDirectory(fileModel: FileModel?) {
        viewModelScope.launch {
            try {
                stateNothingFound.value = false
                stateLoadingFiles.value = true

                val fileTree = explorerRepository.fetchFiles(fileModel)
                if (!tabsList.containsFileModel(fileTree.parent)) {
                    tabsList.add(fileTree.parent)
                    tabsEvent.value = tabsList
                }
                searchList.replaceList(fileTree.children)
                filesEvent.value = fileTree

                stateLoadingFiles.value = false
                stateNothingFound.value = fileTree.children.isEmpty()
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
                when (e) {
                    is DirectoryExpectedException -> {
                        toastEvent.value = R.string.message_directory_expected
                    }
                    else -> {
                        toastEvent.value = R.string.message_unknown_exception
                    }
                }
            }
        }
    }

    fun searchFile(query: CharSequence) {
        val collection: MutableList<FileModel> = mutableListOf()
        val newQuery = query.toString().toLowerCase(Locale.getDefault())
        if (newQuery.isEmpty()) {
            collection.addAll(searchList)
        } else {
            for (row in searchList) {
                if (row.name.toLowerCase(Locale.getDefault()).contains(newQuery)) {
                    collection.add(row)
                }
            }
        }
        stateNothingFound.value = collection.isEmpty()
        searchEvent.value = collection
    }

    fun createFile(fileModel: FileModel) {
        viewModelScope.launch {
            try {
                val file = explorerRepository.createFile(fileModel)
                filesUpdateEvent.call()
                clickEvent.value = file
                toastEvent.value = R.string.message_done
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
                when (e) {
                    is FileAlreadyExistsException -> {
                        toastEvent.value = R.string.message_file_already_exists
                    }
                    else -> {
                        toastEvent.value = R.string.message_unknown_exception
                    }
                }
            }
        }
    }

    fun renameFile(fileModel: FileModel, newName: String) {
        viewModelScope.launch {
            try {
                explorerRepository.renameFile(fileModel, newName)
                filesUpdateEvent.call()
                toastEvent.value = R.string.message_done
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
                when (e) {
                    is FileNotFoundException -> {
                        toastEvent.value = R.string.message_file_not_found
                    }
                    is FileAlreadyExistsException -> {
                        toastEvent.value = R.string.message_file_already_exists
                    }
                    else -> {
                        toastEvent.value = R.string.message_unknown_exception
                    }
                }
            }
        }
    }

    fun propertiesOf(fileModel: FileModel) {
        viewModelScope.launch {
            try {
                propertiesOfEvent.value = explorerRepository.propertiesOf(fileModel)
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
                when (e) {
                    is FileNotFoundException -> {
                        toastEvent.value = R.string.message_file_not_found
                    }
                    else -> {
                        toastEvent.value = R.string.message_unknown_exception
                    }
                }
            }
        }
    }

    fun deleteFiles(fileModels: List<FileModel>) {
        currentJob = viewModelScope.launch {
            progressEvent.value = 0
            try {
                explorerRepository.deleteFiles(fileModels)
                    .onEach {
                        progressEvent.value = (progressEvent.value ?: 0) + 1
                    }
                    .onCompletion {
                        filesUpdateEvent.call()
                        toastEvent.value = R.string.message_done
                    }
                    .collect()
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
                progressEvent.value = Int.MAX_VALUE
                when (e) {
                    is FileNotFoundException -> {
                        toastEvent.value = R.string.message_file_not_found
                    }
                    else -> {
                        toastEvent.value = R.string.message_unknown_exception
                    }
                }
            }
        }
    }

    fun copyFiles(source: List<FileModel>, destPath: String) {
        currentJob = viewModelScope.launch {
            progressEvent.value = 0
            try {
                explorerRepository.copyFiles(source, destPath)
                    .onEach {
                        progressEvent.value = (progressEvent.value ?: 0) + 1
                    }
                    .onCompletion {
                        filesUpdateEvent.call()
                        toastEvent.value = R.string.message_done
                    }
                    .collect()
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
                progressEvent.value = Int.MAX_VALUE
                when (e) {
                    is FileNotFoundException -> {
                        toastEvent.value = R.string.message_file_not_found
                    }
                    is FileAlreadyExistsException -> {
                        toastEvent.value = R.string.message_file_already_exists
                    }
                    else -> {
                        toastEvent.value = R.string.message_unknown_exception
                    }
                }
            }
        }
    }

    fun cutFiles(source: List<FileModel>, destPath: String) {
        currentJob = viewModelScope.launch {
            progressEvent.value = 0
            try {
                explorerRepository.cutFiles(source, destPath)
                    .onEach {
                        progressEvent.value = (progressEvent.value ?: 0) + 1
                    }
                    .onCompletion {
                        filesUpdateEvent.call()
                        toastEvent.value = R.string.message_done
                    }
                    .collect()
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
                progressEvent.value = Int.MAX_VALUE
                when (e) {
                    is FileNotFoundException -> {
                        toastEvent.value = R.string.message_file_not_found
                    }
                    is FileAlreadyExistsException -> {
                        toastEvent.value = R.string.message_file_already_exists
                    }
                    else -> {
                        toastEvent.value = R.string.message_unknown_exception
                    }
                }
            }
        }
    }

    fun compressFiles(source: List<FileModel>, destPath: String, archiveName: String) {
        currentJob = viewModelScope.launch {
            progressEvent.value = 0
            try {
                val dest = FileModel(
                    name = archiveName,
                    path = "$destPath/$archiveName",
                    size = 0L,
                    lastModified = 0L,
                    isFolder = false,
                    isHidden = false
                )
                explorerRepository.compressFiles(source, dest)
                    .onEach {
                        progressEvent.value = (progressEvent.value ?: 0) + 1
                    }
                    .onCompletion {
                        filesUpdateEvent.call()
                        toastEvent.value = R.string.message_done
                    }
                    .collect()
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
                progressEvent.value = Int.MAX_VALUE
                when (e) {
                    is FileNotFoundException -> {
                        toastEvent.value = R.string.message_file_not_found
                    }
                    is FileAlreadyExistsException -> {
                        toastEvent.value = R.string.message_file_already_exists
                    }
                    else -> {
                        toastEvent.value = R.string.message_unknown_exception
                    }
                }
            }
        }
    }

    fun extractAll(source: FileModel, destPath: String) {
        currentJob = viewModelScope.launch {
            progressEvent.value = 0
            try {
                val dest = FileModel(
                    name = "whatever",
                    path = destPath,
                    size = 0L,
                    lastModified = 0L,
                    isFolder = false,
                    isHidden = false
                )
                explorerRepository.extractAll(source, dest)
                progressEvent.value = (progressEvent.value ?: 0) + 1 // FIXME у диалога всегда будет 1 файл
                filesUpdateEvent.call()
                toastEvent.value = R.string.message_done
            } catch (e: Exception) {
                Log.e(TAG, e.message, e)
                progressEvent.value = Int.MAX_VALUE
                when (e) {
                    is FileNotFoundException -> {
                        toastEvent.value = R.string.message_file_not_found
                    }
                    is FileAlreadyExistsException -> {
                        toastEvent.value = R.string.message_file_already_exists
                    }
                    is UnsupportedArchiveException -> {
                        toastEvent.value = R.string.message_unsupported_archive
                    }
                    is EncryptedArchiveException -> {
                        toastEvent.value = R.string.message_encrypted_archive
                    }
                    is SplitArchiveException -> {
                        toastEvent.value = R.string.message_split_archive
                    }
                    is InvalidArchiveException -> {
                        toastEvent.value = R.string.message_invalid_archive
                    }
                    else -> {
                        toastEvent.value = R.string.message_unknown_exception
                    }
                }
            }
        }
    }
}