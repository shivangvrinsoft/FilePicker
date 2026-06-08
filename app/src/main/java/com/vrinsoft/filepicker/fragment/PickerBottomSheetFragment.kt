package com.vrinsoft.filepicker.fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.vrinsoft.filepicker.R
import com.vrinsoft.filepicker.databinding.FragmentPickerSheetBinding
import com.vrinsoft.filepicker.databinding.ItemSheetOptionBinding

/**
 * PickerBottomSheetFragment
 * Screen 2 — Modal bottom sheet with a 2×2 source-picker grid.
 *
 * Usage:
 *   PickerBottomSheetFragment.newInstance(listener)
 *       .show(supportFragmentManager, PickerBottomSheetFragment.TAG)
 */
class PickerBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentPickerSheetBinding? = null
    private val binding get() = _binding!!

    var listener: PickerSheetListener? = null

    interface PickerSheetListener {
        fun onCameraSelected()
        fun onVideoSelected()
        fun onGallerySelected()
        fun onDocumentSelected()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPickerSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupOptions()
        binding.btnCancel.setOnClickListener { dismiss() }
    }

    private fun setupOptions() {
        // Camera
        ItemSheetOptionBinding.bind(binding.optionCamera.root).apply {
            ivSheetIcon.setImageResource(R.drawable.ic_camera)
            cvSheetIconBg.setCardBackgroundColor(color(R.color.icon_bg_blue))
            ivSheetIcon.imageTintList = ColorStateList.valueOf(color(R.color.icon_tint_blue))
            tvSheetLabel.setText(R.string.option_camera)
            root.setOnClickListener { listener?.onCameraSelected(); dismiss() }
        }

        // Video
        ItemSheetOptionBinding.bind(binding.optionVideo.root).apply {
            ivSheetIcon.setImageResource(R.drawable.ic_video)
            cvSheetIconBg.setCardBackgroundColor(color(R.color.icon_bg_coral))
            ivSheetIcon.imageTintList = ColorStateList.valueOf(color(R.color.icon_tint_coral))
            tvSheetLabel.setText(R.string.option_video)
            root.setOnClickListener { listener?.onVideoSelected(); dismiss() }
        }

        // Gallery
        ItemSheetOptionBinding.bind(binding.optionGallery.root).apply {
            ivSheetIcon.setImageResource(R.drawable.ic_photo)
            cvSheetIconBg.setCardBackgroundColor(color(R.color.icon_bg_teal))
            ivSheetIcon.imageTintList = ColorStateList.valueOf(color(R.color.icon_tint_teal))
            tvSheetLabel.setText(R.string.option_gallery)
            root.setOnClickListener { listener?.onGallerySelected(); dismiss() }
        }

        // Document
        ItemSheetOptionBinding.bind(binding.optionDocument.root).apply {
            ivSheetIcon.setImageResource(R.drawable.ic_file_text)
            cvSheetIconBg.setCardBackgroundColor(color(R.color.icon_bg_amber))
            ivSheetIcon.imageTintList = ColorStateList.valueOf(color(R.color.icon_tint_amber))
            tvSheetLabel.setText(R.string.option_documents)
            root.setOnClickListener { listener?.onDocumentSelected(); dismiss() }
        }
    }

    private fun color(res: Int) = ContextCompat.getColor(requireContext(), res)

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PickerBottomSheetFragment"

        fun newInstance(listener: PickerSheetListener): PickerBottomSheetFragment {
            return PickerBottomSheetFragment().apply {
                this.listener = listener
            }
        }
    }
}