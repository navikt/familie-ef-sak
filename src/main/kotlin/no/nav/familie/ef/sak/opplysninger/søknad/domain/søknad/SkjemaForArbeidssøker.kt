package no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDateTime


data class SkjemaForArbeidssøker(@Column("fodselsnummer")
                                 val fødselsnummer: String,
                                 @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
                                 val arbeidssøker: Arbeidssøker,
                                 val datoMottatt: LocalDateTime)
