package no.nav.familie.ef.sak.opplysninger.søknad.domain

import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.*

@Table("soknadsskjema")
interface ISøknadsskjema {

    val id: UUID
    val type: SøknadType
    val fødselsnummer: String
    val navn: String
    val telefonnummer: String?
    val barn: Set<IBarn>
    val datoMottatt: LocalDateTime
}