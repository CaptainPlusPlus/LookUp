package day.domain

interface CloudRepository {
    suspend fun getCloudTypes(lat: Double, lon: Double): Result<CloudResult>
}
