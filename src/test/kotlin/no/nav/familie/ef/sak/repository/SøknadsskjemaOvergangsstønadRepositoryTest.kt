package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadBarnetilsynRepository
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadOvergangsstønadRepository
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadSkolepengerRepository
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Sivilstandsplaner
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SøknadsskjemaMapper
import no.nav.familie.kontrakter.ef.søknad.Barnepass
import no.nav.familie.kontrakter.ef.søknad.Søknadsfelt
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
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
    internal fun `søknad om overgangsstønad lagres korrekt med tom sivilstandsplaner`() {
        val søknadTilLagring = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad.copy(sivilstandsplaner = null))
        søknadOvergangsstønadRepository.insert(søknadTilLagring)
        val søknadFraDatabase = søknadOvergangsstønadRepository.findByIdOrThrow(søknadTilLagring.id)
        assertThat(søknadFraDatabase.sivilstandsplaner).isEqualTo(Sivilstandsplaner(null, null, null))
    }


    @Test
    internal fun `søknad om overgangsstønad lagres korrekt`() {

        val søknadTilLagring = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad)

        søknadOvergangsstønadRepository.insert(søknadTilLagring)
        val søknadFraDatabase = søknadOvergangsstønadRepository.findByIdOrThrow(søknadTilLagring.id)

        assertThat(søknadFraDatabase).isEqualTo(søknadTilLagring)
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
        assertThat(søknadFraDatabase.barn.first().årsakBarnepass).isNull()
        assertThat(søknadFraDatabase.barn.first().barnepassordninger).isEmpty()

    }

    @Test
    internal fun `søknad om skolepenger lagres korrekt`() {
        val søknadTilLagring = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadSkolepenger)

        søknadSkolepengerRepository.insert(søknadTilLagring)
        val søknadFraDatabase = søknadSkolepengerRepository.findByIdOrThrow(søknadTilLagring.id)

        assertThat(søknadFraDatabase).isEqualTo(søknadTilLagring)
    }

    @Test
    internal fun `søknad om barnetilsyn lagres korrekt`() {
        val søknadTilLagring = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadBarnetilsyn)

        søknadBarnetilsynRepository.insert(søknadTilLagring)
        val søknadFraDatabase = søknadBarnetilsynRepository.findByIdOrThrow(søknadTilLagring.id)

        assertThat(søknadFraDatabase).isEqualTo(søknadTilLagring)
    }

    @Test
    internal fun `søknad uten barnepass blir hentet korrekt`() {
        val builder = TestsøknadBuilder.Builder()
        builder.setBarn(listOf(builder.defaultBarn(barnepass = Barnepass(barnepassordninger = Søknadsfelt("", emptyList())),
                                                   skalHaBarnepass = false)))
        val søknadTilLagring = SøknadsskjemaMapper.tilDomene(builder.build().søknadBarnetilsyn)

        søknadBarnetilsynRepository.insert(søknadTilLagring)
        val søknadFraDatabase = søknadBarnetilsynRepository.findByIdOrThrow(søknadTilLagring.id)

        assertThat(søknadFraDatabase).isEqualTo(søknadTilLagring)
        assertThat(søknadFraDatabase.barn.single().barnepassordninger).isEmpty()
    }

    @Test
    internal fun `søknad med barnepass blir hentet korrekt`() {
        val builder = TestsøknadBuilder.Builder()
        val barnepass = builder.defaultBarnepass(ordninger = listOf(builder.defaultBarnepassordning()))
        builder.setBarn(listOf(builder.defaultBarn(barnepass = barnepass, skalHaBarnepass = true)))
        val søknadTilLagring = SøknadsskjemaMapper.tilDomene(builder.build().søknadBarnetilsyn)

        søknadBarnetilsynRepository.insert(søknadTilLagring)
        val søknadFraDatabase = søknadBarnetilsynRepository.findByIdOrThrow(søknadTilLagring.id)

        assertThat(søknadFraDatabase.barn).isEqualTo(søknadTilLagring.barn)
        assertThat(søknadFraDatabase.barn.single().barnepassordninger).hasSize(1)
    }

}