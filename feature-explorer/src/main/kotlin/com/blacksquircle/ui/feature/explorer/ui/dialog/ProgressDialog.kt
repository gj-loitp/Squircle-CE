package com.blacksquircle.ui.feature.explorer.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.blacksquircle.ui.feature.explorer.R
import com.blacksquircle.ui.feature.explorer.data.utils.Operation
import com.blacksquircle.ui.feature.explorer.databinding.DialogProgressBinding
import com.blacksquircle.ui.feature.explorer.ui.viewmodel.ExplorerEvent
import com.blacksquircle.ui.feature.explorer.ui.viewmodel.ExplorerViewModel
import com.blacksquircle.ui.feature.explorer.ui.worker.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ProgressDialog : DialogFragment() {

    private val viewModel by activityViewModels<ExplorerViewModel>()
    private val navArgs by navArgs<ProgressDialogArgs>()

    private lateinit var binding: DialogProgressBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialDialog(requireContext()).show {
            customView(R.layout.dialog_progress)
            cancelOnTouchOutside(false)
            positiveButton(R.string.action_run_in_background)
            negativeButton(R.string.action_cancel)

            binding = DialogProgressBinding.bind(getCustomView())
            binding.progress.isIndeterminate = navArgs.totalCount <= 0
            binding.progress.max = navArgs.totalCount

            when (Operation.find(navArgs.operation)) {
                Operation.CREATE -> {
                    title(R.string.dialog_title_creating)
                    CreateFileWorker.observeJob(requireContext())
                        .flowWithLifecycle(lifecycle)
                        .onEach { fileModel ->
                            binding.progress.progress += 1
                            binding.textDetails.text = getString(R.string.message_creating, fileModel.path)
                            binding.textOfTotal.text = getString(
                                R.string.message_of_total,
                                binding.progress.progress,
                                navArgs.totalCount
                            )
                        }
                        .catch {
                            viewModel.obtainEvent(ExplorerEvent.Refresh)
                            dismiss()
                        }
                        .launchIn(lifecycleScope)
                }
                Operation.RENAME -> {
                    title(R.string.dialog_title_renaming)
                    RenameFileWorker.observeJob(requireContext())
                        .flowWithLifecycle(lifecycle)
                        .onEach { fileModel ->
                            binding.progress.progress += 1
                            binding.textDetails.text = getString(R.string.message_renaming, fileModel.path)
                            binding.textOfTotal.text = getString(
                                R.string.message_of_total,
                                binding.progress.progress,
                                navArgs.totalCount
                            )
                        }
                        .catch {
                            viewModel.obtainEvent(ExplorerEvent.Refresh)
                            dismiss()
                        }
                        .launchIn(lifecycleScope)
                }
                Operation.DELETE -> {
                    title(R.string.dialog_title_deleting)
                    DeleteFileWorker.observeJob(requireContext())
                        .flowWithLifecycle(lifecycle)
                        .onEach { fileModel ->
                            binding.progress.progress += 1
                            binding.textDetails.text = getString(R.string.message_deleting, fileModel.path)
                            binding.textOfTotal.text = getString(
                                R.string.message_of_total,
                                binding.progress.progress,
                                navArgs.totalCount
                            )
                        }
                        .catch {
                            viewModel.obtainEvent(ExplorerEvent.Refresh)
                            dismiss()
                        }
                        .launchIn(lifecycleScope)
                }
                Operation.COPY -> {
                    title(R.string.dialog_title_copying)
                    CopyFileWorker.observeJob(requireContext())
                        .flowWithLifecycle(lifecycle)
                        .onEach { fileModel ->
                            binding.progress.progress += 1
                            binding.textDetails.text = getString(R.string.message_copying, fileModel.path)
                            binding.textOfTotal.text = getString(
                                R.string.message_of_total,
                                binding.progress.progress,
                                navArgs.totalCount
                            )
                        }
                        .catch {
                            viewModel.obtainEvent(ExplorerEvent.Refresh)
                            dismiss()
                        }
                        .launchIn(lifecycleScope)
                }
                Operation.CUT -> {
                    title(R.string.dialog_title_copying)
                    CutFileWorker.observeJob(requireContext())
                        .flowWithLifecycle(lifecycle)
                        .onEach { fileModel ->
                            binding.progress.progress += 1
                            binding.textDetails.text = getString(R.string.message_copying, fileModel.path)
                            binding.textOfTotal.text = getString(
                                R.string.message_of_total,
                                binding.progress.progress,
                                navArgs.totalCount
                            )
                        }
                        .catch {
                            viewModel.obtainEvent(ExplorerEvent.Refresh)
                            dismiss()
                        }
                        .launchIn(lifecycleScope)
                }
                Operation.COMPRESS -> {
                    title(R.string.dialog_title_compressing)
                    CompressFileWorker.observeJob(requireContext())
                        .flowWithLifecycle(lifecycle)
                        .onEach { fileModel ->
                            binding.progress.progress += 1
                            binding.textDetails.text = getString(R.string.message_compressing, fileModel.path)
                            binding.textOfTotal.text = getString(
                                R.string.message_of_total,
                                binding.progress.progress,
                                navArgs.totalCount
                            )
                        }
                        .catch {
                            viewModel.obtainEvent(ExplorerEvent.Refresh)
                            dismiss()
                        }
                        .launchIn(lifecycleScope)
                }
                Operation.EXTRACT -> {
                    title(R.string.dialog_title_extracting)
                    ExtractFileWorker.observeJob(requireContext())
                        .flowWithLifecycle(lifecycle)
                        .onEach { fileModel ->
                            binding.progress.progress += 1
                            binding.textDetails.text = getString(R.string.message_extracting, fileModel.path)
                            binding.textOfTotal.text = getString(
                                R.string.message_of_total,
                                binding.progress.progress,
                                navArgs.totalCount
                            )
                        }
                        .catch {
                            viewModel.obtainEvent(ExplorerEvent.Refresh)
                            dismiss()
                        }
                        .launchIn(lifecycleScope)
                }
                Operation.NONE -> Unit
            }

            lifecycleScope.launchWhenStarted {
                val then = System.currentTimeMillis()
                formatElapsedTime(0L) // 00:00
                repeat(Int.MAX_VALUE) {
                    val difference = System.currentTimeMillis() - then
                    formatElapsedTime(difference)
                    delay(1000)
                }
            }
        }
    }

    private fun formatElapsedTime(timeInMillis: Long) {
        val formatter = SimpleDateFormat(getString(R.string.progress_time_format), Locale.getDefault())
        val elapsedTime = getString(
            R.string.message_elapsed_time,
            formatter.format(timeInMillis)
        )
        binding.textElapsedTime.text = elapsedTime
    }
}