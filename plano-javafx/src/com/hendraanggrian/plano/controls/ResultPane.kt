package com.hendraanggrian.plano.controls

import com.hendraanggrian.plano.MediaBox
import com.hendraanggrian.plano.PlanoApp
import com.hendraanggrian.plano.R
import com.hendraanggrian.plano.Resources
import com.hendraanggrian.plano.ResultFile
import com.hendraanggrian.plano.util.clean
import com.hendraanggrian.plano.util.getResource
import javafx.beans.property.BooleanProperty
import javafx.beans.property.DoubleProperty
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ContextMenu
import javafx.scene.control.Label
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.FlowPane
import javax.imageio.ImageIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import ktfx.controls.paddings
import ktfx.controls.toSwingImage
import ktfx.coroutines.onAction
import ktfx.jfoenix.controls.jfxSnackbar
import ktfx.layouts.KtfxGridPane
import ktfx.layouts.addChild
import ktfx.layouts.anchorPane
import ktfx.layouts.checkMenuItem
import ktfx.layouts.contextMenu
import ktfx.layouts.flowPane
import ktfx.layouts.hbox
import ktfx.layouts.menuItem
import ktfx.layouts.pane
import ktfx.layouts.separatorMenuItem
import ktfx.layouts.styledCircle
import ktfx.layouts.styledLabel
import ktfx.listeners.capture
import ktfx.listeners.onContextMenuRequested
import ktfx.minus
import ktfx.or
import ktfx.times

class ResultPane(
    private val app: PlanoApp,
    mediaWidth: Double,
    mediaHeight: Double,
    private val trimWidth: Double,
    private val trimHeight: Double,
    private val bleed: Double,
    allowFlip: Boolean,
    private val scaleProperty: DoubleProperty,
    private val fillProperty: BooleanProperty,
    private val thickProperty: BooleanProperty
) : KtfxGridPane(), Resources by app {

    private val mediaLabel: Label
    private val trimSizeLabel: Label
    private val trimLabel: Label
    private val infoFlowPane: FlowPane
    private val boxPaneContainer: AnchorPane
    private val closeButton: Button
    private val contextMenu: ContextMenu

    init {
        paddings = 10.0
        val mediaBox = MediaBox(mediaWidth, mediaHeight)
        mediaBox.populate(trimWidth, trimHeight, bleed, allowFlip)

        infoFlowPane = flowPane {
            hgap = 10.0
            hbox {
                alignment = Pos.CENTER_LEFT
                styledCircle(radius = 4.0, id = R.style.circle_amber)
                pane { minWidth = 5.0 }
                mediaLabel = styledLabel(id = R.style.label_info)
            }
            hbox {
                alignment = Pos.CENTER_LEFT
                styledCircle(radius = 4.0, id = R.style.circle_red)
                trimSizeLabel = styledLabel(id = R.style.label_red)
                pane { minWidth = 5.0 }
                trimLabel = styledLabel(id = R.style.label_info)
            }
        } row 0 col 0 fillHeight false
        closeButton = addChild(RoundButton(app, RoundButton.RADIUS_SMALL, R.string.close)) {
            id = R.style.menu_close
            onAction { close() }
        } row 0 col 1
        boxPaneContainer = anchorPane() row 1 col (0 to 2)

        contextMenu = contextMenu {
            menuItem(getString(R.string.rotate)) {
                onAction {
                    mediaBox.rotate()
                    populate(mediaBox)
                }
            }
            checkMenuItem(getString(R.string.allow_flip)) {
                isSelected = mediaBox.allowFlip
                onAction {
                    mediaBox.allowFlip = !mediaBox.allowFlip
                    populate(mediaBox)
                }
            }
            separatorMenuItem()
            menuItem(getString(R.string.close)) { onAction { close() } }
            menuItem(getString(R.string.close_all)) { onAction { app.closeAll() } }
            separatorMenuItem()
            menuItem(getString(R.string.save)) {
                onAction {
                    val isDarkTheme = getResource(R.style._plano_dark) in this@ResultPane.scene.stylesheets
                    if (isDarkTheme) {
                        mediaLabel.id = R.style.label_black
                        trimLabel.id = R.style.label_black
                    }
                    val file = ResultFile()
                    this@ResultPane.capture { ImageIO.write(it.image.toSwingImage(), "png", file) }
                    GlobalScope.launch(Dispatchers.JavaFx) {
                        delay(500)
                        if (isDarkTheme) {
                            mediaLabel.id = null
                            trimLabel.id = null
                        }
                        app.rootPane.jfxSnackbar(
                            getString(R.string._save).format(file.name),
                            PlanoApp.DURATION_SHORT,
                            getString(R.string.btn_show_directory)
                        ) {
                            app.hostServices.showDocument(file.parentFile.toURI().toString())
                        }
                    }
                }
            }
        }
        onContextMenuRequested {
            contextMenu.x = it.screenX
            contextMenu.y = it.screenY
            contextMenu.show(scene.window)
        }

        closeButton.visibleProperty().bind(this@ResultPane.hoverProperty() or contextMenu.showingProperty())
        populate(mediaBox)
    }

    private fun populate(mediaBox: MediaBox) {
        mediaLabel.text = "${mediaBox.width.clean()} x ${mediaBox.height.clean()}"
        trimSizeLabel.text = " ${mediaBox.size}"
        trimLabel.text = "${(trimWidth + bleed * 2).clean()} x ${(trimHeight + bleed * 2).clean()}"
        infoFlowPane.prefWrapLengthProperty()
            .bind(scaleProperty * mediaBox.width - 12.0 * 2) // minus close button
        boxPaneContainer.children.clear()
        boxPaneContainer.children += MediaBoxPane(mediaBox, scaleProperty, fillProperty, thickProperty)
        mediaBox.forEach {
            boxPaneContainer.children += TrimBoxPane(it, scaleProperty, fillProperty, thickProperty)
        }
    }

    private fun close() {
        app.outputPane.children -= this@ResultPane.parent
        app.rootPane.requestFocus()
    }
}
