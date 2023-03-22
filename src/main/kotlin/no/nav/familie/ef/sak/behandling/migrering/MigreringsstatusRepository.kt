package no.nav.familie.ef.sak.behandling.migrering

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import org.springframework.stereotype.Repository

@Repository
interface MigreringsstatusRepository : RepositoryInterface<Migreringsstatus, String>, InsertUpdateRepository<Migreringsstatus> {

    fun findAllByIdentIn(identer: Set<String>): Set<Migreringsstatus>

    fun findAllByÅrsak(årsak: MigreringExceptionType): Set<Migreringsstatus>
}

enum class MigreringResultat {
    OK,
    FEILET,
    IKKE_KONTROLLERT,
}

@Table("migrering")
data class Migreringsstatus(
    @Id
    val ident: String,
    val status: MigreringResultat,
    @Column("arsak")
    val årsak: MigreringExceptionType? = null,
)
