package no.nav.familie.ef.sak.tilbakekreving

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekreving
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekrevingsvalg
import no.nav.familie.ef.sak.tilbakekreving.dto.TilbakekrevingDto
import no.nav.familie.ef.sak.tilbakekreving.dto.tilDomene
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.FeilutbetaltePerioderDto
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Periode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandling as TilbakekrevingBehandling

@Service
class TilbakekrevingService(private val tilbakekrevingRepository: TilbakekrevingRepository,
                            private val behandlingService: BehandlingService,
                            private val fagsakService: FagsakService,
                            private val tilbakekrevingClient: TilbakekrevingClient,
                            private val simuleringService: SimuleringService) {


    fun lagreTilbakekreving(tilbakekrevingDto: TilbakekrevingDto, behandlingId: UUID) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        validerTilbakekreving(behandling, tilbakekrevingDto)
        tilbakekrevingRepository.deleteById(behandlingId)
        tilbakekrevingRepository.insert(tilbakekrevingDto.tilDomene(behandlingId))
    }

    fun hentTilbakekreving(behandlingId: UUID): Tilbakekreving? {
        return tilbakekrevingRepository.findByIdOrNull(behandlingId)
    }

    fun slettTilbakekreving(behandlingId: UUID) {
        tilbakekrevingRepository.deleteById(behandlingId)
    }

    private fun validerTilbakekreving(behandling: Behandling, tilbakekrevingDto: TilbakekrevingDto) {
        feilHvis(tilbakekrevingDto.valg == Tilbakekrevingsvalg.OPPRETT_MED_VARSEL
                 && tilbakekrevingDto.varseltekst.isNullOrBlank()) {
            "Må fylle ut varseltekst for å lage tilbakekreving med varsel"
        }
        feilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Behandlingen er låst for redigering"
        }
    }

    fun finnesÅpenTilbakekrevingsBehandling(behandlingId: UUID): Boolean {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        return tilbakekrevingClient.finnesÅpenBehandling(fagsakEksternId = fagsak.eksternId.id)
    }

    fun hentTilbakekrevingBehandlinger(fagsakId: UUID): List<TilbakekrevingBehandling> {
        val eksternFagsakId = fagsakService.hentEksternId(fagsakId)

        return tilbakekrevingClient.finnBehandlinger(eksternFagsakId)
    }

    fun harSaksbehandlerTattStillingTilTilbakekreving(behandlingsId: UUID): Boolean {
        return tilbakekrevingRepository.existsById(behandlingsId)
    }

    fun hentBrev(behandlingId: UUID): ByteArray {
        val simulering = simuleringService.simuler(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        simulering.perioder.map { Periode(fom = it.fom, tom = it.tom) }
        val perioder: List<Periode> = listOf()
        val feilutbetaltePerioderDto =
                FeilutbetaltePerioderDto(sumFeilutbetaling = simulering.feilutbetaling.toLong(), perioder = perioder)
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandler(strict = true)
        val fagsak = fagsakService.hentFagsak(behandling.fagsakId)
        val request = ForhåndsvisVarselbrevRequest(varseltekst = null,
                                                   ytelsestype = Ytelsestype.OVERGANGSSTØNAD,
                                                   behandlendeEnhetId = "4489", // TODO
                                                   behandlendeEnhetsNavn = "?", // TODO
                                                   saksbehandlerIdent = saksbehandlerIdent,
                                                   språkkode = Språkkode.NB,
                                                   vedtaksdato = LocalDate.now(),
                                                   feilutbetaltePerioderDto = feilutbetaltePerioderDto,
                                                   fagsystem = Fagsystem.EF,
                                                   eksternFagsakId = fagsak.eksternId.toString(),
                                                   ident = fagsak.hentAktivIdent(),
                                                   verge = null)
        return tilbakekrevingClient.hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest = request)
    }

}