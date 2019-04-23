package com.hendraanggrian.plano

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.get
import androidx.lifecycle.observe
import com.hendraanggrian.bundler.Bundler
import com.hendraanggrian.defaults.BindDefault
import com.hendraanggrian.defaults.DefaultsSaver
import com.hendraanggrian.defaults.bindDefaults
import com.hendraanggrian.plano.dialog.AboutDialogFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private var clearMenu: MenuItem? = null
    private lateinit var adapter: MainAdapter
    private lateinit var saver: DefaultsSaver

    @JvmField @BindDefault("media_width") var mediaWidth = 0f
    @JvmField @BindDefault("media_height") var mediaHeight = 0f
    @JvmField @BindDefault("trim_width") var trimWidth = 0f
    @JvmField @BindDefault("trim_height") var trimHeight = 0f
    @JvmField @BindDefault("bleed") var bleed = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        saver = bindDefaults()

        viewModel = ViewModelProviders.of(this).get()
        viewModel.emptyData.observe(this) { isEmpty ->
            when {
                isEmpty -> {
                    emptyText.visibility = View.VISIBLE
                    clearMenu?.isVisible = false
                    appBar.setExpanded(true)
                    mediaWidthText.requestFocus()
                }
                else -> {
                    emptyText.visibility = View.GONE
                    clearMenu?.isVisible = true
                    recyclerView.scrollToPosition(adapter.size - 1)
                }
            }
        }

        mediaWidthText.setText("$mediaWidth")
        mediaHeightText.setText("$mediaHeight")
        trimWidthText.setText("$trimWidth")
        trimHeightText.setText("$trimHeight")
        bleedText.setText("$bleed")

        adapter = MainAdapter(viewModel.emptyData)
        recyclerView.adapter = adapter
        fab.setOnClickListener {
            when {
                mediaWidthText.value <= 0 || mediaHeightText.value <= 0 ||
                    trimWidthText.value <= 0 || trimHeightText.value <= 0 ->
                    recyclerView.snackbar(getString(R.string._incomplete))
                else -> {
                    mediaWidth = mediaWidthText.value
                    mediaHeight = mediaHeightText.value
                    trimWidth = trimWidthText.value
                    trimHeight = trimHeightText.value
                    bleed = bleedText.value
                    saver.saveAsync()

                    getSystemService<InputMethodManager>()!!
                        .hideSoftInputFromWindow(fab.applicationWindowToken, 0)
                    adapter.put(
                        Plano.calculate(
                            mediaWidth.toDouble(), mediaHeight.toDouble(),
                            trimWidth.toDouble(), trimHeight.toDouble(),
                            bleed.toDouble()
                        )
                    )
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        clearMenu = menu.findItem(R.id.clear)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.clear -> {
                val temp = adapter.toList()
                adapter.removeAll()
                recyclerView.snackbar(
                    getString(R.string._boxes_cleared),
                    getString(R.string.btn_undo)
                ) {
                    adapter.putAll(temp)
                }
            }
            R.id.checkForUpdate -> GlobalScope.launch(Dispatchers.Main) {
                val release = withContext(Dispatchers.IO) {
                    GitHubApi.getLatestRelease()
                }
                when {
                    release.isNewerThan(BuildConfig.VERSION_NAME) -> recyclerView.longSnackbar(
                        getString(R.string._update_available).format(BuildConfig.VERSION_NAME),
                        getString(R.string.btn_download)
                    ) {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(release.assets.first {
                                    it.name.endsWith("apk")
                                }.downloadUrl)
                            )
                        )
                    }
                    else -> recyclerView.longSnackbar(getString(R.string._update_unavailable))
                }
            }
            R.id.about -> AboutDialogFragment()
                .also {
                    it.arguments = Bundler.wrapExtras(AboutDialogFragment::class.java, this)
                }
                .show(supportFragmentManager, null)
        }
        return super.onOptionsItemSelected(item)
    }

    private val TextView.value: Float get() = text?.toString()?.toFloatOrNull() ?: 0f
}