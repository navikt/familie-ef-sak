package no.nav.familie.ef.sak.iverksett

import no.nav.familie.ef.sak.api.beregning.BeregningService
import no.nav.familie.ef.sak.api.beregning.Innvilget
import no.nav.familie.ef.sak.api.beregning.VedtakDto
import no.nav.familie.ef.sak.api.beregning.VedtakService
import no.nav.familie.ef.sak.api.beregning.tilInntektsperioder
import no.nav.familie.ef.sak.api.beregning.tilPerioder
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
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class SimuleringService(private val iverksettClient: IverksettClient,
                        private val behandlingService: BehandlingService,
                        private val fagsakService: FagsakService,
                        private val vedtakService: VedtakService,
                        private val beregningService: BeregningService,
                        private val tilkjentYtelseService: TilkjentYtelseService) {


    fun simulerForBehandling(behandlingId: UUID): DetaljertSimuleringResultat {
        val behandling = behandlingService.hentBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)

        return when (behandling.type) {
            BehandlingType.BLANKETT -> simulerUtenTilkjentYtelse(behandling, fagsak)
            else -> simulerMedTilkjentYtelse(behandling, fagsak)
        }
    }

    private fun simulerMedTilkjentYtelse(behandling: Behandling, fagsak: Fagsak): DetaljertSimuleringResultat {
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandling.id)

        val tilkjentYtelseMedMedtadata =
                tilkjentYtelse.tilIverksettMedMetaData(saksbehandlerId = SikkerhetContext.hentSaksbehandler(),
                                                       eksternBehandlingId = behandling.eksternId.id,
                                                       stønadstype = fagsak.stønadstype,
                                                       eksternFagsakId = fagsak.eksternId.id

                )
        val forrigeBehandlingId = behandlingService.hentForrigeBehandlingId(behandling.fagsakId)

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
                                                     inntekt = it.beregningsgrunnlag?.inntekt?.toInt() ?:0,
                                                     samordningsfradrag = it.beregningsgrunnlag?.samordningsfradrag?.toInt() ?: 0,
                                                     personIdent = fagsak.hentAktivIdent())
                        }
            }
            else -> emptyList()
        }


        val tilkjentYtelseForBlankett = TilkjentYtelse(
                personident = fagsak.hentAktivIdent(),
                vedtaksdato = LocalDate.now(),
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
                                         vedtaksdato = this.vedtaksdato ?: LocalDate.now())
    }

    private fun TilkjentYtelse.tilIverksett(): TilkjentYtelseDto {
        return TilkjentYtelseDto(andelerTilkjentYtelse = this.andelerTilkjentYtelse.map { it.tilIverksettDto() })
    }
}