package no.nav.familie.ef.sak.api.simulering

import no.nav.familie.ef.sak.api.beregning.VedtakService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.ef.iverksett.SimuleringDto
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import no.nav.familie.kontrakter.felles.simulering.PosteringType
import no.nav.familie.kontrakter.felles.simulering.SimuleringMottaker
import no.nav.familie.kontrakter.felles.simulering.SimulertPostering
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
class SimuleringService(private val iverksettClient: IverksettClient,
                        private val behandlingService: BehandlingService,
                        private val fagsakService: FagsakService,
                        private val vedtakService: VedtakService,
                        private val blankettSimuleringsService: BlankettSimuleringsService,
                        private val tilkjentYtelseService: TilkjentYtelseService) {


    fun simulerForBehandling(behandlingId: UUID): SimuleringsresultatDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        val simuleringResultat = when (behandling.type) {
            BehandlingType.BLANKETT -> simulerUtenTilkjentYtelse(behandling, fagsak)
            else -> simulerMedTilkjentYtelse(behandling, fagsak)
        }

        return tilSimuleringsresultatDto(simuleringResultat)
    }

    private fun simulerMedTilkjentYtelse(behandling: Behandling, fagsak: Fagsak): DetaljertSimuleringResultat {
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandling.id)

        val tilkjentYtelseMedMedtadata =
                tilkjentYtelse.tilIverksettMedMetaData(saksbehandlerId = SikkerhetContext.hentSaksbehandler(),
                                                       eksternBehandlingId = behandling.eksternId.id,
                                                       stønadstype = fagsak.stønadstype,
                                                       eksternFagsakId = fagsak.eksternId.id

                )
        val forrigeBehandlingId = behandlingService.finnSisteIverksatteBehandling(behandling.fagsakId)

        return iverksettClient.simuler(SimuleringDto(
                nyTilkjentYtelseMedMetaData = tilkjentYtelseMedMedtadata,
                forrigeBehandlingId = forrigeBehandlingId
        ))
    }

    private fun simulerUtenTilkjentYtelse(behandling: Behandling, fagsak: Fagsak): DetaljertSimuleringResultat {
        val vedtak = vedtakService.hentVedtakHvisEksisterer(behandling.id)
        val tilkjentYtelseForBlankett = blankettSimuleringsService.genererTilkjentYtelseForBlankett(vedtak, behandling, fagsak)
        val simuleringDto = SimuleringDto(
                nyTilkjentYtelseMedMetaData = tilkjentYtelseForBlankett,
                forrigeBehandlingId = null

        )
        return iverksettClient.simuler(simuleringDto)
    }


    private fun tilSimuleringsresultatDto(detaljertSimuleringResultat: DetaljertSimuleringResultat): SimuleringsresultatDto {
        val perioder = grupperPosteringerEtterDato(detaljertSimuleringResultat.simuleringMottaker)

        val tidSimuleringHentet = LocalDate.now() // TODO: Tidspunkt vi lagrer i databasen

        val framtidigePerioder =
                perioder.filter {
                    it.fom > tidSimuleringHentet ||
                    (it.tom > tidSimuleringHentet && it.forfallsdato > tidSimuleringHentet)
                }

        val nestePeriode = framtidigePerioder.filter { it.feilutbetaling == BigDecimal.ZERO }.minByOrNull { it.fom }
        val tomSisteUtbetaling = perioder.filter { nestePeriode == null || it.fom < nestePeriode.fom }.maxOfOrNull { it.tom }

        return SimuleringsresultatDto(
                perioder = perioder,
                fomDatoNestePeriode = nestePeriode?.fom,
                etterbetaling = hentTotalEtterbetaling(perioder, nestePeriode?.fom),
                feilutbetaling = hentTotalFeilutbetaling(perioder, nestePeriode?.fom),
                fom = perioder.minOfOrNull { it.fom },
                tomDatoNestePeriode = nestePeriode?.tom,
                forfallsdatoNestePeriode = nestePeriode?.forfallsdato,
                tidSimuleringHentet = tidSimuleringHentet,
                tomSisteUtbetaling = tomSisteUtbetaling,
        )
    }

    private fun grupperPosteringerEtterDato(
            mottakere: List<SimuleringMottaker>
    ): List<SimuleringsPeriode> {
        val simuleringPerioder = mutableMapOf<LocalDate, MutableList<SimulertPostering>>()


        mottakere.forEach {
            it.simulertPostering.filter { it.posteringType == PosteringType.YTELSE || it.posteringType == PosteringType.FEILUTBETALING }
                    .forEach { postering ->
                        if (simuleringPerioder.containsKey(postering.fom))
                            simuleringPerioder[postering.fom]?.add(postering)
                        else simuleringPerioder[postering.fom] = mutableListOf(postering)
                    }
        }

        return simuleringPerioder.map { (fom, posteringListe) ->
            SimuleringsPeriode(
                    fom,
                    posteringListe[0].tom,
                    posteringListe[0].forfallsdato,
                    nyttBeløp = hentNyttBeløpIPeriode(posteringListe),
                    tidligereUtbetalt = hentTidligereUtbetaltIPeriode(posteringListe),
                    resultat = hentResultatIPeriode(posteringListe),
                    feilutbetaling = hentFeilbetalingIPeriode(posteringListe),
            )
        }
    }


}