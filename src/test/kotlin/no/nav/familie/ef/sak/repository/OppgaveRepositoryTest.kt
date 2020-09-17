package no.nav.familie.ef.sak.repository

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("local", "mock-oauth")
internal class OppgaveRepositoryTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var customRepository: CustomRepository
    @Autowired private lateinit var oppgaveRepository: OppgaveRepository

    @Test
    internal fun `oppretter oppgave`() {
        val fagsak = customRepository.persist(fagsak()) as Fagsak
        val behandling = customRepository.persist(behandling(fagsak)) as Behandling
        val oppgave = customRepository.persist(oppgave(behandling))
    }

    private fun oppgave(behandling: Behandling): Oppgave {
        return Oppgave(
                behandlingId = behandling.id!!,
                gsakId = "",
                type = Oppgavetype.Journalføring,
                erFerdigstilt = false
        )
    }

    private fun behandling(fagsak: Fagsak): Behandling {
        return Behandling(
                fagsakId = fagsak.id!!,
                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                opprinnelse = BehandlingOpprinnelse.MANUELL,
                status = BehandlingStatus.OPPRETTET,
                steg = BehandlingSteg.KOMMER_SENDERE
        )
    }

    private fun fagsak() = Fagsak(stønadstype = Stønadstype.OVERGANGSSTØNAD)
}