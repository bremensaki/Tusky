/* Copyright 2019 Tusky Contributors
 *
 * This file is a part of Tusky.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Tusky is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Tusky; if not,
 * see <http://www.gnu.org/licenses>. */

package com.keylesspalace.tusky.components.compose.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.keylesspalace.tusky.R
import com.keylesspalace.tusky.databinding.DialogImageDescriptionBinding

// https://github.com/tootsuite/mastodon/blob/c6904c0d3766a2ea8a81ab025c127169ecb51373/app/models/media_attachment.rb#L32
private const val MEDIA_DESCRIPTION_CHARACTER_LIMIT = 1500

class CaptionDialog : DialogFragment() {
    private lateinit var listener: Listener
    private lateinit var input: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        val binding = DialogImageDescriptionBinding.inflate(layoutInflater)

        input = binding.imageDescriptionText
        val imageView = binding.imageDescriptionView
        imageView.maxZoom = 6f

        input.hint = resources.getQuantityString(
            R.plurals.hint_describe_for_visually_impaired,
            MEDIA_DESCRIPTION_CHARACTER_LIMIT,
            MEDIA_DESCRIPTION_CHARACTER_LIMIT
        )
        input.filters = arrayOf(InputFilter.LengthFilter(MEDIA_DESCRIPTION_CHARACTER_LIMIT))
        input.setText(arguments?.getString(EXISTING_DESCRIPTION_ARG))

        val localId = arguments?.getInt(LOCAL_ID_ARG) ?: error("Missing localId")
        val dialog = AlertDialog.Builder(context)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                listener.onUpdateDescription(localId, input.text.toString())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        isCancelable = true
        val window = dialog.window
        window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val previewUri = BundleCompat.getParcelable(requireArguments(), PREVIEW_URI_ARG, Uri::class.java) ?: error("Preview Uri is null")
        // Load the image and manually set it into the ImageView because it doesn't have a fixed size.
        Glide.with(this)
            .load(previewUri)
            .downsample(DownsampleStrategy.CENTER_INSIDE)
            .into(object : CustomTarget<Drawable>(4096, 4096) {
                override fun onLoadCleared(placeholder: Drawable?) {
                    imageView.setImageDrawable(placeholder)
                }

                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable>?
                ) {
                    imageView.setImageDrawable(resource)
                }
            })

        return dialog
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(DESCRIPTION_KEY, input.text.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        savedInstanceState?.getString(DESCRIPTION_KEY)?.let {
            input.setText(it)
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? Listener ?: error("Activity is not ComposeCaptionDialog.Listener")
    }

    interface Listener {
        fun onUpdateDescription(localId: Int, description: String)
    }

    companion object {
        fun newInstance(
            localId: Int,
            existingDescription: String?,
            previewUri: Uri
        ) = CaptionDialog().apply {
            arguments = bundleOf(
                LOCAL_ID_ARG to localId,
                EXISTING_DESCRIPTION_ARG to existingDescription,
                PREVIEW_URI_ARG to previewUri
            )
        }

        private const val DESCRIPTION_KEY = "description"
        private const val EXISTING_DESCRIPTION_ARG = "existing_description"
        private const val PREVIEW_URI_ARG = "preview_uri"
        private const val LOCAL_ID_ARG = "local_id"
    }
}
