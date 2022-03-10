package no.nav.familie.ef.sak.behandling.migrering

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.relational.core.mapping.Column
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface MigreringsstatusRepository : RepositoryInterface<Migreringsstatus, UUID>, InsertUpdateRepository<Migreringsstatus> {

    fun findAllByIdentIn(identer: Set<String>): Set<Migreringsstatus>
}

enum class MigreringResultat {
    OK,
    FEILET
}

data class Migreringsstatus(val ident: String,
                            val status: MigreringResultat,
                            @Column("arsak")
                            val årsak: MigreringExceptionType? = null)