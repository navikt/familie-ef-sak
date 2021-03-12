package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.repository.OppgaveRepository
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.service.*
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.stereotype.Service
import java.util.*

@Service
class BlankettService(private val tilgangService: TilgangService,
                      private val vurderingService: VurderingService,
                      private val blankettClient: BlankettClient,
                      private val blankettRepository: BlankettRepository,
                      private val journalføringService: JournalføringService,
                      private val behandlingService: BehandlingService,
                      private val fagsakService: FagsakService,
                      private val personopplysningerService: PersonopplysningerService,
                      private val oppgaveRepository: OppgaveRepository) {

    fun opprettBlankettBehandling(journalpostId: String, oppgaveId: Long) : Behandling {
        val journalpost = journalføringService.hentJournalpost(journalpostId)
        val personIdent = journalføringService.hentIdentForJournalpost(journalpost)
        tilgangService.validerTilgangTilPersonMedBarn(personIdent)
        val søknad = journalføringService.hentSøknadFraJournalpostForOvergangsstønad(journalpostId)
        val fagsak = fagsakService.hentEllerOpprettFagsak(personIdent, Stønadstype.OVERGANGSSTØNAD)
        val behandling = behandlingService.opprettBehandling(BehandlingType.BLANKETT, fagsak.id, søknad, journalpost)
        opprettEfOppgave(behandling.id, oppgaveId)
        lagreTomBlankett(behandling.id)
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
                                                    hentVilkårDto(behandlingId))
        val blankettPdfAsByteArray = blankettClient.genererBlankett(blankettPdfRequest)
        oppdaterBlankett(behandlingId, blankettPdfAsByteArray)
        return blankettPdfAsByteArray
    }

    fun oppdaterBlankett(behandlingId: UUID, pdf: ByteArray) : Blankett {
        val blankett = Blankett(behandlingId, Fil(pdf))
        return blankettRepository.update(blankett)
    }

    fun lagreTomBlankett(behandlingId: UUID) {
        val blankett = Blankett(behandlingId, Fil(byteArrayOf()))
        blankettRepository.insert(blankett)
    }

    private fun hentVilkårDto(behandlingId: UUID) = vurderingService.hentVilkår(behandlingId)

    private fun lagPersonopplysningerDto(behandlingId: UUID): PersonopplysningerDto {
        val ident = fagsakService.hentFagsak(behandlingService.hentBehandling(behandlingId).fagsakId).hentAktivIdent()
        return PersonopplysningerDto(hentGjeldendeNavn(ident), ident)
    }

    private fun hentGjeldendeNavn(hentAktivIdent: String): String {
        val navnMap = personopplysningerService.hentGjeldeneNavn(listOf(hentAktivIdent))
        return navnMap.getValue(hentAktivIdent)
    }

    fun hentBlankettPdf(behandlingId: UUID): Blankett {
        return blankettRepository.findByIdOrThrow(behandlingId);
    }

}