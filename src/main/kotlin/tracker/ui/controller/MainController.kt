package tracker.ui.controller

import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.stage.Stage
import tracker.Util
import tracker.Util.flushTimer
import tracker.Util.sessionDir
import tracker.Util.showAlert
import tracker.Util.writer
import tracker.input.MousePoller
import tracker.logic.MainService
import tracker.ui.Tray
import java.io.File
import java.util.*
import kotlin.concurrent.thread

class MainController {

    @FXML lateinit var sessionFiles: ListView<String>

    @FXML
    private lateinit var versionLabel: Label


    @FXML
    fun initialize() {
        println(sessionDir.exists())
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }
        val files = sessionDir.listFiles { f ->
            f.isFile && f.canRead() && hasCsvHeader(f)
        }?.map { it.name } ?: emptyList()
        sessionFiles.items = FXCollections.observableArrayList(files)
        println("Found session files: $files")

        val localVersionFile = File(System.getenv("LOCALAPPDATA"), "MouseMonitor/version.txt")
        val version = if (localVersionFile.exists()) localVersionFile.readText().trim() else "Unknown"
        versionLabel.text = "Version $version"


        Platform.runLater {
            val stage = sessionFiles.scene?.window as? Stage
            stage?.setOnCloseRequest {
                println("[MainController] Main window closed before starting tracking")
                Platform.exit()
                kotlin.system.exitProcess(0)
            }
        }
    }

    @FXML
    private lateinit var updateStatus: Label



    @FXML
    fun checkForUpdatesClicked() {
        updateStatus.text = "Status: Checking..."
        Thread {
            val result = Updater.checkForUpdate { msg ->
                Platform.runLater { updateStatus.text = msg }
            }

            Platform.runLater {
                updateStatus.text = result.message
                versionLabel.text = "Version: ${result.version}"
            }
        }.start()
    }




    private fun hasCsvHeader(file: File): Boolean {
        return try {
            val firstLine = file.bufferedReader().readLine() ?: return false
            firstLine.contains("start") &&
                    firstLine.contains("end") &&
                    firstLine.contains("x") &&
                    firstLine.contains("y") &&
                    firstLine.contains("window")
        } catch (e: Exception) {
            false
        }
    }

    private val trackingService = MainService()

    @FXML
    fun startButtonClicked(event: ActionEvent) {
        val stage = sessionFiles.scene.window as Stage

        Thread {
            trackingService.startTracking {}
            Platform.runLater { stage.hide() }
        }.start()
    }



    @FXML
    fun onSessionFileClicked() {
        val selected = sessionFiles.selectionModel.selectedItem ?: return
        val file = File(sessionDir, selected)
        if (!file.exists()) {
            showAlert("Missing File", "Selected file does not exist.")
        }
    }

    @FXML
    fun loadProfile() {
        val selected = sessionFiles.selectionModel.selectedItem
        if (selected.isNullOrBlank()) {
            showAlert("No session selected", "Select a session file to load.")
            return
        }

        val file = File(sessionDir, selected)

        if (!file.exists()) {
            showAlert("File Not Found", "The selected file does not exist.")
            return
        }

        try {
            val loader = FXMLLoader(javaClass.getResource("/tracker/ui/fxml/DataCollectedView.fxml"))
            val root = loader.load<Parent>()

            val controller = loader.getController<DataCollectedController>()
            controller.setSessionFile(file)

            val stage = Stage()
            stage.title = "Data Collected"
            stage.scene = Scene(root)
            stage.show()

        } catch (e: Exception) {
            e.printStackTrace()
            showAlert("Load Failed", "Failed to load data view.")
        }
    }


    @FXML
    fun deleteProfile() {
        val selected = sessionFiles.selectionModel.selectedItem
        if (selected.isNullOrBlank()) {
            showAlert("No session selected", "Select a session file to delete.")
            return
        }

        val file = File(sessionDir, selected)

        if (!file.exists()) {
            showAlert("File Not Found", "The file does not exist.")
            return
        }

        if (file.delete()) {
            sessionFiles.items.remove(selected)
        } else {
            showAlert("Delete Failed", "Could not delete file: $selected")
        }
    }


}
