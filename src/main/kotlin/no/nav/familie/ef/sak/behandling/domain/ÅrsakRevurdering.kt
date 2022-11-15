package no.nav.familie.ef.sak.behandling.domain

import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("arsak_revurdering")
data class ÅrsakRevurdering(
    @Id
    val behandlingId: UUID,
    val opplysningskilde: Opplysningskilde,
    @Column("arsak")
    val årsak: Revurderingsårsak,
    val beskrivelse: String?,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar()
)

enum class Opplysningskilde {
    MELDING_MODIA,
    INNSENDT_DOKUMENTASJON,
    BESKJED_ANNEN_ENHET,
    LIVSHENDELSER,
    OPPLYSNINGER_INTERNE_KONTROLLER
}

@Suppress("EnumEntryName", "unused")
enum class Revurderingsårsak(
    vararg stønadstyper: StønadType = arrayOf(
        StønadType.OVERGANGSSTØNAD,
        StønadType.BARNETILSYN,
        StønadType.SKOLEPENGER
    )
) {

    ENDRING_INNTEKT(StønadType.OVERGANGSSTØNAD),
    ENDRING_AKTIVITET(StønadType.OVERGANGSSTØNAD, StønadType.BARNETILSYN),
    ENDRING_INNTEKT_OG_AKTIVITET(StønadType.OVERGANGSSTØNAD),

    SØKNAD_UTVIDELSE_UTDANNING(StønadType.OVERGANGSSTØNAD),
    SØKNAD_UTVIDELSE_SÆRLIG_TILSYNSKREVENDE_BARN(StønadType.OVERGANGSSTØNAD),
    SØKNAD_FORLENGELSE_FORBIGÅENDE_SYKDOM(StønadType.OVERGANGSSTØNAD),
    SØKNAD_FORLENGELSE_PÅVENTE_AKTIVITET(StønadType.OVERGANGSSTØNAD),
    SØKNAD_NY_PERIODE_NYTT_BARN(StønadType.OVERGANGSSTØNAD),
    SØKNAD_NYTT_BGH_SKOLEÅR(StønadType.BARNETILSYN),
    SØKNAD_NYTT_SKOLEÅR(StønadType.SKOLEPENGER),

    OPPHØR_VILKÅR_IKKE_OPPFYLT,
    OPPHØR_EGET_ØNSKE,

    ENDRING_STØNADSPERIODE(StønadType.OVERGANGSSTØNAD),
    SØKNAD_NY_PERIODE_HOVEDPERIODE_IKKE_BRUKT_OPP_TIDLIGERE(StønadType.OVERGANGSSTØNAD),
    SØKNAD_BRUKT_OPP_HOVEDPERIODEN_TIDLIGERE(StønadType.OVERGANGSSTØNAD),
    SØKNAD_ETTER_AVSLAG,
    SØKNAD_ETTER_OPPHØR,

    ENDRING_TILSYNSUTGIFTER(StønadType.BARNETILSYN),
    ENDRING_ANTALL_BARN(StønadType.BARNETILSYN),
    ENDRING_UTGIFTER_SKOLEPENGER(StønadType.SKOLEPENGER),

    SANKSJON_1_MÅNED,
    UTESTENGELSE,
    ANNET,
    KLAGE_OMGJØRING,
    ANKE_OMGJØRING;

    val gjelderStønadstyper = stønadstyper.toSet()

    fun erGyldigForStønadstype(stønadType: StønadType): Boolean {
        return gjelderStønadstyper.contains(stønadType)
    }
}
