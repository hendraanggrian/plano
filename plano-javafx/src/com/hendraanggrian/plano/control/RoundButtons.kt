package com.hendraanggrian.plano.control

import com.hendraanggrian.plano.App.Companion.BUTTON_OPACITY
import com.hendraanggrian.plano.R
import com.hendraanggrian.plano.Resources
import com.hendraanggrian.plano.StandardSize
import com.jfoenix.controls.JFXButton
import javafx.beans.value.ObservableBooleanValue
import javafx.beans.value.ObservableValue
import javafx.geometry.Side
import javafx.scene.control.TextField
import javafx.scene.image.ImageView
import javafx.scene.layout.StackPane
import javafx.scene.shape.Circle
import ktfx.bindings.bindingOf
import ktfx.bindings.given
import ktfx.bindings.otherwise
import ktfx.bindings.then
import ktfx.finalStringProperty
import ktfx.layouts.KtfxContextMenu
import ktfx.layouts.MenuItemManager
import ktfx.layouts.contextMenu
import ktfx.layouts.menu
import ktfx.layouts.menuItem
import ktfx.layouts.separatorMenuItem
import ktfx.layouts.tooltip
import ktfx.listeners.onAction

@Suppress("LeakingThis")
sealed class AbstractRoundButton(
    radius: Number,
    textBinding: ObservableValue<String>
) : JFXButton() {
    init {
        val r = radius.toDouble()
        shape = Circle(r)
        setMinSize(2 * r, 2 * r)
        setMaxSize(2 * r, 2 * r)

        tooltip {
            textProperty().bind(textBinding)
        }
    }
}

open class SimpleRoundButton(
    radius: Number,
    text: String,
    graphicUrl: String
) : AbstractRoundButton(radius, finalStringProperty(text)) {
    init {
        graphic = ImageView(graphicUrl)
    }
}

open class RoundButton(
    radius: Number,
    textBinding: ObservableValue<String>,
    graphicUrl: String
) : AbstractRoundButton(radius, textBinding) {
    constructor(
        radius: Number,
        text: String,
        graphicUrl: String
    ) : this(radius, finalStringProperty(text), graphicUrl)

    init {
        graphicProperty().bind(bindingOf(hoverProperty()) {
            ImageView(graphicUrl).also { if (!isHover) it.opacity = BUTTON_OPACITY }
        })
    }
}

open class AdaptableRoundButton(
    radius: Number,
    dependency: ObservableBooleanValue,
    text1: String,
    text2: String,
    graphicUrl1: String,
    graphicUrl2: String
) : AbstractRoundButton(radius, given(dependency) then text1 otherwise text2) {
    init {
        graphicProperty().bind(bindingOf(dependency, hoverProperty()) {
            ImageView(
                when {
                    dependency.value -> graphicUrl1
                    else -> graphicUrl2
                }
            ).also { if (!isHover) it.opacity = BUTTON_OPACITY }
        })
    }
}

open class InfoButton(
    resources: Resources,
    container: StackPane,
    titleId: String,
    contentId: String
) : RoundButton(16, resources.getString(R.string.info), R.image.menu_info) {
    init {
        onAction {
            TextDialog(resources, container, titleId, contentId).show()
        }
    }
}

open class MoreButton(
    resources: Resources,
    init: KtfxContextMenu.() -> Unit
) : RoundButton(16, resources.getString(R.string.more), R.image.menu_more) {
    init {
        val contextMenu = contextMenu(init)
        onAction {
            if (!contextMenu.isShowing) {
                contextMenu.show(this@MoreButton, Side.RIGHT, 0.0, 0.0)
            }
        }
    }
}

open class MorePaperButton(
    resources: Resources,
    widthField: TextField,
    heightField: TextField
) : MoreButton(resources, {
    val append: MenuItemManager.(StandardSize) -> Unit = { standardSize ->
        menuItem(standardSize.title) {
            onAction {
                widthField.text = standardSize.width.toString()
                heightField.text = standardSize.height.toString()
            }
        }
    }
    menu(resources.getString(R.string.a_series)) { StandardSize.aSeries().forEach { append(it) } }
    menu(resources.getString(R.string.b_series)) { StandardSize.bSeries().forEach { append(it) } }
    separatorMenuItem()
    menu(resources.getString(R.string.others)) { StandardSize.others().forEach { append(it) } }
})
