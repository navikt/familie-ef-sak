package no.nav.familie.ef.sak.integration.dto.personopplysning

import no.nav.familie.ef.sak.integration.dto.personopplysning.PersonIdent
import no.nav.familie.ef.sak.integration.dto.personopplysning.adresse.AdressePeriode
import no.nav.familie.ef.sak.integration.dto.personopplysning.status.PersonstatusPeriode
import no.nav.familie.ef.sak.integration.dto.personopplysning.tilhørighet.StatsborgerskapPeriode
import java.util.*

data class PersonhistorikkInfo(val personIdent: PersonIdent,
                               val personstatushistorikk: List<PersonstatusPeriode> = ArrayList(),
                               val statsborgerskaphistorikk: List<StatsborgerskapPeriode> = ArrayList(),
                               val adressehistorikk: List<AdressePeriode> = ArrayList())
