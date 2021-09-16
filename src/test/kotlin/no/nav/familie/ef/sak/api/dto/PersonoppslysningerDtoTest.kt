package no.nav.familie.ef.sak.no.nav.familie.ef.sak.api.dto

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AdresseType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Adressebeskyttelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Kjønn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.AdressebeskyttelseGradering as PdlAdressebeskyttelseGradering
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.KjønnType as PdlKjønnType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Sivilstandstype as PdlSivilstandstype

class PersonoppslysningerDtoTest {

    @Test
    internal fun `Sivilstandstype enum mapping`() {
        enumMappingValidator<Sivilstandstype, PdlSivilstandstype>()
    }

    @Test
    internal fun `Adressebeskyttelse enum mapping`() {
        enumMappingValidator<Adressebeskyttelse, PdlAdressebeskyttelseGradering>()
    }

    @Test
    internal fun `Kjønn enum mapping`() {
        enumMappingValidator<Kjønn, PdlKjønnType>()
    }

    @Test
    internal fun `Test som feiler`() {
        assertThrows(IllegalStateException::class.java) { enumMappingValidator<Sivilstandstype, AdresseType>() }
                .printStackTrace()
    }

    private inline fun <reified A : Enum<A>, reified B : Enum<B>> enumMappingValidator() {
        val aValues = enumValues<A>().map { it.name }.toSet()
        val bValues = enumValues<B>().map { it.name }.toSet()

        val aDelta = aValues.toMutableSet().let { it.removeAll(bValues); it }
        val bDelta = bValues.toMutableSet().let { it.removeAll(aValues); it }

        if (aDelta.isNotEmpty() || bDelta.isNotEmpty()) {
            throw IllegalStateException("${A::class.java.name} inneholder feltene [${aDelta.joinToString(",")}]," +
                                        " ${B::class.java.name} inneholder feltene [${bDelta.joinToString(",")}]")
        }
    }
}