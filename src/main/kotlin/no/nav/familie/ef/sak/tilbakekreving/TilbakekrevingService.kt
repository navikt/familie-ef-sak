package no.nav.familie.ef.sak.tilbakekreving

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService.Companion.MASKINELL_JOURNALFOERENDE_ENHET
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.simulering.hentSammenhengendePerioderMedFeilutbetaling
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
                            private val simuleringService: SimuleringService,
                            private val arbeidsfordelingService: ArbeidsfordelingService) {


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

    fun hentBrev(behandlingId: UUID, varseltekst: String): ByteArray {
        validerIkkeFerdigstiltBehandling(behandlingId)
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val personIdent = fagsak.hentAktivIdent()
        val feilutbetaltePerioderDto = lagFeilutbetaltePerioderDto(behandlingId)
        val navEnhet = arbeidsfordelingService.hentNavEnhet(personIdent)
        val request = ForhåndsvisVarselbrevRequest(varseltekst = varseltekst,
                                                   ytelsestype = Ytelsestype.OVERGANGSSTØNAD,
                                                   behandlendeEnhetId = navEnhet?.enhetId ?: MASKINELL_JOURNALFOERENDE_ENHET,
                                                   behandlendeEnhetsNavn = navEnhet?.enhetNavn ?: "Ukjent",
                                                   saksbehandlerIdent = SikkerhetContext.hentSaksbehandler(strict = true),
                                                   språkkode = Språkkode.NB,
                                                   vedtaksdato = LocalDate.now(),
                                                   feilutbetaltePerioderDto = feilutbetaltePerioderDto,
                                                   fagsystem = Fagsystem.EF,
                                                   eksternFagsakId = fagsak.eksternId.toString(),
                                                   ident = personIdent,
                                                   verge = null)
        return tilbakekrevingClient.hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest = request)

    }

    private fun validerIkkeFerdigstiltBehandling(behandlingId: UUID) {
        feilHvis(behandlingService.hentBehandling(behandlingId).status == BehandlingStatus.FERDIGSTILT)
        { "Kan ikke generere forhåndsvisning av varselbrev på en ferdigstilt behandling. Se dokumentoversikt, eller finn brevet i tilbakekrevingsapp" }
    }

    private fun lagFeilutbetaltePerioderDto(behandlingId: UUID): FeilutbetaltePerioderDto {
        val simulering = simuleringService.simuler(behandlingId)

        val perioderMedFeilutbetaling =
                simulering.hentSammenhengendePerioderMedFeilutbetaling().map { Periode(fom = it.fom, tom = it.tom) }

        val feilutbetaltePerioderDto =
                FeilutbetaltePerioderDto(sumFeilutbetaling = simulering.feilutbetaling.toLong(),
                                         perioder = perioderMedFeilutbetaling)
        return feilutbetaltePerioderDto
    }

    fun hentBrev(behandlingId: UUID): ByteArray {
        val varseltekst = tilbakekrevingRepository.findByIdOrThrow(behandlingId).varseltekst
                          ?: throw Feil("Kan ikke finne varseltekst for behandlingId=$behandlingId",
                                        frontendFeilmelding = "Kan ikke finne varseltekst på tilbakekrevingsvalg")
        return hentBrev(behandlingId, varseltekst)
    }

}