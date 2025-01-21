package no.nav.familie.ef.sak.simulering

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/simulering"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class SimuleringController(
    private val tilgangService: TilgangService,
    private val behandlingService: BehandlingService,
    private val simuleringService: SimuleringService,
) {
    @GetMapping("/{behandlingId}")
    fun simulerForBehandling(
        @PathVariable behandlingId: UUID,
    ): Ressurs<SimuleringsoppsummeringDto> {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        tilgangService.validerTilgangTilBehandling(saksbehandling, AuditLoggerEvent.UPDATE)
        val simuleringsoppsummering = simuleringService.simuler(saksbehandling)
        return Ressurs.success(simuleringsoppsummering.tilSimuleringsoppsummeringDto())
    }

    @GetMapping("/simuleringsresultat-er-endret/{behandlingId}")
    fun erSimuleringsoppsummeringEndret(
        @PathVariable behandlingId: UUID,
    ): Ressurs<Boolean> {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        return Ressurs.success(simuleringService.erSimuleringsoppsummeringEndret(saksbehandling))
    }
}

data class SimuleringsoppsummeringDto(
    val perioder: List<SimuleringsperiodeDto>,
    val fomDatoNestePeriode: LocalDate?,
    val etterbetaling: BigDecimal,
    val feilutbetaling: BigDecimal,
    val feilutbetalingsår: Int?,
    val fireRettsgebyr: Int?,
    val visUnder4rettsgebyr: Boolean,
    val fom: LocalDate?,
    val tomDatoNestePeriode: LocalDate?,
    val forfallsdatoNestePeriode: LocalDate?,
    val tidSimuleringHentet: LocalDate?,
    val tomSisteUtbetaling: LocalDate?,
    val sumManuellePosteringer: BigDecimal?,
    val sumKreditorPosteringer: BigDecimal?,
)

data class SimuleringsperiodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val forfallsdato: LocalDate,
    val nyttBeløp: BigDecimal,
    val tidligereUtbetalt: BigDecimal,
    val resultat: BigDecimal,
    val feilutbetaling: BigDecimal,
)

fun Simuleringsoppsummering.tilSimuleringsoppsummeringDto(): SimuleringsoppsummeringDto {
    val sistefeilutbetalingsperiode = this.perioder.sortedBy { it.tom }.lastOrNull { it.feilutbetaling > BigDecimal.ZERO }
    val år = sistefeilutbetalingsperiode?.tom?.year
    val rettsgebyr = rettsgebyrForÅr[år]
    val erUnder4rettsgebyr = rettsgebyr != null && feilutbetaling < BigDecimal(rettsgebyr * 4) && etterbetaling == BigDecimal.ZERO

    return SimuleringsoppsummeringDto(
        perioder = this.perioder.map {
            SimuleringsperiodeDto(
                fom = it.fom,
                tom = it.tom,
                forfallsdato = it.forfallsdato,
                nyttBeløp = it.nyttBeløp,
                tidligereUtbetalt = it.tidligereUtbetalt,
                resultat = it.resultat,
                feilutbetaling = it.feilutbetaling,
            )
        },
        fomDatoNestePeriode = this.fomDatoNestePeriode,
        etterbetaling = this.etterbetaling,
        feilutbetaling = this.feilutbetaling,
        feilutbetalingsår = år,
        fireRettsgebyr = rettsgebyr?.let { it * 4 },
        visUnder4rettsgebyr = erUnder4rettsgebyr,
        fom = this.fom,
        tomDatoNestePeriode = this.tomDatoNestePeriode,
        forfallsdatoNestePeriode = this.forfallsdatoNestePeriode,
        tidSimuleringHentet = this.tidSimuleringHentet,
        tomSisteUtbetaling = this.tomSisteUtbetaling,
        sumManuellePosteringer = this.sumManuellePosteringer,
        sumKreditorPosteringer = this.sumKreditorPosteringer,
    )
}
