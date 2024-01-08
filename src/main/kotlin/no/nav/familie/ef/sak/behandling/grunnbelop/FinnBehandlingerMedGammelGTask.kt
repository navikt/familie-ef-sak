package no.nav.familie.ef.sak.behandling.grunnbelop

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = FinnBehandlingerMedGammelGTask.TYPE,
    beskrivelse = "Månedlig jobb som finner behandlinger som ikke har blitt g-omregnet",
)
class FinnBehandlingerMedGammelGTask(
    val behandlingRepository: BehandlingRepository,
    val tilkjentYtelseService: TilkjentYtelseService,
) : AsyncTaskStep {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun doTask(task: Task) {
        logger.info("Starter jobb som finner behandlinger som ikke har blitt g-omregnet")
        val gjeldendeGrunnbeløpFraOgMedDato = Grunnbeløpsperioder.nyesteGrunnbeløp.periode.fomDato
        val behandlingsIdMedUtdatertG = behandlingRepository.finnFerdigstilteBehandlingerMedUtdatertGBelopSomMåBehandlesManuelt(
            gjeldendeGrunnbeløpFraOgMedDato,
        )
        val behandlingerSomBurdeBlittGOmregnet = behandlingsIdMedUtdatertG.filter { behandlingId ->
            val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandlingId)
            val harSamordningMenIkkeOmregnet = tilkjentYtelse.andelerTilkjentYtelse
                .filter { it.stønadTom >= gjeldendeGrunnbeløpFraOgMedDato }
                .all { it.beløp == 0 && it.samordningsfradrag > 0 && it.inntektsreduksjon == 0 }
            if (harSamordningMenIkkeOmregnet) {
                logger.info("Behandling $behandlingId har kun samordningsfradrag og er følgelig ikke g-omregnet manuelt av noen saksbehandlere")
            }
            !harSamordningMenIkkeOmregnet
        }
        behandlingerSomBurdeBlittGOmregnet.forEach { behandlingId -> logger.info("Behandling med id $behandlingId har utdatert G") }
        feilHvis(behandlingerSomBurdeBlittGOmregnet.isNotEmpty()) { "Ferdigstilte behandlinger med utdatert G" }
    }

    companion object {
        const val TYPE = "finnBehandlingerMedGammelGTask"
    }
}
