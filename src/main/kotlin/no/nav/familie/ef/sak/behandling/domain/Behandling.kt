package no.nav.familie.ef.sak.behandling.domain

import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak.BEHANDLES_I_GOSYS
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak.FEILREGISTRERT
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak.TRUKKET_TILBAKE
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import java.time.LocalDate
import java.util.UUID

/**
 * @param forrigeBehandlingId forrige iverksatte behandling
 */
data class Behandling(@Id
                      val id: UUID = UUID.randomUUID(),
                      val fagsakId: UUID,
                      val forrigeBehandlingId: UUID? = null,
                      @MappedCollection(idColumn = "behandling_id")
                      val eksternId: EksternBehandlingId = EksternBehandlingId(),
                      val versjon: Int = 0,

                      val type: BehandlingType,
                      val status: BehandlingStatus,
                      val steg: StegType,
                      @Column("arsak")
                      val årsak: BehandlingÅrsak,
                      val kravMottatt: LocalDate? = null,

                      @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                      val sporbar: Sporbar = Sporbar(),
                      val resultat: BehandlingResultat,
                      @Column("henlagt_arsak")
                      val henlagtÅrsak: HenlagtÅrsak? = null) {

    fun kanHenlegges(): Boolean = !status.behandlingErLåstForVidereRedigering()

    fun erMigrering(): Boolean = årsak == BehandlingÅrsak.MIGRERING

    init {
        if (resultat == BehandlingResultat.HENLAGT) {
            feilHvis(henlagtÅrsak == null) { "Kan ikke henlegge behandling uten en årsak" }
            when (henlagtÅrsak) {
                BEHANDLES_I_GOSYS -> feilHvis(this.type !== BehandlingType.BLANKETT) { "Bare blankett kan henlegges med årsak BEHANDLES_I_GOSYS" }
                FEILREGISTRERT -> feilHvis(this.type == BehandlingType.BLANKETT) { "Blankett kan bare henlegges med årsak BEHANDLES_I_GOSYS" }
                TRUKKET_TILBAKE -> feilHvis(this.type == BehandlingType.BLANKETT) { "Blankett kan bare henlegges med årsak BEHANDLES_I_GOSYS" }
            }
        }
    }
}

enum class BehandlingType(val visningsnavn: String) {
    FØRSTEGANGSBEHANDLING("Førstegangsbehandling"),
    BLANKETT("Blankett"),
    REVURDERING("Revurdering"),
    TEKNISK_OPPHØR("Teknisk opphør")
}

enum class BehandlingResultat(val displayName: String) {
    INNVILGET(displayName = "Innvilget"),
    OPPHØRT(displayName = "Opphørt"),
    AVSLÅTT(displayName = "Avslått"),
    IKKE_SATT(displayName = "Ikke satt"),
    HENLAGT(displayName = "Henlagt"),
}

enum class BehandlingStatus {
    OPPRETTET,
    UTREDES,
    FATTER_VEDTAK,
    IVERKSETTER_VEDTAK,
    FERDIGSTILT,
    SATT_PÅ_VENT,
    ;

    fun behandlingErLåstForVidereRedigering(): Boolean =
            setOf(FATTER_VEDTAK, IVERKSETTER_VEDTAK, FERDIGSTILT).contains(this)
}

