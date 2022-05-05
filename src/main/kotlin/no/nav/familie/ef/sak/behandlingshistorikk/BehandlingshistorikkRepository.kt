package no.nav.familie.ef.sak.behandlingshistorikk

import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface BehandlingshistorikkRepository : RepositoryInterface<Behandlingshistorikk, UUID>,
                                           InsertUpdateRepository<Behandlingshistorikk> {

    fun findByBehandlingIdOrderByEndretTidDesc(behandlingId: UUID): List<Behandlingshistorikk>

    fun findByBehandlingIdOrderByEndretTidAsc(behandlingId: UUID): List<Behandlingshistorikk>

    fun findTopByBehandlingIdOrderByEndretTidDesc(behandlingId: UUID): Behandlingshistorikk

    fun findTopByBehandlingIdAndStegOrderByEndretTidDesc(behandlingId: UUID, steg: StegType): Behandlingshistorikk?

    // language=PostgreSQL
    @Query("""SELECT first, second FROM (
                SELECT bh.behandling_id AS first, bh.endret_tid AS second,
                    ROW_NUMBER() OVER (PARTITION BY bh.behandling_id ORDER BY bh.endret_tid DESC) AS rn
                FROM behandlingshistorikk bh
                WHERE bh.steg = :behandlingsteg) q
              WHERE rn = 1""")
    fun finnSisteEndringstidspunktForBehandlinger(behandlingsIder: List<UUID>, behandlingsteg: StegType): List<Pair<UUID, Timestamp>>

}