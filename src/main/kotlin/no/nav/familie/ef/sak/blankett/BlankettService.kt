package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Fil
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.service.*
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
                      private val personopplysningerService: PersonopplysningerService) {

    fun opprettBlankettBehandling(journalpostId: String) : Behandling {
        val journalpost = journalføringService.hentJournalpost(journalpostId)
        val personIdent = journalføringService.hentIdentForJournalpost(journalpost)
        tilgangService.validerTilgangTilPersonMedBarn(personIdent)
        val søknad = journalføringService.hentSøknadFraJournalpostForOvergangsstønad(journalpostId)
        val fagsak = fagsakService.hentEllerOpprettFagsak(personIdent, Stønadstype.OVERGANGSSTØNAD)
        return behandlingService.opprettBehandling(BehandlingType.BLANKETT, fagsak.id, søknad, journalpost)
    }

    fun lagBlankett(behandlingId: UUID): ByteArray {
        val blankettPdfRequest = BlankettPdfRequest(lagPersonopplysningerDto(behandlingId),
                                                    hentInngangsvilkårDto(behandlingId))
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

    private fun hentInngangsvilkårDto(behandlingId: UUID) = vurderingService.hentInngangsvilkår(behandlingId)

    private fun lagPersonopplysningerDto(behandlingId: UUID): PersonopplysningerDto {
        val ident = fagsakService.hentFagsak(behandlingService.hentBehandling(behandlingId).fagsakId).hentAktivIdent()
        return PersonopplysningerDto(hentGjeldendeNavn(ident), ident)
    }

    private fun hentGjeldendeNavn(hentAktivIdent: String): String {
        val navnMap = personopplysningerService.hentGjeldeneNavn(listOf(hentAktivIdent))
        return navnMap.getValue(hentAktivIdent)
    }

}