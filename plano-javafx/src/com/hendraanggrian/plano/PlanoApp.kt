package com.hendraanggrian.plano

import com.hendraanggrian.plano.control.AboutDialog
import com.hendraanggrian.plano.control.AdaptableRoundButton
import com.hendraanggrian.plano.control.DoubleField
import com.hendraanggrian.plano.control.InfoButton
import com.hendraanggrian.plano.control.MediaPane
import com.hendraanggrian.plano.control.MoreButton
import com.hendraanggrian.plano.control.MorePaperButton
import com.hendraanggrian.plano.control.RoundButton
import com.hendraanggrian.plano.control.SimpleRoundButton
import com.hendraanggrian.plano.control.TextDialog
import com.hendraanggrian.plano.control.Toolbar
import com.hendraanggrian.plano.control.TrimPane
import com.hendraanggrian.plano.control.setBorder
import com.hendraanggrian.prefs.BindPref
import com.hendraanggrian.prefs.Prefs
import com.hendraanggrian.prefs.PrefsSaver
import com.hendraanggrian.prefs.jvm.setDebug
import com.hendraanggrian.prefs.jvm.userRoot
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXCheckBox
import javafx.application.Application
import javafx.application.Platform
import javafx.geometry.HPos
import javafx.scene.control.Button
import javafx.scene.control.CheckMenuItem
import javafx.scene.control.ScrollPane
import javafx.scene.control.ToggleGroup
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.FlowPane
import javafx.scene.layout.GridPane
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.stage.Stage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ktfx.bindings.buildBooleanBinding
import ktfx.bindings.eq
import ktfx.bindings.minus
import ktfx.booleanPropertyOf
import ktfx.collections.isEmptyBinding
import ktfx.controls.gap
import ktfx.controls.paddingAll
import ktfx.coroutines.listener
import ktfx.coroutines.onAction
import ktfx.coroutines.onHiding
import ktfx.coroutines.snapshot
import ktfx.doublePropertyOf
import ktfx.inputs.plus
import ktfx.jfoenix.jfxSnackbar
import ktfx.layouts.anchorPane
import ktfx.layouts.borderPane
import ktfx.layouts.checkMenuItem
import ktfx.layouts.circle
import ktfx.layouts.flowPane
import ktfx.layouts.gridPane
import ktfx.layouts.hbox
import ktfx.layouts.imageView
import ktfx.layouts.label
import ktfx.layouts.menu
import ktfx.layouts.menuBar
import ktfx.layouts.menuItem
import ktfx.layouts.radioMenuItem
import ktfx.layouts.region
import ktfx.layouts.rowConstraints
import ktfx.layouts.scene
import ktfx.layouts.scrollPane
import ktfx.layouts.separatorMenuItem
import ktfx.layouts.stackPane
import ktfx.layouts.text
import ktfx.layouts.textFlow
import ktfx.layouts.vbox
import ktfx.runLater
import ktfx.swing.toSwingImage
import ktfx.windows.setMinSize
import org.apache.commons.lang3.SystemUtils
import java.awt.Desktop
import java.net.URI
import java.util.ResourceBundle
import javax.imageio.ImageIO

class PlanoApp : Application(), Resources {

    companion object {
        const val DURATION_SHORT = 3000L
        const val DURATION_LONG = 6000L

        const val SCALE_SMALL = 2.0
        const val SCALE_BIG = 4.0

        const val BUTTON_OPACITY = 0.54

        // Material Orange 500
        val COLOR_YELLOW: Color = Color.web("#ffb300")
        val COLOR_YELLOW_LIGHT: Color = Color.web("#ffe54c")
        // Material Red 500
        val COLOR_RED: Color = Color.web("#f44336")
        val COLOR_RED_LIGHT: Color = Color.web("#ff7961")
        // Others
        val COLOR_BORDER: Color = Color.web("#c8c8c8")

        @JvmStatic
        fun main(args: Array<String>) = ktfx.launch<PlanoApp>(*args)
    }

