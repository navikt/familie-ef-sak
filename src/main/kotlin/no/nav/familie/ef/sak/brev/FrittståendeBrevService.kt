package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.brev.domain.BrevmottakerOrganisasjon
import no.nav.familie.ef.sak.brev.domain.BrevmottakerPerson
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevDto
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevKategori
import no.nav.familie.ef.sak.brev.dto.FrittståendeBrevRequestDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.tilIverksettDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevType
import no.nav.familie.kontrakter.ef.iverksett.Brevmottaker
import org.springframework.stereotype.Service
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevDto as FrittståendeBrevDtoIverksetting

@Service
class FrittståendeBrevService(
    private val brevClient: BrevClient,
    private val fagsakService: FagsakService,
    private val personopplysningerService: PersonopplysningerService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val iverksettClient: IverksettClient,
    private val brevsignaturService: BrevsignaturService
) {

    fun forhåndsvisFrittståendeBrev(frittståendeBrevDto: FrittståendeBrevDto): ByteArray {
        return lagFrittståendeBrevMedSignatur(frittståendeBrevDto)
    }

    fun sendFrittståendeBrev(frittståendeBrevDto: FrittståendeBrevDto) {
        val mottakere = validerOgMapBrevmottakere(frittståendeBrevDto.mottakere)
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(frittståendeBrevDto.fagsakId)
        val ident = fagsak.hentAktivIdent()
        val brev = lagFrittståendeBrevMedSignatur(frittståendeBrevDto, fagsak)
        val journalførendeEnhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(ident)
        iverksettClient.sendFrittståendeBrev(
            FrittståendeBrevDtoIverksetting(
                personIdent = ident,
                eksternFagsakId = fagsak.eksternId.id,
                stønadType = fagsak.stønadstype,
                brevtype = utledFrittståendeBrevtype(frittståendeBrevDto.brevType),
                fil = brev,
                journalførendeEnhet = journalførendeEnhet,
                saksbehandlerIdent = SikkerhetContext.hentSaksbehandler(true),
                mottakere = mottakere
            )
        )
    }

    private fun mapMottakere(mottakere: BrevmottakereDto): List<Brevmottaker> {
        val personer = mottakere.personer.map(BrevmottakerPerson::tilIverksettDto)
        val organisasjoner = mottakere.organisasjoner.map(BrevmottakerOrganisasjon::tilIverksettDto)
        return personer + organisasjoner
    }

    private fun lagFrittståendeBrevRequest(
        frittståendeBrevDto: FrittståendeBrevDto,
        ident: String
    ): FrittståendeBrevRequestDto {
        val navn = personopplysningerService.hentGjeldeneNavn(listOf(ident))
        return FrittståendeBrevRequestDto(
            overskrift = frittståendeBrevDto.overskrift,
            avsnitt = frittståendeBrevDto.avsnitt,
            personIdent = ident,
            navn = navn.getValue(ident)
        )
    }

    private fun lagFrittståendeBrevMedSignatur(frittståendeBrevDto: FrittståendeBrevDto): ByteArray {
        val fagsak = fagsakService.hentFagsak(frittståendeBrevDto.fagsakId)
        return lagFrittståendeBrevMedSignatur(frittståendeBrevDto, fagsak)
    }

    private fun lagFrittståendeBrevMedSignatur(frittståendeBrevDto: FrittståendeBrevDto, fagsak: Fagsak): ByteArray {
        val request = lagFrittståendeBrevRequest(frittståendeBrevDto, fagsak.hentAktivIdent())
        val signatur = brevsignaturService.lagSignaturMedEnhet(fagsak)
        return brevClient.genererBrev(request, signatur.navn, signatur.enhet)
    }

    private fun utledFrittståendeBrevtype(brevKategori: FrittståendeBrevKategori): FrittståendeBrevType =
        when (brevKategori) {
            FrittståendeBrevKategori.INFORMASJONSBREV -> FrittståendeBrevType.INFORMASJONSBREV
            FrittståendeBrevKategori.INNHENTING_AV_OPPLYSNINGER -> FrittståendeBrevType.INNHENTING_AV_OPPLYSNINGER
            FrittståendeBrevKategori.VARSEL_OM_AKTIVITETSPLIKT -> FrittståendeBrevType.VARSEL_OM_AKTIVITETSPLIKT
            FrittståendeBrevKategori.VARSEL_OM_SANKSJON -> FrittståendeBrevType.VARSEL_OM_SANKSJON
            FrittståendeBrevKategori.INNHENTING_AV_KARAKTERUTSKRIFT_HOVEDPERIODE -> FrittståendeBrevType.INNHENTING_AV_KARAKTERUTSKRIFT_HOVEDPERIODE
            FrittståendeBrevKategori.INNHENTING_AV_KARAKTERUTSKRIFT_UTVIDET_PERIODE -> FrittståendeBrevType.INNHENTING_AV_KARAKTERUTSKRIFT_UTVIDET_PERIODE
        }

    private fun validerOgMapBrevmottakere(mottakere: BrevmottakereDto?): List<Brevmottaker> {
        brukerfeilHvis(mottakere == null || (mottakere.personer.isEmpty() && mottakere.organisasjoner.isEmpty())) {
            "Kan ikke sende frittstående brev uten at minst en brevmottaker er lagt til"
        }
        return mapMottakere(mottakere)
    }
}
