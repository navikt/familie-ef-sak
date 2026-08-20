package no.nav.familie.ef.sak.opplysninger.søknad

import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Søknad
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadMapper
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadType
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaBarnetilsyn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaSkolepenger
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Søknadsverdier
import no.nav.familie.ef.sak.opplysninger.søknad.domain.tilSøknadsverdier
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.ef.søknad.SøknadBarnetilsyn
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønadRegelendring2026
import no.nav.familie.kontrakter.ef.søknad.SøknadSkolepenger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class SøknadService(
    private val søknadRepository: SøknadRepository,
    private val søknadOvergangsstønadRepository: SøknadOvergangsstønadRepository,
    private val søknadSkolepengerRepository: SøknadSkolepengerRepository,
    private val søknadBarnetilsynRepository: SøknadBarnetilsynRepository,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentSøknadsgrunnlag(behandlingId: UUID): Søknadsverdier? {
        val søknad = hentSøknad(behandlingId) ?: return null
        return when (søknad.type) {
            SøknadType.OVERGANGSSTØNAD -> hentOvergangsstønad(behandlingId)?.tilSøknadsverdier()
            SøknadType.BARNETILSYN -> hentBarnetilsyn(behandlingId)?.tilSøknadsverdier()
            SøknadType.SKOLEPENGER -> hentSkolepenger(behandlingId)?.tilSøknadsverdier()
        }
    }

    fun hentOvergangsstønad(behandlingId: UUID): SøknadsskjemaOvergangsstønad? {
        val søknad = hentSøknad(behandlingId) ?: return null
        return søknadOvergangsstønadRepository.findByIdOrThrow(søknad.soknadsskjemaId)
    }

    fun hentSkolepenger(behandlingId: UUID): SøknadsskjemaSkolepenger? {
        val søknad = hentSøknad(behandlingId) ?: return null
        return søknadSkolepengerRepository.findByIdOrThrow(søknad.soknadsskjemaId)
    }

    fun hentBarnetilsyn(behandlingId: UUID): SøknadsskjemaBarnetilsyn? {
        val søknad = hentSøknad(behandlingId) ?: return null
        return søknadBarnetilsynRepository.findByIdOrThrow(søknad.soknadsskjemaId)
    }

    fun finnDatoMottattForSøknad(behandlingId: UUID): LocalDateTime? = søknadRepository.finnDatoMottattForSøknad(behandlingId)

    /**
     * Vi kopierer nå for å ikke bryte mye annen funksjonalitet, men burde vurdere OM vi MÅ ha en søknad i en revurdering
     */
    @Transactional
    fun kopierSøknad(
        forrigeBehandlingId: UUID,
        nyBehandlingId: UUID,
    ) {
        val søknad = hentSøknad(forrigeBehandlingId)
        if (søknad == null) {
            logger.info("Finner ingen søknad på forrige behandling=$forrigeBehandlingId")
            return
        }
        søknadRepository.insert(
            søknad.copy(
                id = UUID.randomUUID(),
                behandlingId = nyBehandlingId,
                sporbar = Sporbar(),
            ),
        )
    }

    @Transactional
    fun lagreSøknadForOvergangsstønad(
        søknad: SøknadOvergangsstønad,
        behandlingId: UUID,
        fagsakId: UUID,
        journalpostId: String,
    ) {
        val søknadsskjema = SøknadsskjemaMapper.tilDomene(søknad)
        søknadOvergangsstønadRepository.insert(søknadsskjema)
        søknadRepository.insert(SøknadMapper.toDomain(journalpostId, søknadsskjema, behandlingId))
    }

    @Transactional
    fun lagreSøknadForOvergangsstønadRegelendring2026(
        søknad: SøknadOvergangsstønadRegelendring2026,
        behandlingId: UUID,
        journalpostId: String,
    ) {
        val søknadsskjema = SøknadsskjemaMapper.tilDomene(søknad)
        søknadOvergangsstønadRepository.insert(søknadsskjema)
        søknadRepository.insert(SøknadMapper.toDomain(journalpostId, søknadsskjema, behandlingId))
    }

    @Transactional
    fun lagreSøknadForBarnetilsyn(
        søknad: SøknadBarnetilsyn,
        behandlingId: UUID,
        fagsakId: UUID,
        journalpostId: String,
    ) {
        val søknadsskjema = SøknadsskjemaMapper.tilDomene(søknad)
        søknadBarnetilsynRepository.insert(søknadsskjema)
        søknadRepository.insert(SøknadMapper.toDomain(journalpostId, søknadsskjema, behandlingId))
    }

    @Transactional
    fun lagreSøknadForSkolepenger(
        søknad: SøknadSkolepenger,
        behandlingId: UUID,
        fagsakId: UUID,
        journalpostId: String,
    ) {
        val søknadsskjema = SøknadsskjemaMapper.tilDomene(søknad)
        søknadSkolepengerRepository.insert(søknadsskjema)
        søknadRepository.insert(SøknadMapper.toDomain(journalpostId, søknadsskjema, behandlingId))
    }

    private fun hentSøknad(behandlingId: UUID): Søknad? = søknadRepository.findByBehandlingId(behandlingId)
}
