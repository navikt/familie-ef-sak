package no.nav.familie.ef.sak.texas

import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.logger
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI

class Texas {
    private val tokenEndpoint = System.getenv("NAIS_TOKEN_ENDPOINT")

    fun genererToken(): String {
        val uri = URI(tokenEndpoint)

        logger.info("Genererer token mot uri $uri")

        val obj =
            mapOf(
                "identity_provider" to "azuread",
                "target" to "api://dev-gcp.teamfamilie.familie-ef-sak/.default",
            )

        val body = objectMapper.writeValueAsString(obj)

        val res =
            sendPostRequest(
                urlStr = uri.toString(),
                jsonBody = body,
            )

        logger.info("--- response $res")

        return res
    }

    private fun sendPostRequest(
        urlStr: String,
        jsonBody: String,
    ): String {
        val url = URI(urlStr).toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }
        }

        return connection.inputStream.bufferedReader().use { it.readText() }
    }
}