    private val scaleProperty = doublePropertyOf(SCALE_SMALL)
    private val expandedProperty = booleanPropertyOf().apply {
        bind(scaleProperty eq SCALE_BIG)
        listener { _, _, newValue ->
            expandedMenu.isSelected = newValue
            outputPane.children.forEach {
                val pane = it as Pane
                val gridPane = pane.children[0] as GridPane
                val anchorPane = gridPane.children[0] as AnchorPane
                anchorPane.children.forEachIndexed { index, node ->
                    when (index) {
                        0 -> {
                            node as Pane
                            pane.prefWidth = gridPane.prefWidth
                            pane.prefHeight = gridPane.prefHeight
                        }
                        else -> @Suppress("UNCHECKED_CAST") {
                            val (x, y) = node.userData as Pair<Double, Double>
                            AnchorPane.setLeftAnchor(node, x * scaleProperty.value)
                            AnchorPane.setTopAnchor(node, y * scaleProperty.value)
                        }
                    }
                }
            }
        }
    }
    private val filledProperty = booleanPropertyOf().apply {
        listener { _, _, newValue ->
            fillMenu.isSelected = newValue
        }
    }
    private val thickProperty = booleanPropertyOf().apply {
        listener { _, _, newValue ->
            thickMenu.isSelected = newValue
        }
    }

    private val mediaWidthField = DoubleField().apply { onAction { calculateButton.fire() } }
    private val mediaHeightField = DoubleField().apply { onAction { calculateButton.fire() } }
    private val trimWidthField = DoubleField().apply { onAction { calculateButton.fire() } }
    private val trimHeightField = DoubleField().apply { onAction { calculateButton.fire() } }
    private val bleedField = DoubleField().apply { onAction { calculateButton.fire() } }
    private val allowFlipCheck = JFXCheckBox()

    private lateinit var expandedMenu: CheckMenuItem
    private lateinit var fillMenu: CheckMenuItem
    private lateinit var thickMenu: CheckMenuItem
    private lateinit var rootPane: StackPane
    private lateinit var calculateButton: Button
    private lateinit var outputPane: FlowPane

    private lateinit var saver: PrefsSaver
    override lateinit var resourceBundle: ResourceBundle

    @JvmField @BindPref("language") var language = Language.ENGLISH.code
    @JvmField @BindPref("is_expanded") var isExpanded = false
    @JvmField @BindPref("is_filled") var isFilled = false
    @JvmField @BindPref("is_thick") var isThick = false
    @JvmField @BindPref("media_width") var mediaWidth = 0.0
    @JvmField @BindPref("media_height") var mediaHeight = 0.0
    @JvmField @BindPref("trim_width") var trimWidth = 0.0
    @JvmField @BindPref("trim_height") var trimHeight = 0.0
    @JvmField @BindPref("bleed") var bleed = 0.0
    @JvmField @BindPref("allow_flip") var allowFlip = false

    override fun init() {
        Plano.DEBUG = BuildConfig.DEBUG
        Prefs.setDebug(BuildConfig.DEBUG)
        saver = Prefs.userRoot().node(BuildConfig.GROUP.replace('.', '/')).bind(this)
        resourceBundle = Language.ofCode(language).toResourcesBundle()
    }

