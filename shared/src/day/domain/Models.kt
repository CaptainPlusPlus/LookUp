package day.domain

data class GeoPoint(
    val latitudeDeg: Double,
    val longitudeDeg: Double,
)

data class SavedLocation(
    val id: String,
    val label: String,
    val point: GeoPoint,
)

sealed interface LocationChoice {
    data object Device : LocationChoice

    data class Saved(
        val locationId: String,
    ) : LocationChoice
}

enum class CloudType { CUMULUS, STRATUS, CIRRUS, NIMBUS }

data class InputsUsed(val cloudCover: Int, val weatherCode: Int?)

data class CloudResult(val types: List<CloudType>, val inputs: InputsUsed)
