package com.vrinsoft.filepicker.activity

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.vrinsoft.filepicker.fragment.PickerBottomSheetFragment
import com.vrinsoft.filepicker.model.ResultFileItem

class FilePicker private constructor(
    private val context: Context,
    private val launcher: ActivityResultLauncher<Intent>,
    private val maxSelection: Int
) {

    fun launch() {
        val intent = ResultsActivity.newIntent(context, maxSelection)
        launcher.launch(intent)
    }

    class Builder(private val context: Context) {
        private var launcher: ActivityResultLauncher<Intent>? = null
        private var maxSelection: Int = Int.MAX_VALUE

        fun launcher(launcher: ActivityResultLauncher<Intent>) = apply {
            this.launcher = launcher
        }

        fun maxSelection(max: Int) = apply {
            this.maxSelection = max
        }

        fun launch() {
            requireNotNull(launcher) { "Call .launcher() before .launch()" }
            FilePicker(context, launcher!!, maxSelection).launch()
        }

        fun showBottomSheet(activity: FragmentActivity, delegate: FilePickerDelegate) {
            requireNotNull(launcher) { "Call .launcher() before .showBottomSheet()" }

            PickerBottomSheetFragment.newInstance(
                object : PickerBottomSheetFragment.PickerSheetListener {
                    override fun onCameraSelected()   { delegate.launchCamera() }
                    override fun onVideoSelected()    { delegate.launchVideo() }
                    override fun onGallerySelected()  { delegate.launchGallery() }
                    override fun onDocumentSelected() { delegate.launchDocument() }
                }
            ).show(activity.supportFragmentManager, PickerBottomSheetFragment.TAG)
        }
    }

    companion object {
        fun registerForResult(
            activity: ComponentActivity,
            onResult: (List<ResultFileItem>) -> Unit
        ): ActivityResultLauncher<Intent> {
            return activity.registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == ComponentActivity.RESULT_OK) {
                    val files: List<ResultFileItem> = result.data
                        ?.getParcelableArrayListExtra(
                            ResultsActivity.RESULT_SELECTED_FILES
                        ) ?: emptyList()
                    onResult(files)
                } else {
                    onResult(emptyList())
                }
            }
        }
    }
}