    override fun start(stage: Stage) {
        stage.title = BuildConfig.NAME
        stage.setMinSize(750.0, 500.0)
        stage.onHiding {
            isExpanded = expandedProperty.value
            isFilled = filledProperty.value
            isThick = thickProperty.value
            saver.save()
        }
        stage.scene = scene {
            stylesheets.addAll(
                PlanoApp::class.java.getResource(R.style.plano).toExternalForm(),
                PlanoApp::class.java.getResource(R.style.plano_font).toExternalForm()
            )
            rootPane = stackPane {
                vbox {
                    menuBar {
                        "File" {
                            menu(getString(R.string.language)) {
                                val group = ToggleGroup()
                                Language.values().forEach { lang ->
                                    radioMenuItem(lang.toLocale().displayLanguage) {
                                        toggleGroup = group
                                        isSelected = lang.code == language
                                        onAction {
                                            language = lang.code
                                            TextDialog(
                                                this@PlanoApp,
                                                rootPane,
                                                R.string.please_restart,
                                                R.string._please_restart
                                            ).apply { setOnDialogClosed { stage.close() } }
                                                .show()
                                        }
                                    }
                                }
                            }
                            separatorMenuItem()
                            menuItem(getString(R.string.quit)) {
                                accelerator = KeyCombination.SHORTCUT_DOWN + KeyCode.Q
                                onAction { Platform.exit() }
                            }
                        }
                        "Edit" {
                            menuItem(getString(R.string.clear)) {
                                onAction { clear() }
                                runLater { disableProperty().bind(outputPane.children.isEmptyBinding) }
                            }
                        }
                        "View" {
                            expandedMenu = checkMenuItem(getString(R.string.expand)) {
                                accelerator = KeyCombination.SHORTCUT_DOWN + KeyCode.DIGIT1
                                onAction { toggleExpand() }
                            }
                            fillMenu = checkMenuItem(getString(R.string.fill_background)) {
                                accelerator = KeyCombination.SHORTCUT_DOWN + KeyCode.DIGIT2
                                onAction { toggleFill() }
                            }
                            thickMenu = checkMenuItem(getString(R.string.thicken_border)) {
                                accelerator = KeyCombination.SHORTCUT_DOWN + KeyCode.DIGIT3
                                onAction { toggleThick() }
                            }
                            separatorMenuItem()
                            menuItem(getString(R.string.reset)) {
                                onAction {
                                    scaleProperty.value = SCALE_SMALL
                                    filledProperty.value = false
                                    thickProperty.value = false
                                }
                            }
                        }
                        "Window" {
                            menuItem(getString(R.string.minimize)) {
                                accelerator = KeyCombination.SHORTCUT_DOWN + KeyCode.M
                                onAction { stage.isIconified = true }
                            }
                            menuItem(getString(R.string.zoom)) {
                                onAction { stage.isMaximized = true }
                            }
                        }
                        "Help" {
                            menuItem(getString(R.string.about)) {
                                onAction { this@stackPane.showAbout() }
                            }
                            menuItem(getString(R.string.check_for_update)) {
                                onAction { checkForUpdate() }
                            }
                        }
                    }.isUseSystemMenuBar = SystemUtils.IS_OS_MAC
                    Toolbar().apply {
                        leftItems {
                            imageView(R.image.ic_launcher)
                            region { prefWidth = 12.0 }
                            label(BuildConfig.NAME) { styleClass.addAll("display2", "dark") }
                        }
                        rightItems {
                            RoundButton(
                                24,
                                getString(R.string.clear),
                                R.image.btn_clear
                            ).apply {
                                onAction { clear() }
                                runLater { disableProperty().bind(outputPane.children.isEmptyBinding) }
                            }.add()
                            AdaptableRoundButton(
                                24,
                                expandedProperty,
                                getString(R.string.shrink),
                                getString(R.string.expand),
                                R.image.btn_scale_expand,
                                R.image.btn_scale_shrink
                            ).apply {
                                onAction { toggleExpand() }
                            }.add()
                            AdaptableRoundButton(
                                24,
                                filledProperty,
                                getString(R.string.unfill_background),
                                getString(R.string.fill_background),
                                R.image.btn_background_fill,
                                R.image.btn_background_unfill
                            ).apply {
                                onAction { toggleFill() }
                            }.add()
                            AdaptableRoundButton(
                                24,
                                thickProperty,
                                getString(R.string.unthicken_border),
                                getString(R.string.thicken_border),
                                R.image.btn_border_thick,
                                R.image.btn_border_thin
                            ).apply {
                                onAction { toggleThick() }
                            }.add()
                        }
                    }.add()
                    hbox {
                        gridPane {
                            setBorder(COLOR_BORDER)
                            paddingAll = 20
                            gap = 10
                            var row = 0

                            rowConstraints {
                                constraints() // description text
                                repeat(4) { constraints { prefHeight = 32.0 } } // input form
                            }

                            text(getString(R.string._desc)) {
                                wrappingWidth = 200.0
                            } row row++ col 0 colSpans 6

                            circle(radius = 4.0, fill = COLOR_YELLOW) row row col 0
                            label(getString(R.string.media_box)) row row col 1
                            mediaWidthField.run {
                                value = mediaWidth
                                add() row row col 2
                            }
                            label("x") row row col 3
                            mediaHeightField.run {
                                value = mediaHeight
                                add() row row col 4
                            }
                            MorePaperButton(
                                this@PlanoApp,
                                mediaWidthField,
                                mediaHeightField
                            ).add() row row++ col 5

                            circle(radius = 4.0, fill = COLOR_RED) row row col 0
                            label(getString(R.string.trim_box)) row row col 1
                            trimWidthField.run {
                                value = trimWidth
                                add() row row col 2
                            }
                            label("x") row row col 3
                            trimHeightField.run {
                                value = trimHeight
                                add() row row col 4
                            }
                            MorePaperButton(
                                this@PlanoApp,
                                trimWidthField,
                                trimHeightField
                            ).add() row row++ col 5

                            label(getString(R.string.bleed)) row row col 1
                            bleedField.run {
                                value = bleed
                                add() row row col 2
                            }
                            InfoButton(
                                this@PlanoApp, this@stackPane,
                                R.string.bleed, R.string._bleed
                            ).add() row row++ col 5

                            label(getString(R.string.allow_flip)) row row col 1
                            allowFlipCheck.run {
                                isSelected = allowFlip
                                add() row row col 2
                            }
                            InfoButton(
                                this@PlanoApp, this@stackPane,
                                R.string.allow_flip, R.string._allow_flip
                            ).add() row row++ col 5

                            row++
                            row++
                            calculateButton = SimpleRoundButton(
                                24,
                                getString(R.string.calculate),
                                R.image.btn_send
                            ).apply {
                                styleClass += "raised"
                                buttonType = JFXButton.ButtonType.RAISED
                                disableProperty().bind(buildBooleanBinding(
                                    mediaWidthField.textProperty(),
                                    mediaHeightField.textProperty(),
                                    trimWidthField.textProperty(),
                                    trimHeightField.textProperty()
                                ) {
                                    when {
                                        mediaWidthField.value <= 0.0 || mediaHeightField.value <= 0.0 -> true
                                        trimWidthField.value <= 0.0 || trimHeightField.value <= 0.0 -> true
                                        else -> false
                                    }
                                })
                                onAction {
                                    mediaWidth = mediaWidthField.value
                                    mediaHeight = mediaHeightField.value
                                    trimWidth = trimWidthField.value
                                    trimHeight = trimHeightField.value
                                    bleed = bleedField.value
                                    allowFlip = allowFlipCheck.isSelected

                                    outputPane.children.add(0, ktfx.layouts.pane {
                                        gridPane {
                                            paddingAll = 10
                                            gap = 10
                                            val size = Plano.calculate(
                                                mediaWidth, mediaHeight,
                                                trimWidth, trimHeight,
                                                bleed, allowFlip
                                            )
                                            anchorPane {
                                                MediaPane(
                                                    size,
                                                    scaleProperty,
                                                    filledProperty,
                                                    thickProperty
                                                ).add()
                                                size.trimSizes.forEach {
                                                    TrimPane(
                                                        it,
                                                        scaleProperty,
                                                        filledProperty,
                                                        thickProperty
                                                    ).add()
                                                }
                                            } row 0 rowSpans 3 col 0
                                            circle(
                                                radius = 4.0,
                                                fill = COLOR_YELLOW
                                            ) row 0 col 1
                                            textFlow {
                                                "${mediaWidth}x$mediaHeight"()
                                            } row 0 col 2
                                            circle(radius = 4.0, fill = COLOR_RED) row 1 col 1
                                            textFlow {
                                                "${size.trimSizes.size}pcs " { styleClass += "bold" }
                                                "${trimWidth}x$trimHeight"()
                                            } row 1 col 2
                                            lateinit var moreButton: Button
                                            moreButton = MoreButton(this@PlanoApp) {
                                                getString(R.string.save)(
                                                    ktfx.layouts.imageView(R.image.menu_save) {
                                                        opacity = BUTTON_OPACITY
                                                    }
                                                ) {
                                                    onAction {
                                                        moreButton.isVisible = false
                                                        val file = ResultFile()
                                                        @Suppress("LABEL_NAME_CLASH")
                                                        this@gridPane.snapshot {
                                                            ImageIO.write(
                                                                it.image.toSwingImage(),
                                                                "png",
                                                                file
                                                            )
                                                        }
                                                        GlobalScope.launch(Dispatchers.JavaFx) {
                                                            delay(500)
                                                            moreButton.isVisible = true
                                                            rootPane.jfxSnackbar(
                                                                getString(R.string._save)
                                                                    .format(file.name),
                                                                DURATION_SHORT,
                                                                getString(R.string.btn_open)
                                                            ) {
                                                                Desktop.getDesktop().open(file)
                                                            }
                                                        }
                                                    }
                                                }
                                                getString(R.string.delete)(
                                                    ktfx.layouts.imageView(R.image.menu_delete) {
                                                        opacity = BUTTON_OPACITY
                                                    }
                                                ) {
                                                    onAction {
                                                        outputPane.children -= this@pane
                                                    }
                                                }
                                            }.add() row 2 col 1 colSpans 2
                                        }
                                    })
                                }
                            }.add() row row col 0 colSpans 6 halign HPos.RIGHT
                        }
                        anchorPane {
                            scrollPane {
                                isFitToWidth = true
                                hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
                                outputPane = flowPane {
                                    paddingAll = 10
                                    prefWidthProperty().bind(this@scrollPane.widthProperty() - 10)
                                }
                            } anchorAll 0
                            borderPane {
                                label(getString(R.string.no_content))
                                visibleProperty().bind(outputPane.children.isEmptyBinding)
                                managedProperty().bind(outputPane.children.isEmptyBinding)
                            } anchorAll 0
                        } hpriority Priority.ALWAYS
                    } vpriority Priority.ALWAYS
                }
            }
        }
        stage.show()

        if (isExpanded) toggleExpand()
        if (isFilled) toggleFill()
        mediaWidthField.requestFocus()
    }

