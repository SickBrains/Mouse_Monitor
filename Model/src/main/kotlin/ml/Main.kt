package ml

import java.io.File
import java.io.FileWriter
import ml.data.*
import ml.io.MouseEventLoader.parseBalabitCsv
import ml.logic.FeatureExtractor
import ml.logic.Segmenter

fun main() {
    val inputDir = File("src/main/resources/balabit/training_files/user7")
    val outputFile = File("src/main/resources/training/features_user7_train.csv")

    // Ensure output directory exists
    outputFile.parentFile.mkdirs()

    val writer = FileWriter(outputFile)

    val header = listOf(
        "vxMean", "vxStd", "vyMean", "vyStd", "vMean", "vStd", "aMean", "aStd",
        "jMean", "jStd", "omegaMean", "omegaStd", "curvatureMean", "curvatureStd",
        "elapsedTime", "trajectoryLength", "endToEndDist", "straightness",
        "numPoints", "sumOfAngles", "largestDeviation", "numSharpAngles",
        "accelStartTime", "actionType", "label"
    )
    writer.write(header.joinToString(",") + "\n")

    val files = inputDir.listFiles { f -> f.extension.equals("csv", ignoreCase = true) } ?: emptyArray()
    println("Found ${files.size} CSV files in ${inputDir.absolutePath}")

    for (file in files) {
        println("Processing file: ${file.name}")
        val events = parseBalabitCsv(file)
        println("  Loaded ${events.size} events")

        val actions = Segmenter.segment(events)
        println("  Extracted ${actions.size} actions")

        for (action in actions) {
            if (action.events.size < 4) {
                println("  Skipped action with ${action.events.size} events")
                continue
            }

            val fv = FeatureExtractor.extract(action)
            if (fv == null) {
                println("  Skipped: Feature extraction returned null")
                continue
            }

            val row = listOf(
                fv.vxMean, fv.vxStd, fv.vyMean, fv.vyStd, fv.vMean, fv.vStd,
                fv.aMean, fv.aStd, fv.jMean, fv.jStd, fv.omegaMean, fv.omegaStd,
                fv.curvatureMean, fv.curvatureStd, fv.elapsedTime, fv.trajectoryLength,
                fv.endToEndDist, fv.straightness, fv.numPoints.toDouble(),
                fv.sumOfAngles, fv.largestDeviation, fv.numSharpAngles.toDouble(),
                fv.accelStartTime, fv.actionType, "1"
            )
            writer.write(row.joinToString(",") + "\n")
        }
    }

    writer.close()
    println("Extraction complete: ${outputFile.absolutePath}")
}
