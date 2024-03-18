package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface FagsakPersonRepository : RepositoryInterface<FagsakPerson, UUID>, InsertUpdateRepository<FagsakPerson> {
    @Query(
        """SELECT p.* FROM fagsak_person p WHERE 
                EXISTS(SELECT 1 FROM person_ident WHERE fagsak_person_id = p.id AND ident IN (:identer))""",
    )
    fun findByIdent(identer: Collection<String>): FagsakPerson?

    @Query("SELECT * FROM person_ident WHERE fagsak_person_id = :personId")
    fun findPersonIdenter(personId: UUID): Set<PersonIdent>

    @Query("SELECT ident FROM person_ident WHERE fagsak_person_id = :personId ORDER BY endret_tid DESC LIMIT 1")
    fun hentAktivIdent(personId: UUID): String

    @Query("SELECT * from fagsak_person fp join fagsak f on f.fagsak_person_id = fp.id AND f.id = :fagsakId")
    fun finnFagsakPersonForFagsakId(fagsakId: UUID): FagsakPerson

    @Query(
        """SELECT fp.id FROM fagsak_person fp
            WHERE fp.id NOT IN 
                (SELECT f.fagsak_person_id FROM fagsak f JOIN behandling B ON b.fagsak_id = f.id)
            AND fp.opprettet_tid < :opprettetTid
            AND fp.har_aktivert_mikrofrontend = true
    """,
    )
    fun finnFagsakPersonIderUtenBehandlingAktivertMikrofrontendOgEldreEnn(opprettetTid: LocalDate): List<UUID>

    @Query(
        """
        SELECT fp.id FROM fagsak_person fp
          JOIN fagsak f ON fp.id = f.fagsak_person_id
          JOIN gjeldende_iverksatte_behandlinger gib ON gib.fagsak_person_id = fp.id
          JOIN tilkjent_ytelse ty ON ty.behandling_id = gib.id
          JOIN andel_tilkjent_ytelse aty ON ty.id = aty.tilkjent_ytelse
        WHERE
            fp.id NOT IN (SELECT fagsak_inner.fagsak_person_id FROM fagsak fagsak_inner JOIN behandling b_inner ON fagsak_inner.id = b_inner.fagsak_id AND b_inner.status != 'FERDIGSTILT') -- Åpne behandlinger
        AND 
            fp.id NOT IN (SELECT fagsak_inner.fagsak_person_id FROM fagsak fagsak_inner JOIN behandling b_inner ON fagsak_inner.id = b_inner.fagsak_id AND b_inner.endret_tid > :sistEndretBehandling ) -- Har en behandling endret siste tiden
        AND 
            fp.har_aktivert_mikrofrontend = true
        GROUP BY fp.id
            HAVING MAX(aty.stonad_tom) < :sisteUtbetalingsdag
    """,
    )
    fun finnFagsakPersonIderMedUtbetalingerSomKanSlettes(
        sisteUtbetalingsdag: LocalDate,
        sistEndretBehandling: LocalDate,
    ): List<UUID>

    @Query(
        """
        SELECT fp.id FROM fagsak_person fp
            JOIN fagsak f ON fp.id = f.fagsak_person_id
            JOIN behandling b ON b.fagsak_id = f.id
        WHERE
            fp.id NOT IN (SELECT fagsak_inner.fagsak_person_id FROM fagsak fagsak_inner JOIN behandling b_inner ON fagsak_inner.id = b_inner.fagsak_id AND b_inner.resultat != 'HENLAGT') -- Henlagte behandlinger
        AND 
            fp.id NOT IN (SELECT fagsak_inner.fagsak_person_id FROM fagsak fagsak_inner JOIN behandling b_inner ON fagsak_inner.id = b_inner.fagsak_id AND b_inner.endret_tid > :sistEndretBehandling ) -- Har en behandling endret siste tiden
        AND 
            fp.har_aktivert_mikrofrontend = true
    """,
    )
    fun finnFagsakPersonIderForDeSomKunHarHenlagteBehandlinger(
        sistEndretBehandling: LocalDate,
    ): List<UUID>

    @Query(
        """
        SELECT fp.id FROM fagsak_person fp
            JOIN fagsak f ON fp.id = f.fagsak_person_id
            JOIN behandling b ON b.fagsak_id = f.id
        WHERE
            fp.id NOT IN (SELECT fagsak_inner.fagsak_person_id FROM fagsak fagsak_inner JOIN behandling b_inner ON fagsak_inner.id = b_inner.fagsak_id AND b_inner.resultat NOT IN ('AVSLÅTT', 'HENLAGT')) -- Avslåtte eller henlagte behandlinger
        AND 
            fp.id NOT IN (SELECT fagsak_inner.fagsak_person_id FROM fagsak fagsak_inner JOIN behandling b_inner ON fagsak_inner.id = b_inner.fagsak_id AND b_inner.endret_tid > :sistEndretBehandling ) -- Har en behandling endret siste tiden
        AND 
            fp.har_aktivert_mikrofrontend = true
    """,
    )
    fun finnFagsakPersonIderForDeSomKunHarAvslåtteBehandlinger(
        sistEndretBehandling: LocalDate,
    ): List<UUID>
}
