package no.nav.familie.ef.sak.opplysninger.søknad

import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.domene.Sporbar
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Søknad
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadMapper
import no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad.SøknadsskjemaBarnetilsyn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad.SøknadsskjemaSkolepenger
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.SøknadSkolepenger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
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

    fun finnDatoMottattForSøknad(behandlingId: UUID): LocalDateTime {
        return søknadRepository.finnDatoMottattForSøknad(behandlingId)
    }

    /**
     * Vi kopierer nå for å ikke bryte mye annen funksjonalitet, men burde vurdere OM vi MÅ ha en søknad i en revurdering
     */
    @Transactional
    fun kopierSøknad(forrigeBehandlingId: UUID, nyBehandlingId: UUID) {
        val søknad = hentSøknad(forrigeBehandlingId)
        søknadRepository.insert(søknad.copy(id = UUID.randomUUID(),
                                            behandlingId = nyBehandlingId,
                                            sporbar = Sporbar()))
    }

    @Transactional
    fun lagreSøknadForOvergangsstønad(søknad: SøknadOvergangsstønad,
                                      behandlingId: UUID,
                                      fagsakId: UUID,
                                      journalpostId: String) {
        val søknadsskjema = SøknadsskjemaMapper.tilDomene(søknad)
        søknadOvergangsstønadRepository.insert(søknadsskjema)
        søknadRepository.insert(SøknadMapper.toDomain(journalpostId, søknadsskjema, behandlingId))
    }

    @Transactional
    fun lagreSøknadForBarnetilsyn(søknad: SøknadBarnetilsyn, behandlingId: UUID, fagsakId: UUID, journalpostId: String) {
        val søknadsskjema = SøknadsskjemaMapper.tilDomene(søknad)
        søknadBarnetilsynRepository.insert(søknadsskjema)
        søknadRepository.insert(SøknadMapper.toDomain(journalpostId, søknadsskjema, behandlingId))
    }

    @Transactional
    fun lagreSøknadForSkolepenger(søknad: SøknadSkolepenger, behandlingId: UUID, fagsakId: UUID, journalpostId: String) {
        val søknadsskjema = SøknadsskjemaMapper.tilDomene(søknad)
        søknadSkolepengerRepository.insert(søknadsskjema)
        søknadRepository.insert(SøknadMapper.toDomain(journalpostId, søknadsskjema, behandlingId))
    }

    private fun hentSøknad(behandlingId: UUID): Søknad {
        return søknadRepository.findByBehandlingId(behandlingId)
               ?: error("Finner ikke søknad til behandling: $behandlingId")
    }

}