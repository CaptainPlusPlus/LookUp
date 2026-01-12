package day.data.storage

import kotlinx.serialization.Serializable

@Serializable
data class LocationEntity(
    val id: String,
    val label: String,
    val latitudeDeg: Double,
    val longitudeDeg: Double,
    val isCurrent: Boolean = false
)
