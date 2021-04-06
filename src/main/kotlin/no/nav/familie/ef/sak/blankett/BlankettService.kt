package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.api.beregning.VedtakDto
import no.nav.familie.ef.sak.api.beregning.VedtakService
import no.nav.familie.ef.sak.api.dto.SøknadDatoerDto
import no.nav.familie.ef.sak.repository.OppgaveRepository
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.service.*
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
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
                      private val oppgaveRepository: OppgaveRepository,
                      private val vedtakService: VedtakService) {

    @Transactional
    fun opprettBlankettBehandling(journalpostId: String, oppgaveId: Long): Behandling {
        val journalpost = journalføringService.hentJournalpost(journalpostId)
        val personIdent = journalføringService.hentIdentForJournalpost(journalpost)
        tilgangService.validerTilgangTilPersonMedBarn(personIdent)
        val søknad = journalføringService.hentSøknadFraJournalpostForOvergangsstønad(journalpostId)
        val fagsak = fagsakService.hentEllerOpprettFagsak(personIdent, Stønadstype.OVERGANGSSTØNAD)
        val behandling = behandlingService.opprettBehandling(BehandlingType.BLANKETT, fagsak.id, søknad, journalpost)
        opprettEfOppgave(behandling.id, oppgaveId)

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
                                                    lagVedtakDto(behandlingId),
                                                    lagSøknadsdatoer(behandlingId)
        )
        val blankettPdfAsByteArray = blankettClient.genererBlankett(blankettPdfRequest)
        oppdaterBlankett(behandlingId, blankettPdfAsByteArray)
        return blankettPdfAsByteArray
    }

    fun oppdaterBlankett(behandlingId: UUID, pdf: ByteArray): Blankett {
        val blankett = Blankett(behandlingId, Fil(pdf))
        if (blankettRepository.existsById(behandlingId)) {
            return blankettRepository.update(blankett)
        }
        return blankettRepository.insert(blankett)
    }

    private fun hentVilkårDto(behandlingId: UUID) = vurderingService.hentEllerOpprettVurderinger(behandlingId)

    private fun lagSøknadsdatoer(behandlingId: UUID) : SøknadDatoerDto {
        val overgangsstønad = behandlingService.hentOvergangsstønad(behandlingId)
        return SøknadDatoerDto(
                søknadsdato = overgangsstønad.datoMottatt,
                søkerStønadFra = overgangsstønad.søkerFra
        )

    }

    private fun lagPersonopplysningerDto(behandlingId: UUID): PersonopplysningerDto {
        val aktivIdent = behandlingService.hentAktivIdent(behandlingId)
        return PersonopplysningerDto(hentGjeldendeNavn(aktivIdent), aktivIdent)
    }

    private fun lagVedtakDto(behandlingId: UUID): VedtakDto {
        return vedtakService.hentVedtak(behandlingId)
                .let {
                    VedtakDto(it.resultatType,
                              it.periodeBegrunnelse,
                              it.inntektBegrunnelse,
                              it.perioder.perioder,
                              it.inntekter.inntekter)
                }
    }

    private fun hentGjeldendeNavn(hentAktivIdent: String): String {
        val navnMap = personopplysningerService.hentGjeldeneNavn(listOf(hentAktivIdent))
        return navnMap.getValue(hentAktivIdent)
    }

    fun hentBlankettPdf(behandlingId: UUID): Blankett? {
        return blankettRepository.findByIdOrNull(behandlingId)
    }

}