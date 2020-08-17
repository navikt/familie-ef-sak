package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.kontrakter.ef.søknad.Barn as KontraktBarn
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate

data class Barn(val navn: String?,
                @Column("fodselsnummer")
                val fødselsnummer: String?,
                @Column("fodselsdato")
                val fødselsdato: LocalDate,
                val harSammeAdresse: Boolean,
                @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "forelder2_")
                val annenForelder: AnnenForelder? = null)

object BarnMapper {
    fun toDomain(søknad: SøknadOvergangsstønad): Set<Barn> {
        return søknad.barn.verdi.map(this::toBarn).toSet()

    }

    private fun toBarn(barn: KontraktBarn): Barn {
        val fødselsdato = barn.fødselTermindato?.verdi ?:
                          barn.fødselsnummer?.verdi?.fødselsdato ?:
                          error("Både fødselsdato og fødselsnummer mangler på barn")
        return Barn(navn = barn.navn?.verdi,
                    fødselsnummer = barn.fødselsnummer?.verdi?.verdi,
                    fødselsdato = fødselsdato,
                    harSammeAdresse = barn.harSkalHaSammeAdresse.verdi,
                    annenForelder = AnnenForelderMapper.toDomain(barn.annenForelder?.verdi))
    }
}


