package ua.polodarb.gmsflags.data.remote.flags

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.url
import kotlinx.serialization.json.Json
import ua.polodarb.gmsflags.BuildConfig
import ua.polodarb.gmsflags.data.remote.Resource
import ua.polodarb.gmsflags.data.remote.flags.dto.SuggestedFlagTypes
import ua.polodarb.gmsflags.data.remote.setConfig

private const val BASE_URL = "https://raw.githubusercontent.com/polodarb/GMS-Flags/"
private const val ASSETS_PATH = "/app/src/main/assets/"
private const val LOG_TAG = "FlagsApiService"

class FlagsApiServiceImpl(
    engine: HttpClientEngine
): FlagsApiService {
    private val client = HttpClient(engine) {
        this.setConfig(LOG_TAG)
        defaultRequest {
            url(BASE_URL + (if (BuildConfig.DEBUG) "develop" else "master") + ASSETS_PATH)
        }
    }

    override suspend fun getSuggestedFlags(): Resource<SuggestedFlagTypes> {
        return try {
            val response: String = client.get { url("suggestedFlags_2.0.json") }.body()
            Resource.Success(Json.decodeFromString(response))
        } catch (e: Exception) {
            Resource.Error(e)
        }
    }
}