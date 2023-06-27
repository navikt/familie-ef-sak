package no.nav.familie.ef.sak.brev.domain

import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevKategori
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Deprecated("Skal slettes")
@Table("mellomlagret_frittstaende_brev")
data class MellomlagretFrittståendeBrev(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakId: UUID,
    val brev: Fritekstbrev,
    val brevType: FrittståendeBrevKategori,
    val saksbehandlerIdent: String,
    val tidspunktOpprettet: LocalDateTime = LocalDateTime.now(),
    val mottakere: FrittståendeBrevmottakere?,
)

@Deprecated("Skal slettes")
data class FrittståendeBrevmottakere(
    val personer: List<BrevmottakerPerson>,
    val organisasjoner: List<BrevmottakerOrganisasjon>,
)

@Table("brevmottakere_frittstaende_brev")
data class BrevmottakereFrittståendeBrev(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakId: UUID,
    val saksbehandlerIdent: String = SikkerhetContext.hentSaksbehandler(),
    val tidspunktOpprettet: LocalDateTime = LocalDateTime.now(),
    val personer: PersonerWrapper,
    val organisasjoner: OrganisasjonerWrapper,
)
