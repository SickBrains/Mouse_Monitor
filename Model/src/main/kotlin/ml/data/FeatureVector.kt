package ml.data

data class FeatureVector(
    val vxMean: Double,
    val vxStd: Double,
    val vyMean: Double,
    val vyStd: Double,
    val vMean: Double,
    val vStd: Double,
    val aMean: Double,
    val aStd: Double,
    val jMean: Double,
    val jStd: Double,
    val omegaMean: Double,
    val omegaStd: Double,
    val curvatureMean: Double,
    val curvatureStd: Double,
    val elapsedTime: Double,
    val trajectoryLength: Double,
    val endToEndDist: Double,
    val straightness: Double,
    val numPoints: Int,
    val sumOfAngles: Double,
    val largestDeviation: Double,
    val numSharpAngles: Int,
    val accelStartTime: Double,
    val actionType: String,
    val label: Int = 1
)
