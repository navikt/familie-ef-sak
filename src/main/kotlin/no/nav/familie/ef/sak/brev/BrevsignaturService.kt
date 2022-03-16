package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.brev.dto.SignaturDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND
import org.springframework.stereotype.Service
import java.util.UUID


@Service
class BrevsignaturService(val personopplysningerService: PersonopplysningerService, val fagsakService: FagsakService) {


    fun lagSignaturMedEnhet(fagsakId: UUID): SignaturDto {
        val fagsak = fagsakService.hentFagsak(fagsakId)
        val harStrengtFortroligAdresse: Boolean =
                personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(fagsak.hentAktivIdent())
                        .let { it == STRENGT_FORTROLIG || it == STRENGT_FORTROLIG_UTLAND }

        return if (harStrengtFortroligAdresse) {
            SignaturDto(NAV_ANONYM_NAVN, ENHET_VIKAFOSSEN, true)
        } else {
            SignaturDto(SikkerhetContext.hentSaksbehandlerNavn(true), ENHET_NAY, false)
        }
    }

    companion object {

        val NAV_ANONYM_NAVN = "NAV anonym"
        val ENHET_VIKAFOSSEN = "NAV Vikafossen"
        val ENHET_NAY = "NAV Arbeid og ytelser"
    }

}