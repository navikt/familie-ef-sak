package no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.repository.SøknadBarnetilsynRepository
import no.nav.familie.ef.sak.repository.SøknadOvergangsstønadRepository
import no.nav.familie.ef.sak.repository.SøknadSkolepengerRepository
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.ef.søknad.Søknadsfelt
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class SøknadsskjemaOvergangsstønadRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var søknadOvergangsstønadRepository: SøknadOvergangsstønadRepository

    @Autowired
    private lateinit var søknadBarnetilsynRepository: SøknadBarnetilsynRepository

    @Autowired
    private lateinit var søknadSkolepengerRepository: SøknadSkolepengerRepository

    @Test
    internal fun `søknad om overgangsstønad lagres korrekt`() {

        val søknadTilLagring = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad)

        søknadOvergangsstønadRepository.insert(søknadTilLagring)
        val søknadFraDatabase = søknadOvergangsstønadRepository.findByIdOrThrow(søknadTilLagring.id)

        // Jdbc returnerer tomt objekt for barnepass selv om Embedded.OnEmpty.USE_NULL er satt
        assertThat(søknadFraDatabase).isEqualToIgnoringGivenFields(søknadTilLagring, "barn")
    }

    @Test
    internal fun `søknad om overgangsstønad uten utdanning lagres korrekt`() {

        val aktivitet = Testsøknad.søknadOvergangsstønad.aktivitet
        val barn = Testsøknad.søknadOvergangsstønad.barn
        val barnUtenBarnepass = barn.verdi.map { it.copy(barnepass = null) }
        val aktivitetUtenUtdanning = aktivitet.verdi.copy(underUtdanning = null)
        val søknadTilLagring = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad.copy(
                aktivitet = aktivitet.copy(verdi = aktivitetUtenUtdanning),
                barn = Søknadsfelt("", barnUtenBarnepass)))

        søknadOvergangsstønadRepository.insert(søknadTilLagring)
        val søknadFraDatabase = søknadOvergangsstønadRepository.findByIdOrThrow(søknadTilLagring.id)

        assertThat(søknadFraDatabase.aktivitet.underUtdanning).isNull()
        assertThat(søknadFraDatabase.aktivitet.tidligereUtdanninger).isEmpty()
        assertThat(søknadFraDatabase.barn.first().barnepass!!.årsakBarnepass).isNull()
        assertThat(søknadFraDatabase.barn.first().barnepass!!.barnepassordninger).isEmpty()

    }

    @Test
    internal fun `søknad om skolepenger lagres korrekt`() {
        val søknadTilLagring = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadSkolepenger)

        søknadSkolepengerRepository.insert(søknadTilLagring)
        val søknadFraDatabase = søknadSkolepengerRepository.findByIdOrThrow(søknadTilLagring.id)

        // Jdbc returnerer tomt objekt for barnepass selv om Embedded.OnEmpty.USE_NULL er satt
        assertThat(søknadFraDatabase).isEqualToIgnoringGivenFields(søknadTilLagring, "barn")
    }

    @Test
    internal fun `søknad om barnetilsyn lagres korrekt`() {
        val søknadTilLagring = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadBarnetilsyn)

        søknadBarnetilsynRepository.insert(søknadTilLagring)
        val søknadFraDatabase = søknadBarnetilsynRepository.findByIdOrThrow(søknadTilLagring.id)

        // Jdbc returnerer tomt objekt for barnepass selv om Embedded.OnEmpty.USE_NULL er satt
        assertThat(søknadFraDatabase).isEqualToIgnoringGivenFields(søknadTilLagring, "barn")
    }
}