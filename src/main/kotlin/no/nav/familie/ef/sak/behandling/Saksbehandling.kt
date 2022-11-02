package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Saksbehandling(
    val id: UUID,
    val eksternId: Long,
    val forrigeBehandlingId: UUID? = null,
    val type: BehandlingType,
    val status: BehandlingStatus,
    val steg: StegType,
    @Column("arsak")
    val årsak: BehandlingÅrsak,
    val kravMottatt: LocalDate? = null,
    val resultat: BehandlingResultat,
    val vedtakstidspunkt: LocalDateTime?,
    @Column("henlagt_arsak")
    val henlagtÅrsak: HenlagtÅrsak? = null,
    val ident: String,
    val fagsakId: UUID,
    val eksternFagsakId: Long,
    @Column("stonadstype")
    val stønadstype: StønadType,
    val migrert: Boolean = false,
    val opprettetAv: String,
    val opprettetTid: LocalDateTime,
    val endretTid: LocalDateTime
) {

    val erMigrering get() = årsak == BehandlingÅrsak.MIGRERING

    val erOmregning get() = årsak == BehandlingÅrsak.G_OMREGNING

    val erMaskinellOmregning get() = erOmregning && opprettetAv == SYSTEM_FORKORTELSE
}
