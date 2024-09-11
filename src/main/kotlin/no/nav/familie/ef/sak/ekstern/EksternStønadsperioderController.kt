package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.ekstern.stønadsperiode.EksternStønadsperioderService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.kontrakter.felles.PersonIdent
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.EksternePerioderMedBeløpResponse
import no.nav.familie.kontrakter.felles.ef.EksternePerioderRequest
import no.nav.familie.kontrakter.felles.ef.EksternePerioderResponse
import no.nav.familie.kontrakter.felles.ef.OvergangsstønadOgSkolepengerResponse
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(
    path = ["/api/ekstern/perioder"],
    consumes = [APPLICATION_JSON_VALUE],
    produces = [APPLICATION_JSON_VALUE],
)
@Validated
@ProtectedWithClaims(issuer = "azuread")
class EksternStønadsperioderController(
    private val eksternStønadsperioderService: EksternStønadsperioderService,
    private val perioderForBarnetrygdService: PerioderForBarnetrygdService,
    private val tilgangService: TilgangService,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    /**
     * Brukes av Arena
     */
    @PostMapping("alle-stonader", "")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun hentPerioderForAlleStønader(
        @RequestBody request: EksternePerioderRequest,
    ): Ressurs<EksternePerioderResponse> =
        try {
            Ressurs.success(eksternStønadsperioderService.hentPerioderForAlleStønader(request))
        } catch (e: Exception) {
            secureLogger.error("Kunne ikke hente perioder for ${request.personIdent}", e)
            Ressurs.failure("Henting av perioder for overgangsstønad feilet", error = e)
        }

    /**
     * Brukes av Tiltakspenger-overgangsstønad
     */
    @PostMapping("overgangsstonad")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun hentPerioderForOvergangsstønad(
        @RequestBody request: EksternePerioderRequest,
    ): Ressurs<EksternePerioderResponse> =
        try {
            Ressurs.success(EksternePerioderResponse(perioder = eksternStønadsperioderService.hentPerioderForOvergangsstønad(request)))
        } catch (e: Exception) {
            secureLogger.error("Kunne ikke hente perioder for ${request.personIdent}", e)
            Ressurs.failure("Henting av perioder for overgangsstønad feilet", error = e)
        }

    /**
     * Brukes av Bidrag
     */
    @PostMapping("overgangsstonad/med-belop")
    @ProtectedWithClaims(issuer = "azuread", claimMap = ["roles=access_as_application"])
    fun hentPerioderForOvergangsstønadMedBeløp(
        @RequestBody request: EksternePerioderRequest,
    ): Ressurs<EksternePerioderMedBeløpResponse> =
        try {
            Ressurs.success(EksternePerioderMedBeløpResponse(perioder = eksternStønadsperioderService.hentPerioderForOvergangsstønadMedBeløp(request)))
        } catch (e: Exception) {
            secureLogger.error("Kunne ikke hente perioder for ${request.personIdent}", e)
            Ressurs.failure("Henting av perioder for overgangsstønad feilet", error = e)
        }

    /**
     * Brukes av Barnetrygd, for å vurdere utvidet barnetrygd, henter kun perioder med full overgangsstønad
     */
    @PostMapping("full-overgangsstonad")
    fun hentPerioderMedFullOvergangsstonad(
        @RequestBody request: PersonIdent,
    ): Ressurs<EksternePerioderResponse> {
        if (!SikkerhetContext.erMaskinTilMaskinToken()) {
            tilgangService.validerTilgangTilPerson(request.ident, AuditLoggerEvent.ACCESS)
        }
        return Ressurs.success(perioderForBarnetrygdService.hentPerioderMedFullOvergangsstønad(request))
    }

    /**
     * Brukes av tilleggstønader, for å vurdere barnetilsyn-ytelse. Henter kun perioder med OS og SP.
     */
    @PostMapping("overgangsstonad-og-skolepenger")
    fun hentPerioderMedOvergangsstonadOgSkolepenger(
        @RequestBody request: EksternePerioderRequest,
    ): Ressurs<OvergangsstønadOgSkolepengerResponse> =
        try {
            Ressurs.success(eksternStønadsperioderService.hentPerioderForOvergangsstønadOgSkolepenger(request))
        } catch (e: Exception) {
            secureLogger.error("Kunne ikke hente perioder for $request", e)
            Ressurs.failure("Henting av perioder for overgangsstønad feilet", error = e)
        }
}
