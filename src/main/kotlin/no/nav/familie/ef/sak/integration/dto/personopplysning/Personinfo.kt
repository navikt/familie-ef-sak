package no.nav.familie.ef.sak.integration.dto.personopplysning

import no.nav.familie.ef.sak.integration.dto.personopplysning.adresse.Adresseinfo
import no.nav.familie.ef.sak.integration.dto.personopplysning.relasjon.Familierelasjon
import no.nav.familie.ef.sak.integration.dto.personopplysning.relasjon.SivilstandType
import no.nav.familie.ef.sak.integration.dto.personopplysning.status.PersonstatusType
import no.nav.familie.ef.sak.integration.dto.personopplysning.tilhørighet.Landkode
import java.time.LocalDate
import java.util.*

class Personinfo(val personIdent: PersonIdent,
                 val navn: String,
                 val bostedsadresse: Adresseinfo?,
                 val kjønn: String?,
                 val fødselsdato: LocalDate,
                 val dødsdato: LocalDate?,
                 val personstatus: PersonstatusType?,
                 val sivilstand: SivilstandType?,
                 val familierelasjoner: Set<Familierelasjon> = emptySet(),
                 val statsborgerskap: Landkode?,
                 val utlandsadresse: String?,
                 val geografiskTilknytning: String?,
                 val diskresjonskode: String?,
                 val adresseLandkode: String?,
                 val adresseInfoList: List<Adresseinfo> = ArrayList(),
                 val alder: Int)
