package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.felles.domain.Endret
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

internal class RepositoryTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var fagsakRepository: FagsakRepository

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal oppdatere sporbar automatisk når en entitet oppdateres`() {
        val opprinneligEndret = Endret(endretAv = "~", endretTid = LocalDateTime.MIN)
        val fagsak = fagsakRepository.insert(fagsakDao())
        val opprettetSporbar = fagsakRepository.findByIdOrThrow(fagsak.id).sporbar
        val opprettetTid = fagsak.sporbar.opprettetTid
        val opprettetAv = "VL"

        // verifiser att opprettetAv og opprettetTid ikke er mulig å sette
        // kanskje ikke ønskelig men nå oppfattes alle våre entiteter som nye då vi har satt ID på de
        assertThat(opprettetSporbar.endret.endretAv).isNotEqualTo(opprinneligEndret.endretAv)
        assertThat(opprettetSporbar.endret.endretTid).isNotEqualTo(opprinneligEndret.endretTid)

        assertThat(opprettetSporbar.opprettetAv).isEqualTo(opprettetAv)
        assertThat(opprettetSporbar.opprettetTid).isEqualTo(opprettetTid)
        assertThat(opprettetSporbar.endret.endretAv).isEqualTo(opprettetAv)
        assertThat(opprettetSporbar.endret.endretTid).isEqualTo(fagsak.sporbar.endret.endretTid)

        val nyEndretAv = "En annen brukere"
        BrukerContextUtil.mockBrukerContext(nyEndretAv)
        val oppdatertFagsak = fagsakRepository.update(fagsak)
        val oppdatertSporbar = fagsakRepository.findByIdOrThrow(fagsak.id).sporbar

        assertThat(oppdatertSporbar.endret.endretTid).isNotEqualTo(opprettetSporbar.endret.endretTid)
        assertThat(oppdatertSporbar.opprettetAv).isEqualTo(opprettetAv)
        assertThat(oppdatertSporbar.opprettetTid).isEqualTo(opprettetTid)
        assertThat(oppdatertSporbar.endret.endretAv).isEqualTo(nyEndretAv)
        assertThat(oppdatertSporbar.endret.endretTid).isEqualTo(oppdatertFagsak.sporbar.endret.endretTid)
    }
}
