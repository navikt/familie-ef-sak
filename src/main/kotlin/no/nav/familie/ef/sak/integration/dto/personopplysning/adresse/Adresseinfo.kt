package no.nav.familie.ef.sak.integration.dto.personopplysning.adresse

import no.nav.familie.ef.sak.integration.dto.personopplysning.status.PersonstatusType

class Adresseinfo(val gjeldendePostadresseType: AdresseType,
                  val mottakerNavn: String,
                  val adresselinje1: String?,
                  val adresselinje2: String?,
                  val adresselinje3: String?,
                  val adresselinje4: String?,
                  val postNr: String?,
                  val poststed: String?,
                  val land: String?,
                  val personstatus: PersonstatusType?)
