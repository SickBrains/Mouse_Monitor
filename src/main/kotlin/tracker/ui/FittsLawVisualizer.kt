package tracker.ui

import javafx.application.Application
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.scene.shape.Rectangle
import javafx.stage.Stage
import javafx.stage.StageStyle
import tracker.input.ui.UIAutomationHelper
import kotlin.concurrent.fixedRateTimer

class FittsLawVisualizer : Application() {

    private val rect = Rectangle().apply {
        fill = Color.TRANSPARENT
        stroke = Color.RED
        strokeWidth = 2.0
    }

    override fun start(stage: Stage) {
        val scene = Scene(javafx.scene.Group(rect), 1.0, 1.0, Color.TRANSPARENT)

        stage.initStyle(StageStyle.TRANSPARENT)
        stage.scene = scene
        stage.isAlwaysOnTop = true
        stage.show()

        fixedRateTimer("fitts-visual", daemon = true, initialDelay = 0, period = 100) {
            val element = UIAutomationHelper.getElementAtCursor()
            if (element != null) {
                val (_, bounds) = element
                Platform.runLater {
                    rect.x = bounds.left.toDouble()
                    rect.y = bounds.top.toDouble()
                    rect.width = (bounds.right - bounds.left).toDouble()
                    rect.height = (bounds.bottom - bounds.top).toDouble()
                    stage.setX(0.0)
                    stage.setY(0.0)
                    stage.setWidth(java.awt.Toolkit.getDefaultToolkit().screenSize.width.toDouble())
                    stage.setHeight(java.awt.Toolkit.getDefaultToolkit().screenSize.height.toDouble())
                }
            }
        }
    }
}
