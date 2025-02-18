package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.domain.ÅrsakRevurdering
import no.nav.familie.ef.sak.behandling.revurdering.ÅrsakRevurderingsRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.ef.felles.Opplysningskilde
import no.nav.familie.kontrakter.ef.felles.Revurderingsårsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class ÅrsakRevurderingsRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var repository: ÅrsakRevurderingsRepository

    @Test
    internal fun `skal kunne lagre og hente årsakRevurdering`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val årsakRevurdering =
            ÅrsakRevurdering(
                behandling.id,
                Opplysningskilde.MELDING_MODIA,
                Revurderingsårsak.ENDRING_AKTIVITET,
                "beskrivelse",
            )
        repository.insert(årsakRevurdering)

        val hentetÅrsakRevurdering = repository.findByIdOrThrow(behandling.id)
        assertThat(hentetÅrsakRevurdering.behandlingId).isEqualTo(årsakRevurdering.behandlingId)
        assertThat(hentetÅrsakRevurdering.opplysningskilde).isEqualTo(årsakRevurdering.opplysningskilde)
        assertThat(hentetÅrsakRevurdering.årsak).isEqualTo(årsakRevurdering.årsak)
        assertThat(hentetÅrsakRevurdering.beskrivelse).isEqualTo(årsakRevurdering.beskrivelse)
    }
}
