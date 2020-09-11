package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.gui.dto.*
import no.nav.familie.ef.sak.integration.dto.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import no.nav.familie.ef.sak.integration.dto.pdl.visningsnavn
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad as Søknad


object AleneomsorgMapper {

    fun tilDto(pdlSøker: PdlSøker,
               pdlBarn: Map<String, PdlBarn>,
               barneforeldre: Map<String, PdlAnnenForelder>,
               søknad: Søknad): Aleneomsorg {

        val søkersFnr = søknad.personalia.verdi.fødselsnummer.verdi.verdi
        val alleBarn = BarnMatcher.kobleSøknadsbarnOgRegisterBarn(søknad.barn.verdi, pdlBarn)

        return Aleneomsorg(alleBarn.map { tilBarn(it, barneforeldre, søkersFnr) },
                           pdlSøker.bostedsadresse.map { Adresse(it) })

    }

    private fun tilBarn(barn: MatchetBarn,
                        barneforeldre: Map<String, PdlAnnenForelder>,
                        søkersFnr: String): Barn {
        val navn = barn.pdlBarn?.navn?.firstOrNull()?.visningsnavn() ?: barn.søknadsbarn.navn?.verdi ?: "Ubestemt"
        val fødselsnummer = barn.fødselsnummer ?: barn.søknadsbarn.fødselsnummer?.verdi?.verdi
        val termindatoFødselsdato = barn.pdlBarn?.fødsel?.firstOrNull()?.fødselsdato ?: barn.søknadsbarn.fødselTermindato?.verdi
        val begrunnelseIkkeOppgittAnnenForelder =
                barn.søknadsbarn.annenForelder?.verdi?.ikkeOppgittAnnenForelderBegrunnelse?.verdi
        val annenForelder = tilAnnenForelder(barn, barneforeldre, søkersFnr)
        val skalBoBorHosSøker = barn.søknadsbarn.harSkalHaSammeAdresse.verdi
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
                   ?: barn.søknadsbarn.annenForelder?.verdi?.person?.verdi?.fødselsnummer?.verdi?.verdi)

        val pdlAnnenForelder = barneforeldre[fnr]

        val bostedsland = if (pdlAnnenForelder?.bostedsadresse != null) {
            "NORGE"
        } else {
            pdlAnnenForelder?.oppholdsadresse?.firstOrNull()?.utenlandskAdresse?.landkode
            ?: barn.søknadsbarn.annenForelder?.verdi?.land?.verdi ?: "UKJENT"
        }

        return Forelder(fødselsnummerAnnenForelder = fnr,
                        bostedsland = bostedsland,
                        harForeldreneBoddSammen = barn.søknadsbarn.samvær?.verdi?.harDereTidligereBoddSammen?.verdi,
                        fraflyttingsdato = barn.søknadsbarn.samvær?.verdi?.nårFlyttetDereFraHverandre?.verdi,
                        foreldresKontakt = barn.søknadsbarn.samvær?.verdi?.hvordanPraktiseresSamværet?.verdi,
                        næreBoforhold = barn.søknadsbarn.samvær?.verdi?.borAnnenForelderISammeHus?.verdi,
                        kanSøkerAnsesÅHaAleneomsorgen = null,
                        aleneomsorgBegrunnelse = null,
                        adresser = pdlAnnenForelder?.bostedsadresse?.map { Adresse(it) })


    }


    private fun tilDeltBosted(barn: MatchetBarn): DeltBosted {

        val søknadDeltBosted = barn.søknadsbarn.samvær?.verdi?.spørsmålAvtaleOmDeltBosted?.verdi
        val dokumentasjon = barn.søknadsbarn.samvær?.verdi?.avtaleOmDeltBosted?.verdi?.dokumenter
        val startdatoForKontrakt = barn.pdlBarn?.deltBosted?.firstOrNull()?.startdatoForKontrakt
        val sluttdatoForKontrakt = barn.pdlBarn?.deltBosted?.firstOrNull()?.sluttdatoForKontrakt

        return DeltBosted(søknadDeltBosted, dokumentasjon, startdatoForKontrakt, sluttdatoForKontrakt)


    }


}