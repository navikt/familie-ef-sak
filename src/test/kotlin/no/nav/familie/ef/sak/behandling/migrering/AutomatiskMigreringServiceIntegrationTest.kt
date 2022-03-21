package no.nav.familie.ef.sak.behandling.migrering

import io.mockk.every
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.migrering.AutomatiskMigreringService
import no.nav.familie.ef.sak.behandling.migrering.MigreringExceptionType
import no.nav.familie.ef.sak.behandling.migrering.MigreringResultat
import no.nav.familie.ef.sak.behandling.migrering.Migreringsstatus
import no.nav.familie.ef.sak.behandling.migrering.MigreringsstatusRepository
import no.nav.familie.ef.sak.fagsak.FagsakPersonRepository
import no.nav.familie.ef.sak.infotrygd.InfotrygdPeriodeTestUtil.lagInfotrygdPeriode
import no.nav.familie.ef.sak.infotrygd.InfotrygdReplikaClient
import no.nav.familie.ef.sak.infrastruktur.config.InfotrygdReplikaMock
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdPeriodeResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

internal class AutomatiskMigreringServiceIntegrationTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var iverksettClient: IverksettClient
    @Autowired private lateinit var infotrygdReplikaClient: InfotrygdReplikaClient
    @Autowired private lateinit var migreringsstatusRepository: MigreringsstatusRepository
    @Autowired private lateinit var automatiskMigreringService: AutomatiskMigreringService
    @Autowired private lateinit var fagsakPersonRepository: FagsakPersonRepository

    private val periode1dato = LocalDate.now().plusYears(1)
    private val periode2dato = LocalDate.now().plusYears(2)

    @AfterEach
    internal fun tearDown() {
        InfotrygdReplikaMock.resetMock(infotrygdReplikaClient)
    }

    @Test
    internal fun `ruller tilbake opprettelse av fagsakperson men oppdaterer migreringsstatus ved feil`() {
        val personIdent = "1"
        migreringsstatusRepository.insert(Migreringsstatus(personIdent, MigreringResultat.IKKE_KONTROLLERT))
        val perioderOvergangsstønad = listOf(
                lagInfotrygdPeriode(vedtakId = 1, stønadFom = periode1dato, stønadTom = periode1dato.plusDays(1)),
                lagInfotrygdPeriode(vedtakId = 2, stønadFom = periode2dato, stønadTom = periode2dato.plusDays(1)))
        every { infotrygdReplikaClient.hentPerioder(any()) } returns
                InfotrygdPeriodeResponse(perioderOvergangsstønad, emptyList(), emptyList())

        automatiskMigreringService.migrerPersonAutomatisk(personIdent)

        assertThat(fagsakPersonRepository.findByIdent(setOf(personIdent))).isNull()
        val oppdatertMigreringsstatus = migreringsstatusRepository.findByIdOrThrow(personIdent)
        assertThat(oppdatertMigreringsstatus.status).isEqualTo(MigreringResultat.FEILET)
        assertThat(oppdatertMigreringsstatus.årsak).isEqualTo(MigreringExceptionType.FLERE_AKTIVE_PERIODER)
    }

}
