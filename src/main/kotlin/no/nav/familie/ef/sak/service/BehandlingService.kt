package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.api.fagsak.BehandlingDto
import no.nav.familie.ef.sak.repository.*
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaBarnetilsyn
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaSkolepenger
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.ef.søknad.SøknadSkolepenger as SøknadSkolepengerKontrakt
import no.nav.familie.kontrakter.felles.journalpost.Journalpost
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn as SøknadBarnetilsynKontrakt
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad as SøknadOvergangsstønadKontrakt

@Service
class BehandlingService(private val søknadRepository: SøknadRepository,
                        private val søknadsskjemaRepository: SøknadsskjemaRepository,
                        private val søknadOvergangsstønadRepository: SøknadOvergangsstønadRepository,
                        private val søknadSkolepengerRepository: SøknadSkolepengerRepository,
                        private val søknadBarnetilsynRepository: SøknadBarnetilsynRepository,
                        private val behandlingRepository: BehandlingRepository) {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    fun mottaSøknadForOvergangsstønad(søknad: SøknadOvergangsstønadKontrakt,
                                      behandlingId: UUID,
                                      fagsakId: UUID,
                                      journalpostId: String) {
        val søknadsskjema = SøknadsskjemaMapper.tilDomene(søknad)
        søknadsskjemaRepository.insert(søknadsskjema)
        søknadRepository.insert(SøknadMapper.toDomain(fagsakId.toString(), journalpostId, søknadsskjema, behandlingId))
    }

    @Transactional
    fun mottaSøknadForBarnetilsyn(søknad: SøknadBarnetilsynKontrakt, behandlingId: UUID, fagsakId: UUID, journalpostId: String) {
        val søknadsskjema = SøknadsskjemaMapper.tilDomene(søknad)
        søknadsskjemaRepository.insert(søknadsskjema)
        søknadRepository.insert(SøknadMapper.toDomain(fagsakId.toString(), journalpostId, søknadsskjema, behandlingId))
    }

    @Transactional
    fun mottaSøknadForSkolepenger(søknad: SøknadSkolepengerKontrakt, behandlingId: UUID, fagsakId: UUID, journalpostId: String) {
        val søknadsskjema = SøknadsskjemaMapper.tilDomene(søknad)
        søknadsskjemaRepository.insert(søknadsskjema)
        søknadRepository.insert(SøknadMapper.toDomain(fagsakId.toString(), journalpostId, søknadsskjema, behandlingId))
    }


    fun opprettBehandling(behandlingType: BehandlingType, fagsakId: UUID): Behandling {
        return behandlingRepository.insert(Behandling(fagsakId = fagsakId,
                                                      type = behandlingType,
                                                      steg = StegType.REGISTRERE_OPPLYSNINGER,
                                                      status = BehandlingStatus.OPPRETTET))
    }

    fun hentBehandling(behandlingId: UUID): Behandling = behandlingRepository.findByIdOrThrow(behandlingId)

    fun hentOvergangsstønad(behandlingId: UUID): SøknadsskjemaOvergangsstønad {
        val søknad = hentSøknad(behandlingId)
        return søknadOvergangsstønadRepository.findByIdOrThrow(søknad.soknadsskjemaId)
    }

    fun hentSkolpepnger(behandlingId: UUID): SøknadsskjemaSkolepenger {
        val søknad = hentSøknad(behandlingId)
        return søknadSkolepengerRepository.findByIdOrThrow(søknad.soknadsskjemaId)
    }

    fun hentBarnetilsyn(behandlingId: UUID): SøknadsskjemaBarnetilsyn {
        val søknad = hentSøknad(behandlingId)
        return søknadBarnetilsynRepository.findByIdOrThrow(søknad.soknadsskjemaId)
    }

    fun oppdaterStatusPåBehandling(behandlingId: UUID, status: BehandlingStatus): Behandling {
        val behandling = hentBehandling(behandlingId)
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} endrer status på behandling $behandlingId " +
                    "fra ${behandling.status} til $status")

        behandling.status = status
        return behandlingRepository.update(behandling)
    }

    fun oppdaterStegPåBehandling(behandlingId: UUID, steg: StegType): Behandling {
        val behandling = hentBehandling(behandlingId)
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} endrer steg på behandling $behandlingId " +
                    "fra ${behandling.steg} til $steg")

        behandling.steg = steg
        return behandlingRepository.update(behandling)
    }


    private fun hentSøknad(behandlingId: UUID): Søknad {
        return søknadRepository.findByBehandlingId(behandlingId)
               ?: error("Finner ikke søknad til behandling: $behandlingId")
    }

    fun hentBehandlinger(fagsakId: UUID): List<BehandlingDto> {
        return behandlingRepository.findByFagsakId(fagsakId).map {
            BehandlingDto(id = it.id,
                          type = it.type,
                          status = it.status,
                          aktiv = it.aktiv,
                          sistEndret = it.sporbar.endret.endretTid.toLocalDate())
        }
    }

    fun oppdaterJournalpostIdPåBehandling(journalpost: Journalpost, behandling: Behandling) {
        behandling.journalposter = behandling.journalposter + Behandlingsjournalpost(
                journalpostId = journalpost.journalpostId,
                sporbar = Sporbar(),
                journalpostType = journalpost.journalposttype)
        behandlingRepository.update(behandling)
    }

}
