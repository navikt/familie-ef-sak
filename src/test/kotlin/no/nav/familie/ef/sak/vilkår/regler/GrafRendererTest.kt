package no.nav.familie.ef.sak.vilkår.regler

import io.mockk.mockk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Sivilstand
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaOvergangsstønad
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.regler.vilkår.SivilstandRegel
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

@Disabled
internal class GrafRendererTest {
    private val jsonMapper =
        no.nav.familie.kontrakter.felles.jsonMapper
            .writerWithDefaultPrettyPrinter()

    @Test
    internal fun `print alle vilkår`() {
        val vilkårsregler =
            Vilkårsregler.ALLE_VILKÅRSREGLER.vilkårsregler.filter { it.key != VilkårType.SIVILSTAND }.map {
                val regler = it.value.regler
                mapOf(
                    "name" to it.key,
                    "children" to it.value.hovedregler.map { regelId -> mapSpørsmål(regler, regelId) },
                )
            }
        println(
            jsonMapper.writeValueAsString(
                mapOf(
                    "name" to "vilkår",
                    "children" to vilkårsregler.toList(),
                ),
            ),
        )
    }

    enum class SivilstandData(
        val sivilstandstype: Sivilstandstype,
        val søknad: SøknadsskjemaOvergangsstønad = søknadBuilder(),
    ) {
        UGIFT__UFORMELT_GIFT__ELLER__UFORMELT_SKILT(
            Sivilstandstype.UGIFT,
            søknadBuilder { it.copy(erUformeltGift = true) },
        ),
        UGIFT(Sivilstandstype.UGIFT),
        GIFT__SØKT_OM_SKILSMISSE(
            Sivilstandstype.GIFT,
            søknadBuilder { it.copy(søktOmSkilsmisseSeparasjon = true) },
        ),
        GIFT(Sivilstandstype.GIFT),
        SEPARERT(Sivilstandstype.SEPARERT),
        SKILT(Sivilstandstype.SKILT),
        ENKE(Sivilstandstype.ENKE_ELLER_ENKEMANN),
    }

    @Test
    internal fun `print sivilstand`() {
        val regel = SivilstandRegel()
        val sivilstandregler =
            SivilstandData.values().map {
                val initereDelvilkårsvurdering =
                    regel.initiereDelvilkårsvurdering(
                        HovedregelMetadata(
                            it.søknad.sivilstand,
                            it.sivilstandstype,
                            barn = emptyList(),
                            søktOmBarnetilsyn = emptyList(),
                            vilkårgrunnlagDto = mockk(),
                            behandling = mockk(),
                        ),
                    )
                val hovedregler =
                    initereDelvilkårsvurdering
                        .filter { delvilkårsvurdering ->
                            delvilkårsvurdering.resultat != Vilkårsresultat.IKKE_AKTUELL
                        }.map { delvilkår -> mapSpørsmål(regel.regler, delvilkår.hovedregel) }

                mapOf(
                    "name" to it.name,
                    "children" to hovedregler,
                )
            }
        println(
            jsonMapper.writeValueAsString(
                mapOf(
                    "name" to "vilkår",
                    "children" to sivilstandregler.toList(),
                ),
            ),
        )
    }

    /**
     * Brukes kun til å rendere grafdata for d3
     */
    data class Spørsmål(
        val name: RegelId,
        val children: List<Svar>,
    ) {
        val type = "spørsmål"
    }

    data class Svar(
        val name: SvarId,
        val begrunnelseType: BegrunnelseType,
        val children: List<Spørsmål>,
        val resultat: Vilkårsresultat? = null,
    ) {
        val type = "svar"
    }

    private fun mapSvar(
        regler: Map<RegelId, RegelSteg>,
        svarMapping: Map<SvarId, SvarRegel>,
    ): List<Svar> =
        svarMapping.map {
            try {
                val value = it.value
                if (value is SluttSvarRegel) {
                    Svar(it.key, value.begrunnelseType, emptyList(), value.resultat.vilkårsresultat)
                } else {
                    Svar(it.key, value.begrunnelseType, listOf(mapSpørsmål(regler, value.regelId)))
                }
            } catch (e: Exception) {
                throw e
            }
        }

    private fun mapSpørsmål(
        regler: Map<RegelId, RegelSteg>,
        regelId: RegelId,
    ): Spørsmål {
        val svarMapping = regler[regelId]!!.svarMapping
        return Spørsmål(regelId, mapSvar(regler, svarMapping))
    }

    companion object {
        fun søknadBuilder(changeSivilstand: (Sivilstand) -> Sivilstand = { it }): SøknadsskjemaOvergangsstønad {
            val builder = TestsøknadBuilder.Builder()
            builder.setSivilstandsdetaljer(
                erUformeltGift = false,
                erUformeltSeparertEllerSkilt = false,
                søktOmSkilsmisseSeparasjon = false,
            )
            val søknad = SøknadsskjemaMapper.tilDomene(builder.build().søknadOvergangsstønad)
            return søknad.copy(sivilstand = changeSivilstand.invoke(søknad.sivilstand))
        }
    }
}
