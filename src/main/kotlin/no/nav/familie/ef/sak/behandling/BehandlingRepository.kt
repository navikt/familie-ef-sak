package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.dto.EksternId
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface BehandlingRepository :
    RepositoryInterface<Behandling, UUID>,
    InsertUpdateRepository<Behandling> {
    fun findByFagsakId(fagsakId: UUID): List<Behandling>

    fun findByFagsakIdAndStatus(
        fagsakId: UUID,
        status: BehandlingStatus,
    ): List<Behandling>

    fun existsByFagsakId(fagsakId: UUID): Boolean

    // language=PostgreSQL
    @Query(
        """SELECT b.*         
                     FROM behandling b         
                     WHERE b.ekstern_id = :eksternId""",
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
                    """,
    )
    fun finnAktivIdent(behandlingId: UUID): String

    // language=PostgreSQL
    @Query(
        """SELECT
              b.id,
              b.forrige_behandling_id,
              b.ekstern_id AS ekstern_id,
              b.type,
              b.status,
              b.steg,
              b.kategori,
              b.arsak,
              b.krav_mottatt,
              b.resultat,
              b.vedtakstidspunkt,
              b.henlagt_arsak,
              b.opprettet_av,
              b.opprettet_tid,
              b.endret_tid,
              pi.ident,
              b.fagsak_id,
              f.ekstern_id AS ekstern_fagsak_id,
              f.stonadstype,
              f.migrert
             FROM fagsak f
             JOIN behandling b ON f.id = b.fagsak_id
             JOIN person_ident pi ON f.fagsak_person_id=pi.fagsak_person_id
             WHERE b.id = :behandlingId
             ORDER BY pi.endret_tid DESC
             LIMIT 1
             """,
    )
    fun finnSaksbehandling(behandlingId: UUID): Saksbehandling

    // language=PostgreSQL
    @Query(
        """SELECT
              b.id,
              b.forrige_behandling_id,
              b.ekstern_id AS ekstern_id,
              b.type,
              b.status,
              b.steg,
              b.kategori,
              b.arsak,
              b.krav_mottatt,
              b.resultat,
              b.vedtakstidspunkt,
              b.henlagt_arsak,
              b.opprettet_av,
              b.opprettet_tid,
              b.endret_tid,
              pi.ident,
              b.fagsak_id,
              f.ekstern_id AS ekstern_fagsak_id,
              f.stonadstype,
              f.migrert
             FROM fagsak f
             JOIN behandling b ON f.id = b.fagsak_id
             JOIN person_ident pi ON f.fagsak_person_id=pi.fagsak_person_id
             WHERE b.ekstern_id = :eksternBehandlingId
             ORDER BY pi.endret_tid DESC
             LIMIT 1
             """,
    )
    fun finnSaksbehandling(eksternBehandlingId: Long): Saksbehandling

    // language=PostgreSQL
    @Query(
        """
        SELECT b.*
        FROM behandling b
        WHERE b.fagsak_id = :fagsakId
         AND b.resultat IN ('OPPHØRT', 'INNVILGET')
         AND b.status = 'FERDIGSTILT'
        ORDER BY b.vedtakstidspunkt DESC
        LIMIT 1
    """,
    )
    fun finnSisteIverksatteBehandling(fagsakId: UUID): Behandling?

    @Query(
        """
        SELECT b.*
        FROM behandling b
        JOIN fagsak f on b.fagsak_id = f.id 
        WHERE f.fagsak_person_id = :fagsakPersonId
         AND b.resultat IN ('OPPHØRT', 'INNVILGET', 'AVSLÅTT', 'IKKE_SATT')
         AND b.status NOT IN ('OPPRETTET')
         AND b.arsak != 'MIGRERING'
    """,
    )
    fun finnBehandlingerForGjenbrukAvVilkårOgSamværsavtaler(fagsakPersonId: UUID): List<Behandling>

    @Query(
        """
            SELECT b.id
            FROM fagsak f
                     JOIN behandling b ON f.id = b.fagsak_id
                     JOIN person_ident pi ON f.fagsak_person_id=pi.fagsak_person_id
            WHERE b.fagsak_id = :fagsakId
            ORDER BY b.vedtakstidspunkt desc
            LIMIT 1
    """,
    )
    fun finnSisteBehandlingForOppgaveKanOpprettes(fagsakId: UUID): UUID?

    fun existsByFagsakIdAndStatusIsNot(
        fagsakId: UUID,
        behandlingStatus: BehandlingStatus,
    ): Boolean

    fun existsByFagsakIdAndStatusIsNotIn(
        fagsakId: UUID,
        behandlingStatus: List<BehandlingStatus>,
    ): Boolean

    // language=PostgreSQL
    @Query(
        """
        SELECT b.id AS behandling_id, b.ekstern_id AS ekstern_behandling_id, f.ekstern_id AS ekstern_fagsak_id
        FROM behandling b
            JOIN fagsak f ON b.fagsak_id = f.id 
        WHERE b.id IN (:behandlingId)
        """,
    )
    fun finnEksterneIder(behandlingId: Set<UUID>): Set<EksternId>

    // language=PostgreSQL
    @Query(
        """
        SELECT DISTINCT pi.ident 
        FROM gjeldende_iverksatte_behandlinger gib 
            JOIN behandling b on gib.id = b.id
            LEFT JOIN behandling forrige_behandling on b.forrige_behandling_id=forrige_behandling.id
            JOIN person_ident pi ON gib.fagsak_person_id=pi.fagsak_person_id
        WHERE gib.stonadstype=:stønadstype 
            AND (gib.vedtakstidspunkt < ('now'::timestamp - make_interval(months := :antallMåneder)) 
                OR (b.arsak IN ('MIGRERING', 'G_OMREGNING') AND forrige_behandling.vedtakstidspunkt < ('now'::timestamp - make_interval(months := :antallMåneder))))
        AND EXISTS(SELECT 1 FROM andel_tilkjent_ytelse aty
                               JOIN tilkjent_ytelse ty ON aty.tilkjent_ytelse = ty.id
             WHERE ty.id = aty.tilkjent_ytelse
               AND ty.behandling_id = gib.id
               AND aty.belop > 0
               AND aty.stonad_tom >= 'now'::timestamp)
        EXCEPT
        (SELECT pi.ident
         FROM fagsak
                  JOIN behandling ON fagsak.id = behandling.fagsak_id
                  JOIN person_ident pi ON fagsak.fagsak_person_id = pi.fagsak_person_id
         WHERE behandling.status <> 'FERDIGSTILT' AND fagsak.stonadstype=:stønadstype)
        """,
    )
    fun finnPersonerMedAktivStonadIkkeRevurdertSisteMåneder(
        stønadstype: StønadType = StønadType.OVERGANGSSTØNAD,
        antallMåneder: Int = 3,
    ): List<String>

    // language=PostgreSQL
    @Query(
        """
        SELECT pi.ident AS first, gib.id AS second 
        FROM gjeldende_iverksatte_behandlinger gib 
            JOIN person_ident pi ON gib.fagsak_person_id=pi.fagsak_person_id
        WHERE pi.ident IN (:personidenter)
            AND gib.stonadstype=:stønadstype
    """,
    )
    fun finnSisteIverksatteBehandlingerForPersonIdenter(
        personidenter: Collection<String>,
        stønadstype: StønadType = StønadType.OVERGANGSSTØNAD,
    ): List<Pair<String, UUID>>

    // language=PostgreSQL
    @Query(
        """
        SELECT b.*, f.stonadstype
        FROM behandling b
        JOIN fagsak f ON f.id = b.fagsak_id
        JOIN oppgave o on b.id = o.behandling_id
        WHERE NOT b.status = 'FERDIGSTILT'
        AND o.ferdigstilt = false
        AND o.type in ('BehandleSak', 'GodkjenneVedtak', 'BehandleUnderkjentVedtak')
        AND o.opprettet_tid < :opprettetTidFør
        AND f.stonadstype=:stønadstype
        """,
    )
    fun hentUferdigeBehandlingerOpprettetFørDato(
        stønadstype: StønadType,
        opprettetTidFør: LocalDateTime,
    ): List<Behandling>

    @Query(
        """SELECT DISTINCT b.id 
              FROM gjeldende_iverksatte_behandlinger b   
              JOIN tilkjent_ytelse ty ON b.id = ty.behandling_id
                AND ty.grunnbelopsdato < :gjeldendeGrunnbeløpFraOgMedDato
              JOIN andel_tilkjent_ytelse aty ON aty.tilkjent_ytelse = ty.id 
                AND aty.stonad_tom > :gjeldendeGrunnbeløpFraOgMedDato
              WHERE b.stonadstype = 'OVERGANGSSTØNAD'
              AND b.fagsak_id NOT IN (SELECT b2.fagsak_id FROM behandling b2 
                                      WHERE b2.fagsak_id = b.fagsak_id
                                      AND b2.status <> 'FERDIGSTILT')""",
    )
    fun finnFerdigstilteBehandlingerMedUtdatertGBelopSomMåBehandlesManuelt(gjeldendeGrunnbeløpFraOgMedDato: LocalDate): List<UUID>
}
