package no.nav.familie.ef.sak.klage

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.klage.dto.KlagebehandlingerDto
import no.nav.familie.ef.sak.klage.dto.OpprettKlageDto
import no.nav.familie.ef.sak.klage.dto.ÅpneKlagerInfotrygdDto
import no.nav.familie.kontrakter.felles.klage.BehandlingEventType
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
import no.nav.familie.kontrakter.felles.klage.Klagebehandlingsårsak
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
class KlageService(
    private val fagsakService: FagsakService,
    private val fagsakPersonService: FagsakPersonService,
    private val klageClient: KlageClient,
    private val infotrygdService: InfotrygdService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val featureToggleService: FeatureToggleService,
) {
    fun hentBehandlinger(fagsakPersonId: UUID): KlagebehandlingerDto {
        val fagsaker = fagsakService.finnFagsakerForFagsakPersonId(fagsakPersonId)
        val eksternFagsakIder =
            fagsaker.let {
                listOfNotNull(
                    it.overgangsstønad?.eksternId,
                    it.barnetilsyn?.eksternId,
                    it.skolepenger?.eksternId,
                )
            }
        if (eksternFagsakIder.isEmpty()) {
            return KlagebehandlingerDto(emptyList(), emptyList(), emptyList())
        }
        val klagebehandlingerPåEksternId =
            klageClient.hentKlagebehandlinger(eksternFagsakIder.toSet()).mapValues { klagebehandlingerPåEksternId ->
                klagebehandlingerPåEksternId.value.map { brukVedtaksdatoFraKlageinstansHvisOversendt(it) }
            }

        return KlagebehandlingerDto(
            overgangsstønad = klagebehandlingerPåEksternId[fagsaker.overgangsstønad?.eksternId] ?: emptyList(),
            barnetilsyn = klagebehandlingerPåEksternId[fagsaker.barnetilsyn?.eksternId] ?: emptyList(),
            skolepenger = klagebehandlingerPåEksternId[fagsaker.skolepenger?.eksternId] ?: emptyList(),
        )
    }

    fun validerOgOpprettKlage(
        fagsak: Fagsak,
        opprettKlageDto: OpprettKlageDto,
    ) {
        brukerfeilHvis(opprettKlageDto.mottattDato.isAfter(LocalDate.now())) {
            "Kan ikke opprette klage med krav mottatt frem i tid for fagsak=${fagsak.id}"
        }
        brukerfeilHvis(!featureToggleService.isEnabled(Toggle.VELG_ÅRSAK_VED_KLAGE_OPPRETTELSE) && opprettKlageDto.behandlingsårsak != Klagebehandlingsårsak.ORDINÆR) {
            "Du har valgt en årsak du ikke har tilgang til å velge. Vennligst meld i fra til brukerstøtte dersom du skal ha tilgang til valg av denne årsaken. Fagsak=${fagsak.id}"
        }
        val aktivIdent = fagsak.hentAktivIdent()
        val enhetId = arbeidsfordelingService.hentNavEnhet(aktivIdent)?.enhetId
        brukerfeilHvis(enhetId == null) {
            "Finner ikke behandlende enhet for personen"
        }
        opprettKlage(fagsak, opprettKlageDto, aktivIdent, enhetId)
    }

    private fun opprettKlage(
        fagsak: Fagsak,
        opprettKlageDto: OpprettKlageDto,
        aktivIdent: String,
        enhetId: String,
    ) = klageClient.opprettKlage(
        OpprettKlagebehandlingRequest(
            ident = aktivIdent,
            stønadstype = Stønadstype.fraEfStønadstype(fagsak.stønadstype),
            eksternFagsakId = fagsak.eksternId.toString(),
            fagsystem = Fagsystem.EF,
            klageMottatt = opprettKlageDto.mottattDato,
            behandlendeEnhet = enhetId,
            klageGjelderTilbakekreving = opprettKlageDto.klageGjelderTilbakekreving,
            behandlingsårsak = opprettKlageDto.behandlingsårsak,
        ),
    )

    fun hentÅpneKlagerInfotrygd(fagsakPersonId: UUID): ÅpneKlagerInfotrygdDto {
        val fagsakPerson = fagsakPersonService.hentPerson(fagsakPersonId)
        return hentÅpneKlagerFraInfotrygd(fagsakPerson)
    }

    private fun brukVedtaksdatoFraKlageinstansHvisOversendt(klagebehandling: KlagebehandlingDto): KlagebehandlingDto {
        val erOversendtTilKlageinstans = klagebehandling.resultat == BehandlingResultat.IKKE_MEDHOLD
        val vedtaksdato =
            if (erOversendtTilKlageinstans) {
                klagebehandling.klageinstansResultat.singleOrNull { klageinnstansResultat -> klageinnstansResultat.type == BehandlingEventType.KLAGEBEHANDLING_AVSLUTTET || klageinnstansResultat.type == BehandlingEventType.BEHANDLING_FEILREGISTRERT }?.mottattEllerAvsluttetTidspunkt
            } else {
                klagebehandling.vedtaksdato
            }
        return klagebehandling.copy(vedtaksdato = vedtaksdato)
    }

    private fun hentÅpneKlagerFraInfotrygd(fagsakPerson: FagsakPerson): ÅpneKlagerInfotrygdDto =
        infotrygdService
            .hentÅpneKlagesaker(fagsakPerson.hentAktivIdent())
            .map { it.stønadType }
            .let { ÅpneKlagerInfotrygdDto(stønadstyper = it.toSet()) }
}
