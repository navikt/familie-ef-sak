package no.nav.familie.ef.sak.tilbakekreving

import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.simulering.SimuleringsresultatRepository
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TilbakekrevingOppryddingService(
    private val tilbakekrevingRepository: TilbakekrevingRepository,
    private val simuleringsresultatRepository: SimuleringsresultatRepository,
    private val behandlingRepository: BehandlingRepository,
) {
    fun slettTilbakekrevingsvalgHvisIngenFeilutbetalingEllerForskjelligBeløp(
        behandlingId: UUID,
        nySimuleringsoppsummering: Simuleringsoppsummering,
    ) {
        if (nySimuleringsoppsummering.harIngenFeilutbetaling()) {
            slettTilbakekrevingsvalg(behandlingId)
        } else {
            val oppsummering = hentLagretSimmuleringsresultat(behandlingId)
            if (oppsummering != null && nySimuleringsoppsummering.harUlikeBeløp(oppsummering)) {
                slettTilbakekrevingsvalgUnder4rettsgebyr(behandlingId)
            }
        }
    }

    private fun slettTilbakekrevingsvalgUnder4rettsgebyr(behandlingId: UUID) {
        val tilbakekreving = tilbakekrevingRepository.findByIdOrNull(behandlingId)
        if (tilbakekreving != null && tilbakekreving.valg == Tilbakekrevingsvalg.OPPRETT_AUTOMATISK) {
            slettTilbakekrevingsvalg(behandlingId)
        }
    }

    private fun hentLagretSimmuleringsresultat(behandlingId: UUID): Simuleringsoppsummering? {
        val simuleringsresultat = simuleringsresultatRepository.findByIdOrNull(behandlingId)
        return simuleringsresultat?.beriketData?.oppsummering
    }

    private fun Simuleringsoppsummering.harIngenFeilutbetaling() = this.feilutbetaling.toLong() == 0L

    private fun Simuleringsoppsummering.harUlikeBeløp(oppsummering: Simuleringsoppsummering): Boolean = this.feilutbetaling != oppsummering.feilutbetaling || this.etterbetaling != oppsummering.etterbetaling

    fun slettTilbakekrevingsvalg(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        feilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke slette tilbakekrevingsvalg for behandling=$behandlingId da den er låst"
        }
        tilbakekrevingRepository.deleteById(behandlingId)
    }
}
