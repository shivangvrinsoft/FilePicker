package com.vrinsoft.filepickerdemo

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.vrinsoft.filepickerdemo.databinding.ActivitySampleBinding
import com.vrinsoft.filepicker.activity.FilePicker
import com.vrinsoft.filepicker.activity.FilePickerDelegate
import com.vrinsoft.filepicker.model.ResultFileItem

class SampleActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySampleBinding
    private val adapter = SelectedFilesAdapter()

    // Create delegate ONCE at class level — registers launchers before STARTED
    private val filePickerDelegate by lazy {
        FilePickerDelegate(
            activity     = this,
            maxSelection = 5,
            onResult     = { files -> onFilesPicked(files) }  // wire callback here
        )
    }

    private val filePickerLauncher = FilePicker.registerForResult(this) { files ->
        onFilesPicked(files)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySampleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        filePickerDelegate // force init

        binding.tvSelectedFiles.layoutManager = LinearLayoutManager(this)
        binding.tvSelectedFiles.adapter = adapter

        showEmptyState() // show empty state on first launch

        setupCta()
    }

    private fun setupCta() {
        binding.btnOpenPicker.setOnClickListener {
            FilePicker.Builder(this)
                .launcher(filePickerLauncher)
                .maxSelection(5)
                .showBottomSheet(this, filePickerDelegate)
        }
    }

    private fun onFilesPicked(files: List<ResultFileItem>) {
        if (files.isEmpty()) {
            showEmptyState()
            return
        }
        adapter.submitList(files)
        binding.tvSelectedFiles.visibility = View.VISIBLE
        binding.layoutEmptyState.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.tvSelectedFiles.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.VISIBLE
    }
}