package no.nav.familie.ef.sak.forvaltning

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.grunnbelop.GOmregningTask
import no.nav.familie.ef.sak.behandling.grunnbelop.GOmregningTaskService
import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/gomregning")
@ProtectedWithClaims(issuer = "azuread")
class ManuellGOmregningController(
    private val gOmregningTaskService: GOmregningTaskService,
    private val gOmregningTask: GOmregningTask,
    private val featureToggleService: FeatureToggleService,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val tilgangService: TilgangService,
) {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    @PostMapping(path = ["startjobb"])
    fun opprettGOmregningTasksForBehandlingerMedGammeltGBelop(): Ressurs<Int> {
        tilgangService.validerHarForvalterrolle()
        val antallTaskerOpprettet = gOmregningTaskService.opprettGOmregningTaskForBehandlingerMedUtdatertG()
        return Ressurs.success(antallTaskerOpprettet)
    }

    @PostMapping(path = ["{fagsakId}"])
    fun opprettGOmregningTaskForFagsak(
        @PathVariable fagsakId: UUID,
    ) {
        tilgangService.validerHarForvalterrolle()
        validerHarLøpendeStønadEtterSisteGrunnbeløpdato(fagsakId)

        val opprettTask = gOmregningTask.opprettTask(fagsakId)
        logger.info("Opprettet G-omregning task for $fagsakId - resultat: $opprettTask")
    }

    private fun validerHarLøpendeStønadEtterSisteGrunnbeløpdato(fagsakId: UUID) {
        val sisteBehandling = behandlingService.finnSisteIverksatteBehandling(fagsakId)
        feilHvis(sisteBehandling == null) { "Må ha en iverksatt behandling" }
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(sisteBehandling.id)
        val nyesteGrunnbeløpsFomDato = Grunnbeløpsperioder.nyesteGrunnbeløp.periode.fomDato

        feilHvisIkke(
            tilkjentYtelse.andelerTilkjentYtelse.any {
                it.stønadTom >= nyesteGrunnbeløpsFomDato
            },
        ) {
            "Unødvendig å omregne fagsak uten løpende stønad i ny grunnbeløpsperiode"
        }
    }
}
