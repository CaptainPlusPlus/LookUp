package di
import app.RootViewModel
import day.data.CitySearchRepositoryImpl
import day.data.DeviceLocationRepositoryImpl
import day.data.LocationRepositoryImpl
import day.data.SunRepositoryImpl
import day.data.storage.PreferencesStorage
import day.data.storage.PreferencesStorageImpl
import day.domain.CitySearchRepository
import day.domain.DeviceLocationRepository
import day.domain.GetSunAngle
import day.domain.LocationRepository
import day.domain.SunRepository
import day.presentation.DaySkyViewModel
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import welcome.presentation.WelcomeViewModel
import org.koin.core.module.Module

expect val platformModule: Module

val sharedModule =
    module {
        single {
            HttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                    })
                }
            }
        }

        // Storage (replace database/dao)
        singleOf(::PreferencesStorageImpl) bind PreferencesStorage::class

        // Repositories
        singleOf(::LocationRepositoryImpl) bind LocationRepository::class
        singleOf(::SunRepositoryImpl) bind SunRepository::class
        singleOf(::CitySearchRepositoryImpl) bind CitySearchRepository::class
        singleOf(::DeviceLocationRepositoryImpl) bind DeviceLocationRepository::class

        // Use Cases
        singleOf(::GetSunAngle)

        // ViewModels
        viewModelOf(::RootViewModel)
        viewModelOf(::DaySkyViewModel)
        viewModelOf(::WelcomeViewModel)
    }
