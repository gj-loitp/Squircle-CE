package com.lightteam.modpeide.ui.settings.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.lightteam.modpeide.data.feature.language.LanguageProvider
import com.lightteam.modpeide.data.feature.scheme.Theme
import com.lightteam.modpeide.databinding.ItemThemeBinding
import com.lightteam.modpeide.ui.base.adapters.BaseViewHolder
import com.lightteam.modpeide.ui.settings.customview.CodeView
import com.lightteam.modpeide.utils.extensions.isUltimate

class ThemeAdapter(
    private val themeInteractor: ThemeInteractor
) : ListAdapter<Theme, ThemeAdapter.ThemeViewHolder>(diffCallback) {

    companion object {
        private val diffCallback = object : DiffUtil.ItemCallback<Theme>() {
            override fun areItemsTheSame(oldItem: Theme, newItem: Theme): Boolean {
                return oldItem.uuid == newItem.uuid
            }
            override fun areContentsTheSame(oldItem: Theme, newItem: Theme): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        return ThemeViewHolder.create(parent, themeInteractor)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ThemeViewHolder(
        private val binding: ItemThemeBinding,
        private val themeInteractor: ThemeInteractor
    ) : BaseViewHolder<Theme>(binding.root) {

        companion object {
            fun create(parent: ViewGroup, themeInteractor: ThemeInteractor): ThemeViewHolder {
                val inflater = LayoutInflater.from(parent.context)
                val binding = ItemThemeBinding.inflate(inflater, parent, false)
                return ThemeViewHolder(binding, themeInteractor)
            }
        }

        private lateinit var theme: Theme

        init {
            binding.actionSelect.setOnClickListener {
                themeInteractor.selectTheme(theme)
            }
            binding.actionInfo.setOnClickListener {
                themeInteractor.openInfo(theme)
            }
            itemView.setOnClickListener {
                if (!binding.actionSelect.isEnabled) {
                    themeInteractor.selectTheme(theme)
                }
            }
        }

        override fun bind(item: Theme) {
            theme = item
            binding.itemTitle.text = item.name
            binding.itemSubtitle.text = item.author

            binding.card.setCardBackgroundColor(item.colorScheme.backgroundColor)
            binding.editor.doOnPreDraw {
                binding.editor.theme = theme
                binding.editor.language = LanguageProvider.provide(".js")
            }
            binding.editor.text = CodeView.CODE_PREVIEW

            val isUltimate = itemView.context.isUltimate()
            binding.actionInfo.isEnabled = !item.isPaid || isUltimate
            binding.actionSelect.isEnabled = !item.isPaid || isUltimate
        }
    }

    interface ThemeInteractor {
        fun selectTheme(theme: Theme)
        fun openInfo(theme: Theme)
    }
}