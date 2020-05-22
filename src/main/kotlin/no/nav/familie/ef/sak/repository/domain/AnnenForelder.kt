package no.nav.familie.ef.sak.repository.domain

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded


data class AnnenForelder(val navn: String?,
                         @Column("fodselsnummer")
                         val fødselsnummer: String?,
                         val bosattNorge: Boolean? = true,
                         @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
                         val annenForelderAdresse: AnnenForelderAdresse? = null)

object AnnenForelderMapper {

    fun toDomain(annenForelder: no.nav.familie.kontrakter.ef.søknad.AnnenForelder?): AnnenForelder? {
        if (annenForelder == null) {
            return null
        }

        return AnnenForelder(navn = annenForelder.person?.verdi?.navn?.verdi,
                             fødselsnummer = annenForelder.person?.verdi?.fødselsnummer?.verdi?.verdi,
                             bosattNorge = annenForelder.bosattNorge?.verdi)
    }


}