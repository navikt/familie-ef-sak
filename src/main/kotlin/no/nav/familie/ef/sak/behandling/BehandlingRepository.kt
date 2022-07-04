package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.EksternId
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BehandlingRepository : RepositoryInterface<Behandling, UUID>, InsertUpdateRepository<Behandling> {

    fun findByFagsakId(fagsakId: UUID): List<Behandling>

    fun findByFagsakIdAndStatus(fagsakId: UUID, status: BehandlingStatus): List<Behandling>

    // language=PostgreSQL
    @Query(
        """SELECT b.*, be.id AS eksternid_id         
                     FROM behandling b         
                     JOIN behandling_ekstern be 
                     ON be.behandling_id = b.id         
                     WHERE be.id = :eksternId"""
    )
    fun finnMedEksternId(eksternId: Long): Behandling?

    // language=PostgreSQL
    @Query(
        """SELECT pi.ident FROM fagsak f
                    JOIN behandling b ON f.id = b.fagsak_id
                    JOIN person_ident pi ON f.fagsak_person_id=pi.fagsak_person_id
                    WHERE b.id = :behandlingId
                    ORDER BY pi.endret_tid DESC 
                    LIMIT 1
                    """
    )
    fun finnAktivIdent(behandlingId: UUID): String

    // language=PostgreSQL
    @Query(
        """SELECT b.id AS first, pi.ident AS second FROM fagsak f
                    JOIN behandling b ON f.id = b.fagsak_id
                    JOIN person_ident pi ON f.fagsak_person_id=pi.fagsak_person_id
                    WHERE b.id in (:behandlingIds)
            """
    )
    fun finnAktiveIdenter(behandlingIds: Collection<UUID>): List<Pair<UUID, String>>

    // language=PostgreSQL
    @Query(
        """SELECT
              b.id,
              b.forrige_behandling_id,
              be.id AS ekstern_id,
              b.type,
              b.status,
              b.steg,
              b.arsak,
              b.krav_mottatt,
              b.resultat,
              b.henlagt_arsak,
              b.opprettet_av,
              b.opprettet_tid,
              b.endret_tid,
              pi.ident,
              b.fagsak_id,
              fe.id AS ekstern_fagsak_id,
              f.stonadstype,
              f.migrert
             FROM fagsak f
             JOIN behandling b ON f.id = b.fagsak_id
             JOIN person_ident pi ON f.fagsak_person_id=pi.fagsak_person_id
             JOIN behandling_ekstern be ON be.behandling_id = b.id         
             JOIN fagsak_ekstern fe ON f.id = fe.fagsak_id         
             WHERE b.id = :behandlingId
             ORDER BY pi.endret_tid DESC
             LIMIT 1
             """
    )
    fun finnSaksbehandling(behandlingId: UUID): Saksbehandling

    // language=PostgreSQL
    @Query(
        """SELECT
              b.id,
              b.forrige_behandling_id,
              be.id AS ekstern_id,
              b.type,
              b.status,
              b.steg,
              b.arsak,
              b.krav_mottatt,
              b.resultat,
              b.henlagt_arsak,
              b.opprettet_av,
              b.opprettet_tid,
              b.endret_tid,
              pi.ident,
              b.fagsak_id,
              fe.id AS ekstern_fagsak_id,
              f.stonadstype,
              f.migrert
             FROM fagsak f
             JOIN behandling b ON f.id = b.fagsak_id
             JOIN person_ident pi ON f.fagsak_person_id=pi.fagsak_person_id
             JOIN behandling_ekstern be ON be.behandling_id = b.id         
             JOIN fagsak_ekstern fe ON f.id = fe.fagsak_id         
             WHERE be.id = :eksternBehandlingId
             ORDER BY pi.endret_tid DESC
             LIMIT 1
             """
    )
    fun finnSaksbehandling(eksternBehandlingId: Long): Saksbehandling

    // language=PostgreSQL
    @Query(
        """
        SELECT b.*, be.id AS eksternid_id
        FROM behandling b
        JOIN behandling_ekstern be ON b.id = be.behandling_id
        JOIN fagsak f ON f.id = b.fagsak_id
        JOIN person_ident pi ON f.fagsak_person_id = pi.fagsak_person_id
        WHERE pi.ident IN (:personidenter) AND f.stonadstype = :stønadstype
        ORDER BY b.opprettet_tid DESC
        LIMIT 1
    """
    )
    fun finnSisteBehandling(
        stønadstype: StønadType,
        personidenter: Set<String>
    ): Behandling?

    // language=PostgreSQL
    @Query(
        """
        SELECT b.*, be.id AS eksternid_id
        FROM behandling b
        JOIN behandling_ekstern be ON b.id = be.behandling_id
        WHERE b.fagsak_id = :fagsakId
         AND b.resultat IN ('OPPHØRT', 'INNVILGET')
         AND b.status = 'FERDIGSTILT'
        ORDER BY b.opprettet_tid DESC
        LIMIT 1
    """
    )
    fun finnSisteIverksatteBehandling(fagsakId: UUID): Behandling?

    fun existsByFagsakIdAndStatusIsNot(fagsakId: UUID, behandlingStatus: BehandlingStatus): Boolean

    // language=PostgreSQL
    @Query(
        """
        SELECT b.id behandling_id, be.id ekstern_behandling_id, fe.id ekstern_fagsak_id
        FROM behandling b
            JOIN behandling_ekstern be ON b.id = be.behandling_id
            JOIN fagsak_ekstern fe ON b.fagsak_id = fe.fagsak_id 
        WHERE behandling_id IN (:behandlingId)
        """
    )
    fun finnEksterneIder(behandlingId: Set<UUID>): Set<EksternId>

    // language=PostgreSQL
    @Query("""SELECT id FROM gjeldende_iverksatte_behandlinger WHERE stonadstype=:stønadstype""")
    fun finnSisteIverksatteBehandlinger(stønadstype: StønadType): Set<UUID>

    // language=PostgreSQL
    @Query(
        """
        SELECT DISTINCT pi.ident 
        FROM gjeldende_iverksatte_behandlinger gib 
            JOIN behandling b ON b.id = gib.id
            JOIN fagsak f ON f.id = b.fagsak_id
            JOIN person_ident pi ON f.fagsak_person_id=pi.fagsak_person_id
        WHERE gib.stonadstype=:stønadstype
    """
    )
    fun finnPersonerMedAktivStonad(stønadstype: StønadType = StønadType.OVERGANGSSTØNAD): List<String>

    // language=PostgreSQL
    @Query(
        """
        SELECT pi.ident AS first, gib.id AS second 
        FROM gjeldende_iverksatte_behandlinger gib 
            JOIN behandling b ON b.id = gib.id
            JOIN fagsak f ON f.id = b.fagsak_id
            JOIN person_ident pi ON f.fagsak_person_id=pi.fagsak_person_id
        WHERE pi.ident IN (:personidenter)
            AND gib.stonadstype=:stønadstype
    """
    )
    fun finnSisteIverksatteBehandlingerForPersonIdenter(
        personidenter: Collection<String>,
        stønadstype: StønadType = StønadType.OVERGANGSSTØNAD
    ): List<Pair<String, UUID>>

    fun existsByFagsakIdAndTypeIn(fagsakId: UUID, typer: Set<BehandlingType>): Boolean
}
