package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.repository.*
import no.nav.familie.ef.sak.repository.domain.*
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
                        private val fagsakRepository: FagsakRepository,
                        private val behandlingRepository: BehandlingRepository,
                        private val customRepository: CustomRepository) {

    val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    @Transactional
    fun mottaSakOvergangsstønad(sak: SakRequest<SøknadOvergangsstønad>, vedleggMap: Map<String, ByteArray>): UUID {
        val ident = sak.søknad.søknad.personalia.verdi.fødselsnummer.verdi.verdi
        val behandling = hentBehandling(ident)
        mottaSøknad(SøknadMapper.toDomain(sak.saksnummer, sak.journalpostId, sak.søknad.søknad, behandling.id),
                    sak,
                    vedleggMap)
        return behandling.id
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
        val save = customRepository.persist(domenesak)
        val vedleggListe = sak.søknad.vedlegg.map {
            val vedlegg = vedleggMap[it.id] ?: error("Finner ikke vedlegg ${it.id}")
            VedleggMapper.toDomain(save.id, it, vedlegg)
        }
        vedleggListe.forEach { customRepository.persist(it) }
        logger.info("lagret ${save.id} sammen med ${vedleggListe.size} vedlegg")
    }

    //tmp for å gjøre det mulig å støtte mottaSakOvergangsstønad
    private fun hentBehandling(ident: String): Behandling {
        val fagsak = fagsakRepository.findBySøkerIdent(ident, Stønadstype.OVERGANGSSTØNAD)
                     ?: customRepository.persist(Fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD))

        return customRepository.persist(Behandling(fagsakId = fagsak.id,
                                                   type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                   steg = BehandlingSteg.KOMMER_SENDERE,
                                                   status = BehandlingStatus.OPPRETTET))
    }

    fun hentBehandling(behandlingId: UUID): Behandling = behandlingRepository.findByIdOrThrow(behandlingId)

    fun hentOvergangsstønad(behandlingId: UUID): SøknadOvergangsstønad {
        val søknad = hentSøknad(behandlingId)
        return SøknadMapper.pakkOppOvergangsstønad(søknad).søknad
    }

    private fun hentSøknad(behandlingId: UUID): Søknad {
        return søknadRepository.findByBehandlingId(behandlingId) ?: error("Finner ikke søknad til behandling: $behandlingId")
    }

}
