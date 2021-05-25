package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.repository.SøknadBarnetilsynRepository
import no.nav.familie.ef.sak.repository.SøknadOvergangsstønadRepository
import no.nav.familie.ef.sak.repository.SøknadRepository
import no.nav.familie.ef.sak.repository.SøknadSkolepengerRepository
import no.nav.familie.ef.sak.repository.domain.Søknad
import no.nav.familie.ef.sak.repository.domain.SøknadMapper
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaBarnetilsyn
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaSkolepenger
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.SøknadSkolepenger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SøknadService(private val søknadRepository: SøknadRepository,
                    private val søknadOvergangsstønadRepository: SøknadOvergangsstønadRepository,
                    private val søknadSkolepengerRepository: SøknadSkolepengerRepository,
                    private val søknadBarnetilsynRepository: SøknadBarnetilsynRepository,) {

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

    @Transactional
    fun lagreSøknadForOvergangsstønad(søknad: SøknadOvergangsstønad,
                                      behandlingId: UUID,
                                      fagsakId: UUID,
                                      journalpostId: String) {
        val søknadsskjema = SøknadsskjemaMapper.tilDomene(søknad)
        søknadOvergangsstønadRepository.insert(søknadsskjema)
        søknadRepository.insert(SøknadMapper.toDomain(fagsakId.toString(), journalpostId, søknadsskjema, behandlingId))
    }

    @Transactional
    fun lagreSøknadForBarnetilsyn(søknad: SøknadBarnetilsyn, behandlingId: UUID, fagsakId: UUID, journalpostId: String) {
        val søknadsskjema = SøknadsskjemaMapper.tilDomene(søknad)
        søknadBarnetilsynRepository.insert(søknadsskjema)
        søknadRepository.insert(SøknadMapper.toDomain(fagsakId.toString(), journalpostId, søknadsskjema, behandlingId))
    }

    @Transactional
    fun lagreSøknadForSkolepenger(søknad: SøknadSkolepenger, behandlingId: UUID, fagsakId: UUID, journalpostId: String) {
        val søknadsskjema = SøknadsskjemaMapper.tilDomene(søknad)
        søknadSkolepengerRepository.insert(søknadsskjema)
        søknadRepository.insert(SøknadMapper.toDomain(fagsakId.toString(), journalpostId, søknadsskjema, behandlingId))
    }

    private fun hentSøknad(behandlingId: UUID): Søknad {
        return søknadRepository.findByBehandlingId(behandlingId)
               ?: error("Finner ikke søknad til behandling: $behandlingId")
    }

}