package no.nav.familie.ef.sak.api.gui.dto

import no.nav.familie.ef.sak.integration.dto.personopplysning.PersonhistorikkInfo
import no.nav.familie.ef.sak.integration.dto.personopplysning.Personinfo

class Person(val personinfo: Personinfo,
             val personhistorikkInfo: PersonhistorikkInfo,
             val pdlData: Map<String, Any>)
