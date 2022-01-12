package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.felles.domain.Fil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.journalføring.JournalføringService
import no.nav.familie.ef.sak.oppgave.Oppgave
import no.nav.familie.ef.sak.oppgave.OppgaveRepository
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadDatoerDto
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.tilVedtakDto
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BlankettService(private val tilgangService: TilgangService,
                      private val vurderingService: VurderingService,
                      private val blankettClient: BlankettClient,
                      private val blankettRepository: BlankettRepository,
                      private val journalføringService: JournalføringService,
                      private val behandlingService: BehandlingService,
                      private val søknadService: SøknadService,
                      private val fagsakService: FagsakService,
                      private val personopplysningerService: PersonopplysningerService,
                      private val oppgaveRepository: OppgaveRepository,
                      private val grunnlagsdataService: GrunnlagsdataService,
                      private val vedtakService: VedtakService) {

    @Transactional
    fun opprettBlankettBehandling(journalpostId: String, oppgaveId: Long): Behandling {
        val journalpost = journalføringService.hentJournalpost(journalpostId)
        val personIdent = journalføringService.hentIdentForJournalpost(journalpost)
        tilgangService.validerTilgangTilPersonMedBarn(personIdent)
        val søknad = journalføringService.hentSøknadFraJournalpostForOvergangsstønad(journalpostId)
        val fagsak = fagsakService.hentEllerOpprettFagsak(personIdent, Stønadstype.OVERGANGSSTØNAD)
        val behandling = behandlingService.opprettBehandlingForBlankett(BehandlingType.BLANKETT, fagsak.id, søknad, journalpost)
        opprettEfOppgave(behandling.id, oppgaveId)
        grunnlagsdataService.opprettGrunnlagsdata(behandling.id)

        return behandling
    }

    private fun opprettEfOppgave(behandlingId: UUID, oppgaveId: Long) {
        val oppgave = Oppgave(gsakOppgaveId = oppgaveId,
                              behandlingId = behandlingId,
                              type = Oppgavetype.BehandleSak)
        oppgaveRepository.insert(oppgave)
    }

    fun lagBlankett(behandlingId: UUID): ByteArray {
        val blankettPdfRequest = BlankettPdfRequest(lagPersonopplysningerDto(behandlingId),
                                                    hentVilkårDto(behandlingId),
                                                    hentVedtak(behandlingId),
                                                    lagSøknadsdatoer(behandlingId)
        )
        val blankettPdfAsByteArray = blankettClient.genererBlankett(blankettPdfRequest)
        oppdaterEllerOpprettBlankett(behandlingId, blankettPdfAsByteArray)
        return blankettPdfAsByteArray
    }

    fun oppdaterEllerOpprettBlankett(behandlingId: UUID, pdf: ByteArray): Blankett {
        val blankett = Blankett(behandlingId, Fil(pdf))
        if (blankettRepository.existsById(behandlingId)) {
            return blankettRepository.update(blankett)
        }
        return blankettRepository.insert(blankett)
    }

    private fun hentVilkårDto(behandlingId: UUID) = vurderingService.hentEllerOpprettVurderinger(behandlingId)

    private fun lagSøknadsdatoer(behandlingId: UUID): SøknadDatoerDto? {
        val overgangsstønad = søknadService.hentOvergangsstønad(behandlingId) ?: return null
        return SøknadDatoerDto(
                søknadsdato = overgangsstønad.datoMottatt,
                søkerStønadFra = overgangsstønad.søkerFra
        )

    }

    private fun lagPersonopplysningerDto(behandlingId: UUID): PersonopplysningerDto {
        val aktivIdent = behandlingService.hentAktivIdent(behandlingId)
        return PersonopplysningerDto(hentGjeldendeNavn(aktivIdent), aktivIdent)
    }

    private fun hentVedtak(behandlingId: UUID): VedtakDto {
        return vedtakService.hentVedtak(behandlingId).tilVedtakDto()
    }

    private fun hentGjeldendeNavn(hentAktivIdent: String): String {
        val navnMap = personopplysningerService.hentGjeldeneNavn(listOf(hentAktivIdent))
        return navnMap.getValue(hentAktivIdent)
    }

    fun hentBlankettPdf(behandlingId: UUID): Blankett? {
        return blankettRepository.findByIdOrNull(behandlingId)
    }

}