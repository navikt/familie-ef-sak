package no.nav.familie.ef.sak.kontantstøtte

import no.nav.familie.restklient.client.AbstractRestClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate
import java.time.YearMonth

@Component
class KontantstøtteClient(
    @Value("\${FAMILIE_KS_SAK_URL}") private val kontantstøtteUrl: URI,
    @Qualifier("azure") restOperations: RestOperations,
) : AbstractRestClient(restOperations, "kontantstøtte") {
    private fun lagHentUtbetalingsinfoUri() =
        UriComponentsBuilder
            .fromUri(kontantstøtteUrl)
            .pathSegment("api/bisys/hent-utbetalingsinfo")
            .build()
            .toUri()

    fun hentUtbetalingsinfo(forelderIdenter: List<String>): HentUtbetalingsinfoKontantstøtte =
        postForEntity(
            lagHentUtbetalingsinfoUri(),
            HentUtbetalingsinfoKontantstøtteRequest(LocalDate.MIN.toString(), forelderIdenter),
        )
}

data class HentUtbetalingsinfoKontantstøtteRequest(
    val fom: String,
    val identer: List<String>,
)

data class HentUtbetalingsinfoKontantstøtte(
    val infotrygdPerioder: List<KontantstøtteInfotrygdPeriode>,
    val ksSakPerioder: List<KsSakPeriode>,
)

data class KontantstøtteInfotrygdPeriode(
    val fomMåned: YearMonth,
    val tomMåned: YearMonth?,
    val beløp: Int,
    val barna: List<String>,
)

data class KsSakPeriode(
    val fomMåned: YearMonth,
    val tomMåned: YearMonth?,
    val barn: KsBarn,
)

data class KsBarn(
    val beløp: Int,
    val ident: String,
)
