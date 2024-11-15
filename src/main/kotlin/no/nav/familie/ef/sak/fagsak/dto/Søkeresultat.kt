package no.nav.familie.ef.sak.fagsak.dto

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Kjønn
import no.nav.familie.kontrakter.felles.ef.StønadType
import java.time.LocalDate
import java.util.UUID

data class Søkeresultat(
    val personIdent: String,
    val visningsnavn: String,
    val kjønn: Kjønn,
    val fagsakPersonId: UUID?,
    val fagsaker: List<FagsakForSøkeresultat>,
)

data class FagsakForSøkeresultat(
    val fagsakId: UUID,
    val stønadstype: StønadType,
    val erLøpende: Boolean,
    val erMigrert: Boolean,
)

data class PersonPåAdresse(
    val personIdent: String,
    val visningsadresse: String?,
    val visningsnavn: String,
    val fødselsdato: LocalDate? = null,
    val erSøker: Boolean? = null,
    val erBarn: Boolean? = null,
)

data class SøkeresultatPerson(
    val personer: List<PersonPåAdresse>,
)

data class SøkeresultatUtenFagsak(
    val personIdent: String,
    val navn: String,
)
