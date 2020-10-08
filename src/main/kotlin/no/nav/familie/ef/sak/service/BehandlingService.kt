package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.repository.*
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.service.steg.StegType
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.ef.sak.SakRequest
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class BehandlingService(private val søknadRepository: SøknadRepository,
                        private val vedleggRepository: VedleggRepository,
                        private val fagsakRepository: FagsakRepository,
                        private val behandlingRepository: BehandlingRepository) {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    fun mottaSakOvergangsstønad(sak: SakRequest<SøknadOvergangsstønad>, vedleggMap: Map<String, ByteArray>): Behandling {
        val ident = sak.søknad.søknad.personalia.verdi.fødselsnummer.verdi.verdi
        val behandling = hentBehandling(ident)
        mottaSøknad(SøknadMapper.toDomain(sak.saksnummer, sak.journalpostId, sak.søknad.søknad, behandling.id),
                    sak,
                    vedleggMap)
        return behandling
    }

    @Transactional
    fun mottaSakBarnetilsyn(sak: SakRequest<SøknadBarnetilsyn>, vedleggMap: Map<String, ByteArray>): UUID {
        val ident = sak.søknad.søknad.personalia.verdi.fødselsnummer.verdi.verdi
        val behandling = hentBehandling(ident)
        mottaSøknad(SøknadMapper.toDomain(sak.saksnummer, sak.journalpostId, sak.søknad.søknad, behandling.id),
                    sak,
                    vedleggMap)
        return behandling.id
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

    fun hentBehandling(behandlingId: UUID): Behandling = behandlingRepository.findByIdOrThrow(behandlingId)

    fun hentOvergangsstønad(behandlingId: UUID): SøknadOvergangsstønad {
        val søknad = hentSøknad(behandlingId)
        return SøknadMapper.pakkOppOvergangsstønad(søknad).søknad
    }

    fun oppdaterStatusPåBehandling(behandlingId: UUID, status: BehandlingStatus): Behandling {
        val behandling = hentBehandling(behandlingId)
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} endrer status på behandling $behandlingId fra ${behandling.status} til $status")

        behandling.status = status
        return behandlingRepository.update(behandling)
    }

    fun oppdaterStegPåBehandling(behandlingId: UUID, steg: StegType): Behandling {
        val behandling = hentBehandling(behandlingId)
        logger.info("${SikkerhetContext.hentSaksbehandlerNavn()} endrer steg på behandling $behandlingId fra ${behandling.steg} til $steg")

        behandling.steg = steg
        return behandlingRepository.update(behandling)
    }


    private fun hentSøknad(behandlingId: UUID): Søknad {
        return søknadRepository.findByBehandlingId(behandlingId) ?: error("Finner ikke søknad til behandling: $behandlingId")
    }

}
