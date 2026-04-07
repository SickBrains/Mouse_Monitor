package tracker.ui.controller

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleLongProperty
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.fxml.FXML
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.text.Text
import tracker.data.CsvRow
import java.io.File

class DataCollectedController {

    @FXML
    lateinit var tableView: TableView<CsvRow>

    @FXML
    lateinit var startCol: TableColumn<CsvRow, Long>

    @FXML
    lateinit var endCol: TableColumn<CsvRow, Long>

    @FXML
    lateinit var xCol: TableColumn<CsvRow, Int>

    @FXML
    lateinit var yCol: TableColumn<CsvRow, Int>

    @FXML
    lateinit var leftCol: TableColumn<CsvRow, Int>

    @FXML
    lateinit var rightCol: TableColumn<CsvRow, Int>

    @FXML
    lateinit var middleCol: TableColumn<CsvRow, Int>

    @FXML
    lateinit var windowCol: TableColumn<CsvRow, String>

    @FXML
    lateinit var repeatsCol: TableColumn<CsvRow, Int>

    private var sessionFile: File? = null

    fun setSessionFile(file: File) {
        sessionFile = file
        if (this::tableView.isInitialized) {
            loadSessionData()
        }
    }

    private fun loadSessionData() {
        val file = sessionFile ?: return
        if (!file.exists()) return

        val rows = file.readLines().drop(1).mapNotNull { line ->
            val parts = line.split(",")
            if (parts.size >= 9) {
                val repeats = parts.last().trim().toIntOrNull() ?: 0
                val window = parts.subList(7, parts.size - 1)
                    .joinToString(",")
                    .trim()
                    .removeSurrounding("\"")
                    .replace("\"\"", "\"")
                CsvRow(
                    start = parts[0].toLong(),
                    end = parts[1].toLong(),
                    x = parts[2].toInt(),
                    y = parts[3].toInt(),
                    left = parts[4].toInt(),
                    right = parts[5].toInt(),
                    middle = parts[6].toInt(),
                    window = window,
                    repeats = repeats
                )
            } else null
        }
        tableView.items = FXCollections.observableArrayList(rows)
        autoResizeColumns(tableView)
    }

    @FXML
    fun initialize() {
        startCol.setCellValueFactory { SimpleLongProperty(it.value.start).asObject() }
        endCol.setCellValueFactory { SimpleLongProperty(it.value.end).asObject() }
        xCol.setCellValueFactory { SimpleIntegerProperty(it.value.x).asObject() }
        yCol.setCellValueFactory { SimpleIntegerProperty(it.value.y).asObject() }
        leftCol.setCellValueFactory { SimpleIntegerProperty(it.value.left).asObject() }
        rightCol.setCellValueFactory { SimpleIntegerProperty(it.value.right).asObject() }
        middleCol.setCellValueFactory { SimpleIntegerProperty(it.value.middle).asObject() }
        windowCol.setCellValueFactory { SimpleStringProperty(it.value.window) }
        repeatsCol.setCellValueFactory { SimpleIntegerProperty(it.value.repeats).asObject() }

        sessionFile?.let { loadSessionData() }
    }

    fun <T> autoResizeColumns(tableView: TableView<T>) {
        for (column in tableView.columns) {
            var max = computeTextWidth(column.text)
            for (item in tableView.items) {
                val cellData = column.getCellData(item)
                if (cellData != null) {
                    val width = computeTextWidth(cellData.toString())
                    if (width > max) max = width
                }
            }
            column.prefWidth = max + 20
        }
    }

    private fun computeTextWidth(text: String): Double {
        val textNode = Text(text)
        return textNode.layoutBounds.width
    }
}
