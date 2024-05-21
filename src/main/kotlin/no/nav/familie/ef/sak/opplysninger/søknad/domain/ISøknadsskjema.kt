package no.nav.familie.ef.sak.opplysninger.søknad.domain

import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Table("soknadsskjema")
interface ISøknadsskjema {
    val id: UUID
    val type: SøknadType
    val fødselsnummer: String
    val navn: String
    val barn: Set<IBarn>
    val datoMottatt: LocalDateTime
    val datoPåbegyntSøknad: LocalDate?
}
