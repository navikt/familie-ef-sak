package no.nav.familie.ef.sak.no.nav.familie.ef.sak.næringsinntektskontroll

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.næringsinntektskontroll.NæringsinntektKontrollDomain
import no.nav.familie.ef.sak.næringsinntektskontroll.NæringsinntektKontrollRepository
import no.nav.familie.ef.sak.næringsinntektskontroll.NæringsinntektKontrollUtfall
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

internal class NæringsinntektKontrollRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var næringsinntektKontrollRepository: NæringsinntektKontrollRepository

    @Test
    internal fun testInsert() {
        næringsinntektKontrollRepository.insert(NæringsinntektKontrollDomain(oppgaveId = 1, fagsakId = UUID.randomUUID(), kjøretidspunkt = LocalDateTime.now(), utfall = NæringsinntektKontrollUtfall.UENDRET_INNTEKT))
    }
}
