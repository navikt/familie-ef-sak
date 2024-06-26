package no.nav.familie.ef.sak.klage

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.klage.dto.KlagebehandlingerDto
import no.nav.familie.ef.sak.klage.dto.OpprettKlageDto
import no.nav.familie.ef.sak.klage.dto.ÅpneKlagerInfotrygdDto
import no.nav.familie.kontrakter.felles.klage.BehandlingEventType
import no.nav.familie.kontrakter.felles.klage.BehandlingResultat
import no.nav.familie.kontrakter.felles.klage.Fagsystem
import no.nav.familie.kontrakter.felles.klage.KlagebehandlingDto
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

    fun opprettKlage(
        fagsakId: UUID,
        opprettKlageDto: OpprettKlageDto,
    ) {
        val klageMottatt = opprettKlageDto.mottattDato
        brukerfeilHvis(klageMottatt.isAfter(LocalDate.now())) {
            "Kan ikke opprette klage med krav mottatt frem i tid for fagsak=$fagsakId"
        }
        opprettKlage(fagsakService.hentFagsak(fagsakId), opprettKlageDto.mottattDato, opprettKlageDto.klageGjelderTilbakekreving)
    }

    fun opprettKlage(
        fagsak: Fagsak,
        klageMottatt: LocalDate,
        klageGjelderTilbakekreving: Boolean,
    ) {
        val aktivIdent = fagsak.hentAktivIdent()
        val enhetId = arbeidsfordelingService.hentNavEnhet(aktivIdent)?.enhetId
        brukerfeilHvis(enhetId == null) {
            "Finner ikke behandlende enhet for personen"
        }
        klageClient.opprettKlage(
            OpprettKlagebehandlingRequest(
                ident = aktivIdent,
                stønadstype = Stønadstype.fraEfStønadstype(fagsak.stønadstype),
                eksternFagsakId = fagsak.eksternId.toString(),
                fagsystem = Fagsystem.EF,
                klageMottatt = klageMottatt,
                behandlendeEnhet = enhetId,
                klageGjelderTilbakekreving = klageGjelderTilbakekreving,
            ),
        )
    }

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
