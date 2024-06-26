package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.fagsak.domain.FagsakDomain
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface FagsakRepository :
    RepositoryInterface<FagsakDomain, UUID>,
    InsertUpdateRepository<FagsakDomain> {
    // language=PostgreSQL
    @Query(
        """SELECT DISTINCT f.*
                    FROM fagsak f 
                    LEFT JOIN person_ident pi ON pi.fagsak_person_id = f.fagsak_person_id 
                    WHERE pi.ident IN (:personIdenter)
                    AND f.stonadstype = :stønadstype""",
    )
    fun findBySøkerIdent(
        personIdenter: Set<String>,
        stønadstype: StønadType,
    ): FagsakDomain?

    fun findByFagsakPersonIdAndStønadstype(
        fagsakPersonId: UUID,
        stønadstype: StønadType,
    ): FagsakDomain?

    // language=PostgreSQL
    @Query(
        """SELECT f.*
                    FROM fagsak f
                    JOIN behandling b 
                        ON b.fagsak_id = f.id 
                    WHERE b.id = :behandlingId""",
    )
    fun finnFagsakTilBehandling(behandlingId: UUID): FagsakDomain?

    // language=PostgreSQL
    @Query(
        """SELECT DISTINCT f.*
             FROM fagsak f 
                JOIN person_ident pi ON pi.fagsak_person_id = f.fagsak_person_id 
              WHERE ident IN (:personIdenter)""",
    )
    fun findBySøkerIdent(personIdenter: Set<String>): List<FagsakDomain>

    fun findByFagsakPersonId(fagsakPersonId: UUID): List<FagsakDomain>

    // language=PostgreSQL
    @Query(
        """SELECT f.*         
                    FROM fagsak f         
                    WHERE f.ekstern_id = :eksternId""",
    )
    fun finnMedEksternId(eksternId: Long): FagsakDomain?

    // language=PostgreSQL
    @Query(
        """SELECT pi.ident FROM fagsak f
                JOIN person_ident pi ON pi.fagsak_person_id = f.fagsak_person_id
              WHERE f.id=:fagsakId
              ORDER BY pi.endret_tid DESC
              LIMIT 1""",
    )
    fun finnAktivIdent(fagsakId: UUID): String

    // language=PostgreSQL
    @Query(
        """
        SELECT DISTINCT f.id AS first, 
            FIRST_VALUE(ident) OVER (PARTITION BY pi.fagsak_person_id ORDER BY pi.endret_tid DESC) AS second
        FROM fagsak f
          JOIN person_ident pi ON pi.fagsak_person_id = f.fagsak_person_id
        WHERE f.id IN (:fagsakIder)""",
    )
    fun finnAktivIdenter(fagsakIder: Set<UUID>): List<Pair<UUID, String>>

    // language=PostgreSQL
    @Query(
        """SELECT COUNT(*) > 0 FROM gjeldende_iverksatte_behandlinger b 
              JOIN tilkjent_ytelse ty
              ON b.id = ty.behandling_id
              JOIN andel_tilkjent_ytelse aty 
              ON ty.id = aty.tilkjent_ytelse 
              AND aty.stonad_tom >= CURRENT_DATE 
              WHERE b.fagsak_id = :fagsakId
              LIMIT 1""",
    )
    fun harLøpendeUtbetaling(fagsakId: UUID): Boolean

    // language=PostgreSQL
    @Query(
        """SELECT DISTINCT b.fagsak_id 
              FROM gjeldende_iverksatte_behandlinger b   
              JOIN tilkjent_ytelse ty ON b.id = ty.behandling_id
                AND ty.grunnbelopsdato < :gjeldendeGrunnbeløpFraOgMedDato
              JOIN andel_tilkjent_ytelse aty ON aty.tilkjent_ytelse = ty.id 
                AND aty.samordningsfradrag = 0
                AND aty.stonad_tom > :gjeldendeGrunnbeløpFraOgMedDato
              WHERE b.stonadstype = 'OVERGANGSSTØNAD'
              AND b.fagsak_id NOT IN (SELECT b2.fagsak_id FROM behandling b2 
                                      WHERE b2.fagsak_id = b.fagsak_id
                                      AND b2.status <> 'FERDIGSTILT')""",
    )
    fun finnFerdigstilteFagsakerMedUtdatertGBelop(gjeldendeGrunnbeløpFraOgMedDato: LocalDate): List<UUID>
}
