package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.*
import no.nav.familie.ef.sak.integration.dto.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import no.nav.familie.ef.sak.integration.dto.pdl.visningsnavn
import no.nav.familie.ef.sak.repository.domain.søknad.SøknadsskjemaOvergangsstønad


object AleneomsorgMapper {

    fun tilDto(pdlSøker: PdlSøker,
               pdlBarn: Map<String, PdlBarn>,
               barneforeldre: Map<String, PdlAnnenForelder>,
               søknadsskjemaOvergangsstønad: SøknadsskjemaOvergangsstønad): Aleneomsorg {

        val søkersFnr = søknadsskjemaOvergangsstønad.fødselsnummer
        val alleBarn = BarnMatcher.kobleSøknadsbarnOgRegisterBarn(søknadsskjemaOvergangsstønad.barn, pdlBarn)

        return Aleneomsorg(alleBarn.map { tilBarn(it, barneforeldre, søkersFnr) },
                           pdlSøker.bostedsadresse.map { Adresse(it) })

    }

    private fun tilBarn(barn: MatchetBarn,
                        barneforeldre: Map<String, PdlAnnenForelder>,
                        søkersFnr: String): Barn {
        val navn = barn.pdlBarn?.navn?.firstOrNull()?.visningsnavn() ?: barn.søknadsbarn.navn ?: "Ubestemt"
        val fødselsnummer = barn.fødselsnummer ?: barn.søknadsbarn.fødselsnummer
        val termindatoFødselsdato = barn.pdlBarn?.fødsel?.firstOrNull()?.fødselsdato ?: barn.søknadsbarn.fødselTermindato
        val begrunnelseIkkeOppgittAnnenForelder = barn.søknadsbarn.annenForelder?.ikkeOppgittAnnenForelderBegrunnelse
        val annenForelder = tilAnnenForelder(barn, barneforeldre, søkersFnr)
        val skalBoBorHosSøker = barn.søknadsbarn.harSkalHaSammeAdresse
        val deltBosted = tilDeltBosted(barn)
        val fraRegister = barn.pdlBarn != null

        return Barn(navn,
                    fødselsnummer,
                    termindatoFødselsdato,
                    begrunnelseIkkeOppgittAnnenForelder,
                    annenForelder,
                    skalBoBorHosSøker,
                    deltBosted,
                    fraRegister)
    }

    private fun tilAnnenForelder(barn: MatchetBarn, barneforeldre: Map<String, PdlAnnenForelder>, søkersFnr: String): Forelder? {

        val fnr = (barn.pdlBarn?.familierelasjoner?.firstOrNull { it.relatertPersonsIdent != søkersFnr }?.relatertPersonsIdent
                   ?: barn.søknadsbarn.annenForelder?.person?.fødselsnummer)

        val pdlAnnenForelder = barneforeldre[fnr]

        val bostedsland = if (pdlAnnenForelder?.bostedsadresse != null) {
            pdlAnnenForelder.bostedsadresse.firstOrNull()?.utenlandskAdresse?.landkode ?: "NORGE"
        } else {
            pdlAnnenForelder?.oppholdsadresse?.firstOrNull()?.utenlandskAdresse?.landkode
            ?: barn.søknadsbarn.annenForelder?.land ?: "UKJENT"
        }

        return Forelder(fødselsnummerAnnenForelder = fnr,
                        navn = pdlAnnenForelder?.navn?.firstOrNull()?.visningsnavn()
                               ?: barn.søknadsbarn.annenForelder?.person?.navn,
                        fødselsdato = barn.søknadsbarn.annenForelder?.person?.fødselsdato,
                        bostedsland = bostedsland,
                        harForeldreneBoddSammen = barn.søknadsbarn.samvær?.harDereTidligereBoddSammen,
                        fraflyttingsdato = barn.søknadsbarn.samvær?.nårFlyttetDereFraHverandre,
                        foreldresKontakt = barn.søknadsbarn.samvær?.hvordanPraktiseresSamværet,
                        næreBoforhold = barn.søknadsbarn.samvær?.borAnnenForelderISammeHus,
                        kanSøkerAnsesÅHaAleneomsorgen = null,
                        aleneomsorgBegrunnelse = null,
                        adresser = pdlAnnenForelder?.bostedsadresse?.map { Adresse(it) })
    }


    private fun tilDeltBosted(barn: MatchetBarn): DeltBosted {

        val søknadDeltBosted = barn.søknadsbarn.samvær?.spørsmålAvtaleOmDeltBosted
        val dokumentasjon = barn.søknadsbarn.samvær?.avtaleOmDeltBosted?.dokumenter ?: emptyList()
        val startdatoForKontrakt = barn.pdlBarn?.deltBosted?.firstOrNull()?.startdatoForKontrakt
        val sluttdatoForKontrakt = barn.pdlBarn?.deltBosted?.firstOrNull()?.sluttdatoForKontrakt

        return DeltBosted(søknadDeltBosted, dokumentasjon, startdatoForKontrakt, sluttdatoForKontrakt)


    }


}