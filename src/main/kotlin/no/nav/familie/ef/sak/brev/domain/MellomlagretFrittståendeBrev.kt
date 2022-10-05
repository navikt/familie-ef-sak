package no.nav.familie.ef.sak.brev.domain

import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevKategori
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("mellomlagret_frittstaende_brev")
data class MellomlagretFrittståendeBrev(
    @Id val id: UUID = UUID.randomUUID(),
    val fagsakId: UUID,
    val brev: Fritekstbrev,
    val brevType: FrittståendeBrevKategori,
    val saksbehandlerIdent: String,
    val tidspunktOpprettet: LocalDateTime = LocalDateTime.now(),
    val mottakere: FrittståendeBrevmottakere?
)

data class FrittståendeBrevmottakere(
    val personer: List<BrevmottakerPerson>,
    val organisasjoner: List<BrevmottakerOrganisasjon>
)
