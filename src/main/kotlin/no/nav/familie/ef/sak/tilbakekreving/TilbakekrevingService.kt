package no.nav.familie.ef.sak.tilbakekreving

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService.Companion.MASKINELL_JOURNALFOERENDE_ENHET
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
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
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.tilbakekreving.FeilutbetaltePerioderDto
import no.nav.familie.kontrakter.felles.tilbakekreving.ForhåndsvisVarselbrevRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
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
        brukerfeilHvis(tilbakekrevingDto.valg == Tilbakekrevingsvalg.OPPRETT_MED_VARSEL
                       && tilbakekrevingDto.varseltekst.isNullOrBlank()) {
            "Må fylle ut varseltekst for å lage tilbakekreving med varsel"
        }
        brukerfeilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
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

    fun genererBrev(saksbehandling: Saksbehandling, varseltekst: String): ByteArray {
        validerIkkeFerdigstiltBehandling(saksbehandling.id)
        val feilutbetaltePerioderDto = lagFeilutbetaltePerioderDto(saksbehandling)
        val navEnhet = arbeidsfordelingService.hentNavEnhet(saksbehandling.ident)
        val request = ForhåndsvisVarselbrevRequest(varseltekst = varseltekst,
                                                   ytelsestype = Ytelsestype.valueOf(saksbehandling.stønadstype.name),
                                                   behandlendeEnhetId = navEnhet?.enhetId ?: MASKINELL_JOURNALFOERENDE_ENHET,
                                                   behandlendeEnhetsNavn = navEnhet?.enhetNavn ?: "Ukjent",
                                                   saksbehandlerIdent = SikkerhetContext.hentSaksbehandler(strict = true),
                                                   språkkode = Språkkode.NB,
                                                   vedtaksdato = LocalDate.now(),
                                                   feilutbetaltePerioderDto = feilutbetaltePerioderDto,
                                                   fagsystem = Fagsystem.EF,
                                                   eksternFagsakId = saksbehandling.eksternFagsakId.toString(),
                                                   fagsystemsbehandlingId = saksbehandling.eksternId.toString(),
                                                   ident = saksbehandling.ident,
                                                   verge = null)
        return tilbakekrevingClient.hentForhåndsvisningVarselbrev(forhåndsvisVarselbrevRequest = request)

    }

    private fun validerIkkeFerdigstiltBehandling(behandlingId: UUID) {
        brukerfeilHvis(behandlingService.hentBehandling(behandlingId).status == BehandlingStatus.FERDIGSTILT)
        { "Kan ikke generere forhåndsvisning av varselbrev på en ferdigstilt behandling." }
    }

    private fun lagFeilutbetaltePerioderDto(saksbehandling: Saksbehandling): FeilutbetaltePerioderDto {
        val simulering = simuleringService.simuler(saksbehandling)

        val perioderMedFeilutbetaling = simulering.hentSammenhengendePerioderMedFeilutbetaling()

        return FeilutbetaltePerioderDto(sumFeilutbetaling = simulering.feilutbetaling.toLong(),
                                        perioder = perioderMedFeilutbetaling)
    }

    fun genererBrevMedVarseltekstFraEksisterendeTilbakekreving(saksbehandling: Saksbehandling): ByteArray {
        val varseltekst = tilbakekrevingRepository.findByIdOrThrow(saksbehandling.id).varseltekst
                          ?: throw Feil("Kan ikke finne varseltekst for behandlingId=$saksbehandling",
                                        frontendFeilmelding = "Kan ikke finne varseltekst på tilbakekrevingsvalg")
        return genererBrev(saksbehandling, varseltekst)
    }

    fun opprettManuellTilbakekreving(fagsakId: UUID) {
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(fagsakId)
        val kanBehandlingOpprettesManuelt =
                tilbakekrevingClient.kanBehandlingOpprettesManuelt(fagsak.stønadstype, fagsak.eksternId.id)
        if (!kanBehandlingOpprettesManuelt.kanBehandlingOpprettes) {
            throw ApiFeil(kanBehandlingOpprettesManuelt.melding, HttpStatus.BAD_REQUEST)
        }

        val behandling = behandlingService.finnSisteIverksatteBehandling(fagsakId)
                         ?: throw Feil("Kan ikke opprette manuell tilbakekreving for fagsak uten iverksatt behandling. " +
                                       "fagsakId=$fagsakId")

        tilbakekrevingClient.opprettManuelTilbakekreving(fagsak.eksternId.id, behandling.eksternId.id, fagsak.stønadstype)
    }

}
