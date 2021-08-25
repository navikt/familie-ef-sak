package no.nav.familie.ef.sak.api.simulering

import no.nav.familie.ef.sak.api.beregning.BeregningService
import no.nav.familie.ef.sak.api.beregning.Innvilget
import no.nav.familie.ef.sak.api.beregning.VedtakDto
import no.nav.familie.ef.sak.api.beregning.VedtakService
import no.nav.familie.ef.sak.api.beregning.tilInntektsperioder
import no.nav.familie.ef.sak.api.beregning.tilPerioder
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.tilIverksettDto
import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseType
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.iverksett.SimuleringDto
import no.nav.familie.kontrakter.ef.iverksett.TilkjentYtelseDto
import no.nav.familie.kontrakter.ef.iverksett.TilkjentYtelseMedMetadata
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
                        private val beregningService: BeregningService,
                        private val tilkjentYtelseService: TilkjentYtelseService) {


    fun simulerForBehandling(behandlingId: UUID): SimuleringsresultatDto {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        val simuleringResultat = when (behandling.type) {
            BehandlingType.BLANKETT -> simulerUtenTilkjentYtelse(behandling, fagsak)
            else -> simulerMedTilkjentYtelse(behandling, fagsak)
        }

        return vedtakSimuleringMottakereTilDto(simuleringResultat)
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
        val tilkjentYtelseForBlankett = genererTilkjentYtelseForBlankett(vedtak, behandling, fagsak)
        val simuleringDto = SimuleringDto(
                nyTilkjentYtelseMedMetaData = tilkjentYtelseForBlankett,
                forrigeBehandlingId = null

        )
        return iverksettClient.simuler(simuleringDto)
    }

    private fun genererTilkjentYtelseForBlankett(vedtak: VedtakDto?,
                                                 behandling: Behandling,
                                                 fagsak: Fagsak): TilkjentYtelseMedMetadata {
        val andeler = when (vedtak) {
            is Innvilget -> {
                beregningService.beregnYtelse(vedtak.perioder.tilPerioder(),
                                              vedtak.inntekter.tilInntektsperioder())
                        .map {
                            AndelTilkjentYtelse(beløp = it.beløp.toInt(),
                                                stønadFom = it.periode.fradato,
                                                stønadTom = it.periode.tildato,
                                                kildeBehandlingId = behandling.id,
                                                inntektsreduksjon = it.beregningsgrunnlag?.avkortningPerMåned?.toInt() ?: 0,
                                                inntekt = it.beregningsgrunnlag?.inntekt?.toInt() ?: 0,
                                                samordningsfradrag = it.beregningsgrunnlag?.samordningsfradrag?.toInt() ?: 0,
                                                personIdent = fagsak.hentAktivIdent())
                        }
            }
            else -> emptyList()
        }


        val tilkjentYtelseForBlankett = TilkjentYtelse(
                personident = fagsak.hentAktivIdent(),
                behandlingId = behandling.id,
                andelerTilkjentYtelse = andeler,
                type = TilkjentYtelseType.FØRSTEGANGSBEHANDLING)

        return tilkjentYtelseForBlankett
                .tilIverksettMedMetaData(
                        saksbehandlerId = SikkerhetContext.hentSaksbehandler(),
                        stønadstype = fagsak.stønadstype,
                        eksternBehandlingId = behandling.eksternId.id,
                        eksternFagsakId = fagsak.eksternId.id
                )
    }

    private fun TilkjentYtelse.tilIverksettMedMetaData(saksbehandlerId: String,
                                                       eksternBehandlingId: Long,
                                                       stønadstype: Stønadstype,
                                                       eksternFagsakId: Long): TilkjentYtelseMedMetadata {
        return TilkjentYtelseMedMetadata(tilkjentYtelse = this.tilIverksett(),
                                         saksbehandlerId = saksbehandlerId,
                                         eksternBehandlingId = eksternBehandlingId,
                                         stønadstype = StønadType.valueOf(stønadstype.name),
                                         eksternFagsakId = eksternFagsakId,
                                         personIdent = this.personident,
                                         behandlingId = this.behandlingId,
                                         vedtaksdato = this.vedtakstidspunkt?.toLocalDate() ?: LocalDate.now())
    }

    private fun TilkjentYtelse.tilIverksett(): TilkjentYtelseDto {
        return TilkjentYtelseDto(andelerTilkjentYtelse = this.andelerTilkjentYtelse.map { it.tilIverksettDto() })
    }

    fun vedtakSimuleringMottakereTilDto(detaljertSimuleringResultat: DetaljertSimuleringResultat): SimuleringsresultatDto {
        val perioder = vedtakSimuleringMottakereTilSimuleringPerioder(detaljertSimuleringResultat.simuleringMottaker)

        val tidSimuleringHentet = LocalDate.now() // TODO: Hva burde denne være?

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

    fun vedtakSimuleringMottakereTilSimuleringPerioder(
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

    fun hentNyttBeløpIPeriode(periode: List<SimulertPostering>): BigDecimal {
        val sumPositiveYtelser = periode.filter { postering ->
            postering.posteringType == PosteringType.YTELSE && postering.beløp > BigDecimal.ZERO
        }.sumOf { it.beløp }
        val feilutbetaling = hentFeilbetalingIPeriode(periode)
        return if (feilutbetaling > BigDecimal.ZERO) sumPositiveYtelser - feilutbetaling else sumPositiveYtelser
    }

    fun hentFeilbetalingIPeriode(periode: List<SimulertPostering>) =
            periode.filter { postering ->
                postering.posteringType == PosteringType.FEILUTBETALING
            }.sumOf { it.beløp }

    fun hentTidligereUtbetaltIPeriode(periode: List<SimulertPostering>): BigDecimal {
        val sumNegativeYtelser = periode.filter { postering ->
            (postering.posteringType === PosteringType.YTELSE && postering.beløp < BigDecimal.ZERO)
        }.sumOf { -it.beløp }
        val feilutbetaling = hentFeilbetalingIPeriode(periode)
        return if (feilutbetaling < BigDecimal.ZERO) sumNegativeYtelser - feilutbetaling else sumNegativeYtelser
    }

    fun hentResultatIPeriode(periode: List<SimulertPostering>) =
            if (periode.map { it.posteringType }.contains(PosteringType.FEILUTBETALING)) {
                periode.filter {
                    it.posteringType == PosteringType.FEILUTBETALING
                }.sumOf { -it.beløp }
            } else
                periode.sumOf { it.beløp }

    fun hentTotalEtterbetaling(simuleringPerioder: List<SimuleringsPeriode>, fomDatoNestePeriode: LocalDate?) =
            simuleringPerioder.filter {
                it.resultat > BigDecimal.ZERO && (fomDatoNestePeriode == null || it.fom < fomDatoNestePeriode)
            }.sumOf { it.resultat }


    fun hentTotalFeilutbetaling(simuleringPerioder: List<SimuleringsPeriode>, fomDatoNestePeriode: LocalDate?) =
            simuleringPerioder.filter { fomDatoNestePeriode == null || it.fom < fomDatoNestePeriode }.sumOf { it.feilutbetaling }
}