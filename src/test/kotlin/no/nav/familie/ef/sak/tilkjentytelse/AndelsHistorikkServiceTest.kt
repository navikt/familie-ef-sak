package no.nav.familie.ef.sak.tilkjentytelse

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.vilkår.Vurdering
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.util.UUID

class AndelsHistorikkServiceTest {
    private val vilkårsvurderingRepository = mockk<VilkårsvurderingRepository>()

    val andelsHistorikkService = AndelsHistorikkService(mockk(), mockk(), mockk(), mockk(), vilkårsvurderingRepository, mockk(), mockk())

    private val behandlingId = UUID.randomUUID()

    @Test
    internal fun `Skal returnere aktivitet i historikk`() {
        val vilkårsvurderingList =
            VilkårType.hentVilkårForStønad(StønadType.BARNETILSYN).map {
                vilkårsvurdering(
                    behandlingId = behandlingId,
                    resultat = Vilkårsresultat.OPPFYLT,
                    type = VilkårType.AKTIVITET_ARBEID,
                    delvilkårsvurdering =
                        listOf(
                            Delvilkårsvurdering(
                                Vilkårsresultat.OPPFYLT,
                                listOf(Vurdering(RegelId.ER_I_ARBEID_ELLER_FORBIGÅENDE_SYKDOM, SvarId.ER_I_ARBEID)),
                            ),
                        ),
                )
            }

        every {
            vilkårsvurderingRepository.findByTypeAndBehandlingIdIn(
                VilkårType.AKTIVITET_ARBEID,
                listOf(behandlingId),
            )
        } returns vilkårsvurderingList

        val behandlingIdToSvarID = andelsHistorikkService.aktivitetArbeidForBehandlingIds(listOf(behandlingId))

        Assertions.assertThat(behandlingIdToSvarID[behandlingId]).isEqualTo(SvarId.ER_I_ARBEID)
    }
}
