package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface GjeldendeBarnRepository :
    RepositoryInterface<BarnTilUtplukkForOppgave, UUID>,
    InsertUpdateRepository<BarnTilUtplukkForOppgave> {

    // language=PostgreSQL
    @Query(
        """
        SELECT b.id behandling_id, pi.ident fodselsnummer_soker, bb.person_ident fodselsnummer_barn, 
           FALSE AS fra_migrering
        FROM gjeldende_iverksatte_behandlinger b
         JOIN (SELECT DISTINCT ON(pi.fagsak_person_id) * FROM person_ident pi ORDER BY pi.fagsak_person_id, pi.opprettet_tid DESC) pi ON pi.fagsak_person_id = b.fagsak_person_id
         JOIN behandling_barn bb ON bb.behandling_id = b.id
        WHERE b.stonadstype=:stønadstype AND EXISTS(SELECT 1 FROM andel_tilkjent_ytelse aty
            JOIN tilkjent_ytelse ty ON aty.tilkjent_ytelse = ty.id
            WHERE ty.id = aty.tilkjent_ytelse 
            AND ty.behandling_id = b.id
          AND aty.stonad_tom >= :dato
          AND aty.belop > 0)
        """
    )
    fun finnBarnAvGjeldendeIverksatteBehandlinger(stønadstype: StønadType, dato: LocalDate): List<BarnTilUtplukkForOppgave>

    @Query(
        """
        SELECT b.id behandling_id, pi.ident fodselsnummer_soker, 
         JSON_ARRAY_ELEMENTS(data -> 'barn') ->> 'personIdent' fodselsnummer_barn, TRUE AS fra_migrering
        FROM gjeldende_iverksatte_behandlinger b
         JOIN (SELECT DISTINCT ON(pi.fagsak_person_id) * FROM person_ident pi ORDER BY pi.fagsak_person_id, pi.opprettet_tid DESC) pi ON pi.fagsak_person_id = b.fagsak_person_id
         JOIN grunnlagsdata g ON g.behandling_id = b.id
        WHERE NOT EXISTS(SELECT FROM behandling_barn WHERE behandling_id = b.id)
         AND EXISTS(SELECT 
                    FROM andel_tilkjent_ytelse aty
                     JOIN tilkjent_ytelse ty ON aty.tilkjent_ytelse = ty.id
                    WHERE ty.id = aty.tilkjent_ytelse
                     AND ty.behandling_id = b.id
                     AND aty.stonad_tom >= :dato
                     AND aty.belop > 0)
         AND b.migrert = TRUE
    """
    )
    fun finnBarnTilMigrerteBehandlinger(stønadstype: StønadType, dato: LocalDate): List<BarnTilUtplukkForOppgave>

    // language=PostgreSQL
    @Query(
        """
        SELECT bb.person_ident barn_person_ident, b.id behandling_id, be.id ekstern_behandling_id, fe.id ekstern_fagsak_id
        FROM behandling b
            JOIN behandling_ekstern be ON b.id = be.behandling_id
            JOIN fagsak_ekstern fe ON b.fagsak_id = fe.fagsak_id
            JOIN behandling_barn bb ON b.id = bb.behandling_id
        WHERE b.id IN (:behandlingIds) 
        """
    )
    fun finnEksternFagsakIdForBehandlingId(behandlingIds: List<UUID>): Set<BarnTilOppgave>
}
