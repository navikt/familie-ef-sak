package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.domain.MellomlagretFrittståendeSanitybrev
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.UUID

@Repository
interface MellomlagerFrittståendeSanitybrevRepository :
    RepositoryInterface<MellomlagretFrittståendeSanitybrev, UUID>,
    InsertUpdateRepository<MellomlagretFrittståendeSanitybrev> {
    fun findByFagsakIdAndSaksbehandlerIdent(
        fagsakId: UUID,
        saksehandlerIdent: String,
    ): MellomlagretFrittståendeSanitybrev?

    // Bruker en atomisk upsert for å unngå race condition (duplicate key) ved samtidige mellomlagringer
    // av samme fagsakId/saksbehandlerIdent, som kan oppstå ved f.eks. autolagring og manuell lagring nesten samtidig.
    @Modifying
    @Query(
        """
        INSERT INTO mellomlagret_frittstaende_sanitybrev (id, fagsak_id, brevverdier, brevmal, saksbehandler_ident, opprettet_tid)
        VALUES (:id, :fagsakId, :brevverdier, :brevmal, :saksbehandlerIdent, :opprettetTid)
        ON CONFLICT (fagsak_id, saksbehandler_ident) DO UPDATE SET
            brevverdier = excluded.brevverdier,
            brevmal = excluded.brevmal,
            opprettet_tid = excluded.opprettet_tid
        """,
    )
    fun upsert(
        id: UUID,
        fagsakId: UUID,
        brevverdier: String,
        brevmal: String,
        saksbehandlerIdent: String,
        opprettetTid: LocalDateTime,
    )
}
