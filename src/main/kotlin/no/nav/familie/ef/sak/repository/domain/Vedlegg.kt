package no.nav.familie.ef.sak.repository.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.*

@Suppress("ArrayInDataClass")
@Table("vedlegg")
data class Vedlegg(@Id
                   val id: UUID,
                   @Column("sak_id")
                   val sakId: UUID,
                   @Column("opprettet_tid")
                   val opprettetTid: LocalDateTime = LocalDateTime.now(),
        //TODO spårbar
                   val data: ByteArray,
                   val navn: String)

object VedleggMapper {
    fun toDomain(sakId: UUID, vedlegg: no.nav.familie.kontrakter.ef.søknad.Vedlegg): Vedlegg =
            Vedlegg(id = UUID.fromString(vedlegg.id), sakId = sakId, data = vedlegg.bytes, navn = vedlegg.navn)
}
