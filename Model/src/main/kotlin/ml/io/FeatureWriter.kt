package ml.io

import ml.data.FeatureVector
import java.io.File

object FeatureWriter {
    fun saveCsv(features: List<FeatureVector>, outputPath: String) {
        val header = FeatureVector::class.java.declaredFields.joinToString(",") { it.name }
        val lines = features.map { fv ->
            listOf(
                fv.vxMean, fv.vxStd, fv.vyMean, fv.vyStd, fv.vMean, fv.vStd,
                fv.aMean, fv.aStd, fv.jMean, fv.jStd,
                fv.omegaMean, fv.omegaStd, fv.curvatureMean, fv.curvatureStd,
                fv.elapsedTime, fv.trajectoryLength, fv.endToEndDist, fv.straightness,
                fv.numPoints, fv.sumOfAngles, fv.largestDeviation, fv.numSharpAngles,
                fv.accelStartTime, fv.actionType, fv.label
            ).joinToString(",")
        }

        File(outputPath).printWriter().use { out ->
            out.println(header)
            lines.forEach(out::println)
        }
    }
}
