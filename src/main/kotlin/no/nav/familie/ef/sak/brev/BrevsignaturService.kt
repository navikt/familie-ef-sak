package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.dto.SignaturDto
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND
import org.springframework.stereotype.Service


@Service
class BrevsignaturService(val personopplysningerService: PersonopplysningerService) {


    fun lagSignaturMedEnhet(fagsak: Fagsak): SignaturDto {
        val harStrengtFortroligAdresse: Boolean =
                personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(fagsak.hentAktivIdent())
                        .let { it == STRENGT_FORTROLIG || it == STRENGT_FORTROLIG_UTLAND }

        return if (harStrengtFortroligAdresse) {
            SignaturDto(NAV_ANONYM_NAVN, ENHET_VIKAFOSSEN)
        } else {
            SignaturDto(SikkerhetContext.hentSaksbehandlerNavn(true), ENHET_NAY)
        }
    }

    companion object {

        val NAV_ANONYM_NAVN = "NAV anonym"
        val ENHET_VIKAFOSSEN = "NAV Vikafossen"
        val ENHET_NAY = "NAV Arbeid og ytelser"
    }

}