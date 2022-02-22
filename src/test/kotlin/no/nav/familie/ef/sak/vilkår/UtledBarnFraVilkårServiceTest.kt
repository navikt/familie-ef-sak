package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.barn.BehandlingBarn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

internal class UtledBarnFraVilkårServiceTest {

    @Test
    internal fun `skal finne ny barnId for barn for barn fra søknaden`() {
        val søknadBarnId = UUID.randomUUID()
        val tidligereBarn = opprettBarn(søknadBarnId = søknadBarnId)
        val nyttBarn = opprettBarn(søknadBarnId = søknadBarnId)
        val alleBarnPåForrigeBehandling = listOf(tidligereBarn)
        val alleBarnPåGjeldendeBehandling = listOf(nyttBarn)
        val utledetBarnIdMap = VurderingService.byggBarnMapFraTidligereTilNyId(alleBarnPåForrigeBehandling,
                                                                               alleBarnPåGjeldendeBehandling)

        assertThat(utledetBarnIdMap[tidligereBarn.id]?.id).isEqualTo(nyttBarn.id)
    }

    @Test
    internal fun `skal finne ny barnId for barn for barn fra søknaden og ikke for nytt barn`() {
        val søknadBarnId = UUID.randomUUID()
        val tidligereBarn = opprettBarn(søknadBarnId = søknadBarnId)
        val nyttBarnA = opprettBarn(søknadBarnId = søknadBarnId)
        val nyttBarnB = opprettBarn(søknadBarnId = søknadBarnId)
        val alleBarnPåForrigeBehandling = listOf(tidligereBarn)
        val alleBarnPåGjeldendeBehandling = listOf(nyttBarnA, nyttBarnB)
        val utledetBarnIdMap = VurderingService.byggBarnMapFraTidligereTilNyId(alleBarnPåForrigeBehandling,
                                                                               alleBarnPåGjeldendeBehandling)

        assertThat(utledetBarnIdMap[tidligereBarn.id]?.id).isEqualTo(nyttBarnA.id)
        assertThat(utledetBarnIdMap).hasSize(1)
    }

    @Test
    internal fun `skal finne ny barnId for barn for barn basert på personIdent`() {
        val personIdentA = "1234123412"
        val personIdentB = "99988877721"
        val tidligereBarnA = opprettBarn(personIdent = personIdentA)
        val tidligereBarnB = opprettBarn(personIdent = personIdentB)
        val nyttBarnA = opprettBarn(personIdent = personIdentA)
        val nyttBarnB = opprettBarn(personIdent = personIdentB)
        val alleBarnPåForrigeBehandling = listOf(tidligereBarnA, tidligereBarnB)
        val alleBarnPåGjeldendeBehandling = listOf(nyttBarnB, nyttBarnA)
        val utledetBarnIdMap = VurderingService.byggBarnMapFraTidligereTilNyId(alleBarnPåForrigeBehandling,
                                                                               alleBarnPåGjeldendeBehandling)

        assertThat(utledetBarnIdMap[tidligereBarnA.id]?.id).isEqualTo(nyttBarnA.id)
        assertThat(utledetBarnIdMap[tidligereBarnB.id]?.id).isEqualTo(nyttBarnB.id)
    }


    @Test
    internal fun `skal finne ny barnId for barn for barn som ikke er på søknad og uten personIdent`() {
        val fødselTermindatoA = LocalDate.now().plusWeeks(2)
        val fødselTermindatoB = LocalDate.now().plusWeeks(2)
        val tidligereBarnA = opprettBarn(fødselTermindato = fødselTermindatoA)
        val tidligereBarnB = opprettBarn(fødselTermindato = fødselTermindatoB)
        val nyttBarnA = opprettBarn(fødselTermindato = fødselTermindatoA)
        val nyttBarnB = opprettBarn(fødselTermindato = fødselTermindatoB)
        val alleBarnPåForrigeBehandling = listOf(tidligereBarnA, tidligereBarnB)
        val alleBarnPåGjeldendeBehandling = listOf(nyttBarnB, nyttBarnA)
        val utledetBarnIdMap = VurderingService.byggBarnMapFraTidligereTilNyId(alleBarnPåForrigeBehandling,
                                                                               alleBarnPåGjeldendeBehandling)

        assertThat(utledetBarnIdMap[tidligereBarnA.id]?.id).isNotNull
        assertThat(utledetBarnIdMap[tidligereBarnB.id]?.id).isNotNull
        assertThat(utledetBarnIdMap[tidligereBarnA.id]?.id).isNotEqualTo(utledetBarnIdMap[tidligereBarnB.id]?.id)
    }

    private fun opprettBarn(søknadBarnId: UUID? = null,
                            navn: String? = null,
                            fødselTermindato: LocalDate? = null,
                            personIdent: String? = null): BehandlingBarn = BehandlingBarn(behandlingId = UUID.randomUUID(),
                                                                                          søknadBarnId = søknadBarnId,
                                                                                          navn = navn,
                                                                                          personIdent = personIdent,
                                                                                          fødselTermindato = fødselTermindato)
}