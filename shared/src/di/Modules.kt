package di
import day.data.LocationRepositoryImpl
import day.data.SunRepositoryImpl
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

// expect val platformModule: Module

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
        singleOf(::LocationRepositoryImpl) bind LocationRepository::class
        singleOf(::SunRepositoryImpl) bind SunRepository::class
        singleOf(::GetSunAngle)
        viewModelOf(::DaySkyViewModel)
    }
