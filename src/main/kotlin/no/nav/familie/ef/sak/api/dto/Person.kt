package no.nav.familie.ef.sak.api.dto

import no.nav.familie.ef.sak.integration.dto.personopplysning.PersonhistorikkInfo
import no.nav.familie.ef.sak.integration.dto.personopplysning.Personinfo

class Person(val personinfo: Personinfo,
             val personhistorikkInfo: PersonhistorikkInfo)
