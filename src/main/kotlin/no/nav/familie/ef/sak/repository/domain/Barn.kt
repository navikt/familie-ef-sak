package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.kontrakter.ef.søknad.NyttBarn
import no.nav.familie.kontrakter.ef.søknad.RegistrertBarn
import no.nav.familie.kontrakter.ef.søknad.Søknad
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
    fun toDomain(søknad: Søknad): Set<Barn> {
        return (søknad.kommendeBarn?.verdi?.map(this::toBarn)?.toSet() ?: emptySet()) +
               (søknad.folkeregisterbarn?.verdi?.map(this::toBarn)?.toSet() ?: emptySet())

    }

    private fun toBarn(nyttBarn: NyttBarn): Barn {
        return Barn(navn = nyttBarn.navn?.verdi,
                    fødselsnummer = nyttBarn.fødselsnummer?.verdi,
                    fødselsdato = nyttBarn.fødselTermindato.verdi,
                    harSammeAdresse = nyttBarn.skalBarnetBoHosSøker.verdi,
                    annenForelder = AnnenForelderMapper.toDomain(nyttBarn.annenForelder?.verdi))
    }

    private fun toBarn(registrertBarn: RegistrertBarn): Barn {
        return Barn(navn = registrertBarn.navn.verdi,
                    fødselsnummer = registrertBarn.fødselsnummer.verdi.verdi,
                    fødselsdato = registrertBarn.fødselsnummer.verdi.fødselsdato,
                    harSammeAdresse = registrertBarn.harSammeAdresse.verdi,
                    annenForelder = AnnenForelderMapper.toDomain(registrertBarn.annenForelder?.verdi))
    }
}


