package no.nav.familie.ef.sak.repository.domain.søknad

import no.nav.familie.ef.sak.repository.domain.SøknadType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.MappedCollection
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.*

@Table("soknadsskjema")
class Søknadsskjema(@Id override val id: UUID,
                    override val type: SøknadType,
                    @Column("fodselsnummer")
                    override val fødselsnummer: String,
                    override val navn: String,
                    override val telefonnummer: String?,
                    @MappedCollection(idColumn = "soknadsskjema_id")
                    override val barn: Set<Barn>,
                    override val datoMottatt: LocalDateTime) : ISøknadsskjema
