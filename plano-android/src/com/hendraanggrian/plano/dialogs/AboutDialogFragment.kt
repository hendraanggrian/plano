package com.hendraanggrian.plano.dialogs

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hendraanggrian.plano.BuildConfig
import com.hendraanggrian.plano.R

class AboutDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        MaterialAlertDialogBuilder(context!!)
            .setIcon(R.mipmap.ic_launcher)
            .setTitle("${BuildConfig.NAME} ${BuildConfig.VERSION_NAME}")
            .setMessage(getString(R.string._about))
            .setPositiveButton(getString(R.string.btn_close)) { _, _ -> }
            .setNegativeButton(getString(R.string.btn_homepage)) { _, _ ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.HOMEPAGE)))
            }
            .create()
}
