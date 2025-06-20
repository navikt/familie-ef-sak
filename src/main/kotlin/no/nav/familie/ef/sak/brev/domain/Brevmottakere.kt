package no.nav.familie.ef.sak.brev.domain

import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table
data class Brevmottakere(
    @Id
    val behandlingId: UUID,
    val personer: PersonerWrapper,
    val organisasjoner: OrganisasjonerWrapper,
)

enum class MottakerRolle {
    BRUKER,
    VERGE,
    FULLMEKTIG,
}

data class BrevmottakerPerson(
    val personIdent: String,
    val navn: String,
    val mottakerRolle: MottakerRolle,
)

data class BrevmottakerOrganisasjon(
    val organisasjonsnummer: String,
    val navnHosOrganisasjon: String,
    val mottakerRolle: MottakerRolle,
)

data class PersonerWrapper(
    val personer: List<BrevmottakerPerson>,
)

data class OrganisasjonerWrapper(
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
