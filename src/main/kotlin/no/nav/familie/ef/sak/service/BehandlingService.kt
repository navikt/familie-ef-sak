package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.api.fagsak.BehandlingDto
import no.nav.familie.ef.sak.repository.*
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.repository.domain.søknad.ISøknadsskjema
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaBarnetilsyn
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaSkolepenger
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.ef.sak.SakRequest
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
                        private val vedleggRepository: VedleggRepository,
                        private val fagsakRepository: FagsakRepository,
                        private val søknadsskjemaRepository: SøknadsskjemaRepository,
                        private val søknadOvergangsstønadRepository: SøknadOvergangsstønadRepository,
                        private val søknadSkolepengerRepository: SøknadSkolepengerRepository,
                        private val søknadBarnetilsynRepository: SøknadBarnetilsynRepository,
                        private val behandlingRepository: BehandlingRepository) {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    fun mottaSakOvergangsstønad(sak: SakRequest<SøknadOvergangsstønadKontrakt>, vedleggMap: Map<String, ByteArray>): Behandling {
        val søknadsskjema = SøknadsskjemaMapper.tilDomene(sak.søknad.søknad)
        return mottaSak(sak, vedleggMap, søknadsskjema)
    }

    @Transactional
    fun mottaSakBarnetilsyn(sak: SakRequest<SøknadBarnetilsynKontrakt>, vedleggMap: Map<String, ByteArray>): Behandling {
        val søknadsskjema = SøknadsskjemaMapper.tilDomene(sak.søknad.søknad)
        return mottaSak(sak, vedleggMap, søknadsskjema)
    }

    fun mottaSak(sak: SakRequest<*>, vedleggMap: Map<String, ByteArray>, søknadsskjema: ISøknadsskjema): Behandling {
        søknadsskjemaRepository.insert(søknadsskjema)
        val ident = søknadsskjema.fødselsnummer
        val behandling = hentBehandling(ident)
        mottaSøknad(SøknadMapper.toDomain(sak.saksnummer, sak.journalpostId, søknadsskjema, behandling.id), sak, vedleggMap)
        return behandling
    }

    private fun <T> mottaSøknad(domenesak: Søknad,
                                sak: SakRequest<T>,
                                vedleggMap: Map<String, ByteArray>) {
        val save = søknadRepository.insert(domenesak)
        val vedleggListe = sak.søknad.vedlegg.map {
            val vedlegg = vedleggMap[it.id] ?: error("Finner ikke vedlegg ${it.id}")
            VedleggMapper.toDomain(save.id, it, vedlegg)
        }
        vedleggListe.forEach { vedleggRepository.insert(it) }
        logger.info("lagret ${save.id} sammen med ${vedleggListe.size} vedlegg")
    }

    //tmp for å gjøre det mulig å støtte mottaSakOvergangsstønad
    private fun hentBehandling(ident: String): Behandling {
        val fagsak = fagsakRepository.findBySøkerIdent(ident, Stønadstype.OVERGANGSSTØNAD)
                     ?: fagsakRepository.insert(Fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD))

        return behandlingRepository.insert(Behandling(fagsakId = fagsak.id,
                                                      type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                      steg = StegType.REGISTRERE_OPPLYSNINGER,
                                                      status = BehandlingStatus.OPPRETTET))
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
        return søknadRepository.findByBehandlingId(behandlingId) ?: error("Finner ikke søknad til behandling: $behandlingId")
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
