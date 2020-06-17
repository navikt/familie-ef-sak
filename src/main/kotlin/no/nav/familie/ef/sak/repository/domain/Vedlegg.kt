package no.nav.familie.ef.sak.repository.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.*

@Table("vedlegg")
data class Vedlegg(@Id
                   val id: UUID,
                   @Column("sak_id")
                   val sakId: UUID,
                   @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                   val sporbar: Sporbar = Sporbar(),
                   val data: ByteArray,
                   val navn: String) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vedlegg

        if (id != other.id) return false
        if (sakId != other.sakId) return false
        if (sporbar != other.sporbar) return false
        if (!data.contentEquals(other.data)) return false
        if (navn != other.navn) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + sakId.hashCode()
        result = 31 * result + sporbar.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + navn.hashCode()
        return result
    }
}

object VedleggMapper {
    fun toDomain(sakId: UUID, vedlegg: no.nav.familie.kontrakter.ef.søknad.Vedlegg): Vedlegg =
            Vedlegg(id = UUID.fromString(vedlegg.id), sakId = sakId, data = vedlegg.bytes, navn = vedlegg.navn)
}
