package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.domain.MellomlagretBrev
import no.nav.familie.ef.sak.repository.InsertUpdateRepository
import no.nav.familie.ef.sak.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Modifying
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.UUID

@Repository
interface MellomlagerBrevRepository :
    RepositoryInterface<MellomlagretBrev, UUID>,
    InsertUpdateRepository<MellomlagretBrev> {
    // Bruker en atomisk upsert for å unngå race condition (duplicate key) ved samtidige mellomlagringer
    // av samme behandlingId, som kan oppstå ved f.eks. autolagring og manuell lagring nesten samtidig.
    @Modifying
    @Query(
        """
        INSERT INTO mellomlagret_brev (behandling_id, brevverdier, brevmal, sanity_versjon, opprettet_tid)
        VALUES (:behandlingId, :brevverdier, :brevmal, :sanityVersjon, :opprettetTid)
        ON CONFLICT (behandling_id) DO UPDATE SET
            brevverdier = excluded.brevverdier,
            brevmal = excluded.brevmal,
            sanity_versjon = excluded.sanity_versjon,
            opprettet_tid = excluded.opprettet_tid
        """,
    )
    fun upsert(
        behandlingId: UUID,
        brevverdier: String,
        brevmal: String,
        sanityVersjon: String,
        opprettetTid: LocalDate,
    )
}
