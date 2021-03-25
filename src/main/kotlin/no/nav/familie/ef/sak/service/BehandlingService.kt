package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.BehandlingDto
import no.nav.familie.ef.sak.api.dto.tilDto
import no.nav.familie.ef.sak.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.repository.*
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaBarnetilsyn
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaSkolepenger
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import no.nav.familie.kontrakter.felles.journalpost.Journalposttype
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn as SøknadBarnetilsynKontrakt
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad as SøknadOvergangsstønadKontrakt
import no.nav.familie.kontrakter.ef.søknad.SøknadSkolepenger as SøknadSkolepengerKontrakt

@Service
class BehandlingService(private val søknadRepository: SøknadRepository,
                        private val søknadsskjemaRepository: SøknadsskjemaRepository,
                        private val søknadOvergangsstønadRepository: SøknadOvergangsstønadRepository,
                        private val søknadSkolepengerRepository: SøknadSkolepengerRepository,
                        private val søknadBarnetilsynRepository: SøknadBarnetilsynRepository,
                        private val behandlingRepository: BehandlingRepository,
                        private val behandlingshistorikkService: BehandlingshistorikkService) {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")


    fun hentAktivIdent(behandlingId: UUID): String = behandlingRepository.finnAktivIdent(behandlingId)

    @Transactional
    fun lagreSøknadForOvergangsstønad(søknad: SøknadOvergangsstønadKontrakt,
                                      behandlingId: UUID,
                                      fagsakId: UUID,
                                      journalpostId: String) {
        val søknadsskjema = SøknadsskjemaMapper.tilDomene(søknad)
        søknadsskjemaRepository.insert(søknadsskjema)
        søknadRepository.insert(SøknadMapper.toDomain(fagsakId.toString(), journalpostId, søknadsskjema, behandlingId))
    }

    @Transactional
    fun lagreSøknadForBarnetilsyn(søknad: SøknadBarnetilsynKontrakt, behandlingId: UUID, fagsakId: UUID, journalpostId: String) {
        val søknadsskjema = SøknadsskjemaMapper.tilDomene(søknad)
        søknadsskjemaRepository.insert(søknadsskjema)
        søknadRepository.insert(SøknadMapper.toDomain(fagsakId.toString(), journalpostId, søknadsskjema, behandlingId))
    }

    @Transactional
    fun lagreSøknadForSkolepenger(søknad: SøknadSkolepengerKontrakt, behandlingId: UUID, fagsakId: UUID, journalpostId: String) {
        val søknadsskjema = SøknadsskjemaMapper.tilDomene(søknad)
        søknadsskjemaRepository.insert(søknadsskjema)
        søknadRepository.insert(SøknadMapper.toDomain(fagsakId.toString(), journalpostId, søknadsskjema, behandlingId))
    }

    @Transactional
    fun opprettBehandling(behandlingType: BehandlingType,
                          fagsakId: UUID,
                          søknad: SøknadOvergangsstønadKontrakt,
                          journalpost: Journalpost): Behandling {
        val behandling = behandlingRepository.insert(Behandling(fagsakId = fagsakId,
                                                                type = behandlingType,
                                                                steg = StegType.VILKÅR,
                                                                status = BehandlingStatus.OPPRETTET,
                                                                resultat = BehandlingResultat.IKKE_SATT,
                                                                journalposter = setOf(Behandlingsjournalpost(journalpost.journalpostId,
                                                                                                             journalpost.journalposttype))))

        lagreSøknadForOvergangsstønad(søknad, behandling.id, fagsakId, journalpost.journalpostId)
        return behandling
    }

    fun opprettBehandling(behandlingType: BehandlingType, fagsakId: UUID): Behandling {
        return behandlingRepository.insert(Behandling(fagsakId = fagsakId,
                                                      type = behandlingType,
                                                      steg = StegType.VILKÅR,
                                                      status = BehandlingStatus.OPPRETTET,
                                                      resultat = BehandlingResultat.IKKE_SATT))
    }

    fun hentBehandling(behandlingId: UUID): Behandling = behandlingRepository.findByIdOrThrow(behandlingId)

    fun hentBehandlingStatus(behandlingId: UUID): BehandlingStatus = behandlingRepository.finnStatus(behandlingId)

    fun hentOvergangsstønad(behandlingId: UUID): SøknadsskjemaOvergangsstønad {
        val søknad = hentSøknad(behandlingId)
        return søknadOvergangsstønadRepository.findByIdOrThrow(søknad.soknadsskjemaId)
    }

    fun hentSkolepenger(behandlingId: UUID): SøknadsskjemaSkolepenger {
        val søknad = hentSøknad(behandlingId)
        return søknadSkolepengerRepository.findByIdOrThrow(søknad.soknadsskjemaId)
    }

    fun hentBarnetilsyn(behandlingId: UUID): SøknadsskjemaBarnetilsyn {
        val søknad = hentSøknad(behandlingId)
        return søknadBarnetilsynRepository.findByIdOrThrow(søknad.soknadsskjemaId)
    }

    fun oppdaterStatusPåBehandling(behandling: Behandling, status: BehandlingStatus) {
        val behandlingId = behandling.id
        secureLogger.info("${SikkerhetContext.hentSaksbehandler()} endrer status på behandling=$behandlingId " +
                          "fra ${behandling.status} til $status")

        if (!behandlingRepository.oppdaterStatus(behandlingId, status, behandling.status)) {
            throw Feil("Fikk ikke oppdatert behandling=${behandlingId} med status=$status")
        }
    }

    fun oppdaterStegPåBehandling(behandling: Behandling, steg: StegType) {
        val behandlingId = behandling.id
        secureLogger.info("${SikkerhetContext.hentSaksbehandler()} endrer steg på behandling=$behandlingId " +
                          "fra ${behandling.steg} til $steg")
        if (!behandlingRepository.oppdaterSteg(behandlingId, steg, behandling.steg)) {
            throw Feil("Fikk ikke oppdatert behandling=${behandlingId} med steg=$steg")
        }
    }

    private fun hentSøknad(behandlingId: UUID): Søknad {
        return søknadRepository.findByBehandlingId(behandlingId)
               ?: error("Finner ikke søknad til behandling: $behandlingId")
    }

    fun hentBehandlinger(fagsakId: UUID): List<BehandlingDto> {
        return behandlingRepository.findByFagsakId(fagsakId).map(Behandling::tilDto)
    }

    fun oppdaterJournalpostIdPåBehandling(journalpostId: String, journalposttype: Journalposttype, behandling: Behandling) {
        behandling.journalposter = behandling.journalposter +
                                   Behandlingsjournalpost(journalpostId = journalpostId,
                                                          sporbar = Sporbar(),
                                                          journalpostType = journalposttype)
        behandlingRepository.update(behandling)
    }

    fun annullerBehandling(behandlingId: UUID): Behandling {
        val behandling = hentBehandling(behandlingId)
        validerAtBehandlingenKanAnnulleres(behandling)
        val oppdatertBehandling = behandling.copy(status = BehandlingStatus.FERDIGSTILT,
                                                  resultat = BehandlingResultat.ANNULLERT,
                                                  steg = StegType.BEHANDLING_FERDIGSTILT)
        behandlingshistorikkService.opprettHistorikkInnslag(oppdatertBehandling)
        return behandlingRepository.update(oppdatertBehandling)
    }

    private fun validerAtBehandlingenKanAnnulleres(behandling: Behandling) {
        if (!behandling.kanAnnulleres()) {
            throw Feil(
                    message = "Kan ikke annullere en behandling med status ${behandling.status} for ${behandling.type}",
                    frontendFeilmelding = "Kan ikke annullere en behandling med status ${behandling.status} for ${behandling.type}",
                    httpStatus = HttpStatus.BAD_REQUEST,
                    throwable = null
            )
        }
    }

}
