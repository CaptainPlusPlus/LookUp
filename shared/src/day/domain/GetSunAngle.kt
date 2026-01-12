package day.domain

class GetSunAngle(
    private val locationRepo: LocationRepository,
    private val sunRepo: SunRepository,
) {
    suspend operator fun invoke(): Float {
        val point = locationRepo.resolveActivePoint()
        val angle = sunRepo.getSunAngleNowDeg(point)
        return angle.coerceIn(0f, 180f)
    }
}