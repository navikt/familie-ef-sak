package no.nav.familie.ef.sak.behandlingshistorikk.domain

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingshistorikk.dto.BehandlingshistorikkDto
import no.nav.familie.ef.sak.behandlingshistorikk.dto.Hendelse
import no.nav.familie.ef.sak.behandlingshistorikk.dto.HendelseshistorikkDto
import no.nav.familie.ef.sak.felles.domain.JsonWrapper
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

data class Behandlingshistorikk(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val steg: StegType,
    val utfall: StegUtfall? = null,
    val metadata: JsonWrapper? = null,
    val opprettetAvNavn: String = SikkerhetContext.hentSaksbehandlerNavn(),
    val opprettetAv: String = SikkerhetContext.hentSaksbehandler(),
    val endretTid: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS)
)

fun Behandlingshistorikk.tilDto(): BehandlingshistorikkDto {
    return BehandlingshistorikkDto(
        behandlingId = this.behandlingId,
        steg = this.steg,
        endretAvNavn = this.opprettetAvNavn,
        endretAv = this.opprettetAv,
        endretTid = this.endretTid,
        utfall = this.utfall,
        metadata = this.metadata.tilJson()
    )
}

fun Behandlingshistorikk.tilHendelseshistorikkDto(): HendelseshistorikkDto {

    val hendelse: Hendelse = when (this.steg) {
        StegType.VILKÅR -> Hendelse.OPPRETTET
        StegType.SEND_TIL_BESLUTTER -> Hendelse.SENDT_TIL_BESLUTTER
        StegType.BEHANDLING_FERDIGSTILT -> Hendelse.VEDTAK_IVERKSATT
        StegType.BESLUTTE_VEDTAK -> when (this.utfall) {
            StegUtfall.BESLUTTE_VEDTAK_GODKJENT -> Hendelse.VEDTAK_GODKJENT
            StegUtfall.BESLUTTE_VEDTAK_UNDERKJENT -> Hendelse.VEDTAK_UNDERKJENT
            else -> Hendelse.HENLAGT
        }
        else -> Hendelse.UKJENT
    }

    return HendelseshistorikkDto(
        behandlingId = this.behandlingId,
        hendelse = hendelse,
        endretAvNavn = this.opprettetAvNavn,
        endretTid = this.endretTid,
        metadata = this.metadata.tilJson()
    )
}

fun JsonWrapper?.tilJson(): Map<String, Any>? {
    return this?.json?.let { objectMapper.readValue(it) }
}

enum class StegUtfall {
    BESLUTTE_VEDTAK_GODKJENT,
    BESLUTTE_VEDTAK_UNDERKJENT,
    HENLAGT
}