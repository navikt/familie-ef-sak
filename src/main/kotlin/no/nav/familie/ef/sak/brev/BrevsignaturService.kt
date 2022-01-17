package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.brev.dto.SignaturDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.kontrakter.felles.personopplysning.ADRESSEBESKYTTELSEGRADERING
import org.springframework.stereotype.Service


@Service
class BrevsignaturService(val personopplysningerService: PersonopplysningerService) {


    fun lagSignaturMedEnhet(fagsak: Fagsak): SignaturDto {
        val harStrengtFortroligAdresse: Boolean =
                personopplysningerService.hentStrengesteAdressebeskyttelseForPersonMedRelasjoner(fagsak.hentAktivIdent())
                        .let { it == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG || it == ADRESSEBESKYTTELSEGRADERING.STRENGT_FORTROLIG_UTLAND }

        return if (harStrengtFortroligAdresse) {
            SignaturDto(FrittståendeBrevService.NAV_ANONYM_NAVN, FrittståendeBrevService.ENHET_VIKAFOSSEN)
        } else {
            SignaturDto(SikkerhetContext.hentSaksbehandlerNavn(true), FrittståendeBrevService.ENHET_NAY)
        }
    }

}