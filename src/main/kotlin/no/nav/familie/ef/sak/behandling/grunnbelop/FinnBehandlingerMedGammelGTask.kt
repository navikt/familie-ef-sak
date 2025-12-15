package no.nav.familie.ef.sak.behandling.grunnbelop

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.logg.Logg
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = FinnBehandlingerMedGammelGTask.TYPE,
    beskrivelse = "Månedlig jobb som finner behandlinger som ikke har blitt g-omregnet",
)
class FinnBehandlingerMedGammelGTask(
    val behandlingRepository: BehandlingRepository,
    val tilkjentYtelseService: TilkjentYtelseService,
) : AsyncTaskStep {
    private val logger = Logg.getLogger(this::class)

    private val uaktuelleBehandlinger = listOf("4d4de67b-f87b-4b6b-8524-aa92b3ebb870")

    override fun doTask(task: Task) {
        logger.info("Starter jobb som finner behandlinger som ikke har blitt g-omregnet")
        val gjeldendeGrunnbeløpFraOgMedDato = Grunnbeløpsperioder.nyesteGrunnbeløp.periode.fomDato
        val behandlingsIdMedUtdatertG =
            behandlingRepository.finnFerdigstilteBehandlingerMedUtdatertGBelopSomMåBehandlesManuelt(
                gjeldendeGrunnbeløpFraOgMedDato,
            )
        val behandlingerSomBurdeBlittGOmregnet =
            behandlingsIdMedUtdatertG.filter { behandlingId ->
                val erIkkeRelevantForOmregning = utledOmBehandlingIkkeErRelevantForOmregning(behandlingId, gjeldendeGrunnbeløpFraOgMedDato)
                when {
                    erIkkeRelevantForOmregning -> false
                    uaktuelleBehandlinger.contains(behandlingId.toString()) -> false
                    else -> true
                }
            }
        behandlingerSomBurdeBlittGOmregnet.forEach { behandlingId -> logger.info("Behandling med id $behandlingId har utdatert G") }
        feilHvis(behandlingerSomBurdeBlittGOmregnet.isNotEmpty()) { "Ferdigstilte behandlinger med utdatert G" }
    }

    private fun utledOmBehandlingIkkeErRelevantForOmregning(
        behandlingId: UUID,
        gjeldendeGrunnbeløpFraOgMedDato: LocalDate?,
    ): Boolean {
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandlingId)
        val harSamordningMenErIkkeRelevantForOmregning =
            tilkjentYtelse.andelerTilkjentYtelse
                .filter { it.stønadTom >= gjeldendeGrunnbeløpFraOgMedDato }
                .all { it.beløp == 0 && it.samordningsfradrag > 0 }
        if (harSamordningMenErIkkeRelevantForOmregning) {
            logger.info("Behandling $behandlingId har kun samordningsfradrag og er følgelig ikke g-omregnet manuelt av noen saksbehandlere")
        }
        return harSamordningMenErIkkeRelevantForOmregning
    }

    companion object {
        const val TYPE = "finnBehandlingerMedGammelGTask"
    }
}
