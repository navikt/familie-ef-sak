package no.nav.familie.ef.sak.repository.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("vedlegg")
data class Vedlegg(@Id
                   val id: UUID,
                   @Column("gr_soknad_id")
                   val søknadId: UUID,
                   @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                   val sporbar: Sporbar = Sporbar(),
                   val data: ByteArray,
                   val navn: String) {


    override fun toString(): String {
        return "Vedlegg(id=$id, søknadId=$søknadId, sporbar=$sporbar, navn='$navn')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vedlegg

        if (id != other.id) return false
        if (søknadId != other.søknadId) return false
        if (sporbar != other.sporbar) return false
        if (!data.contentEquals(other.data)) return false
        if (navn != other.navn) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + søknadId.hashCode()
        result = 31 * result + sporbar.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + navn.hashCode()
        return result
    }
}

object VedleggMapper {

    fun toDomain(søknadId: UUID, vedlegg: no.nav.familie.kontrakter.ef.søknad.Vedlegg, bytes: ByteArray): Vedlegg =
            Vedlegg(id = UUID.fromString(vedlegg.id), søknadId = søknadId, data = bytes, navn = vedlegg.navn)
}