    private fun clear() {
        val children = outputPane.children.toList()
        outputPane.children.clear()
        rootPane.jfxSnackbar(
            getString(R.string._boxes_cleared),
            DURATION_SHORT,
            getString(R.string.btn_undo)
        ) {
            outputPane.children += children
        }
        mediaWidthField.requestFocus()
    }

    private fun toggleExpand() {
        scaleProperty.value = when (scaleProperty.value) {
            SCALE_SMALL -> SCALE_BIG
            else -> SCALE_SMALL
        }
    }

    private fun toggleFill() {
        filledProperty.value = !filledProperty.value
    }

    private fun toggleThick() {
        thickProperty.value = !thickProperty.value
    }

    private suspend fun checkForUpdate() {
        val release = withContext(Dispatchers.IO) { GitHubApi.getLatestRelease() }
        when {
            release.isNewerThan(BuildConfig.VERSION) -> rootPane.jfxSnackbar(
                getString(R.string._update_available).format(BuildConfig.VERSION),
                DURATION_LONG,
                getString(R.string.btn_download)
            ) {
                Desktop.getDesktop()
                    .browse(URI(release.assets.first {
                        when {
                            SystemUtils.IS_OS_MAC -> it.name.endsWith("dmg")
                            SystemUtils.IS_OS_WINDOWS -> it.name.endsWith("exe")
                            else -> it.name.endsWith("jar")
                        }
                    }.downloadUrl))
            }
            else -> rootPane.jfxSnackbar(
                getString(R.string._update_unavailable),
                DURATION_LONG
            )
        }
    }

    private fun StackPane.showAbout() =
        AboutDialog(this@PlanoApp, this).show()
}