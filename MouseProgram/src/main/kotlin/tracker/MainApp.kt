package tracker

import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.stage.Stage


class MainApp : Application() {
    override fun start(primaryStage: Stage) {

        Platform.setImplicitExit(false)

        val fxml = javaClass.getResource("/tracker/ui/fxml/GUI.fxml")
            ?: throw IllegalStateException("GUI.fxml not found")

        val root = FXMLLoader.load<javafx.scene.Parent>(fxml)

        primaryStage.title = "Mouse Tracker GUI"
        primaryStage.scene = Scene(root)
        primaryStage.show()
    }
}

