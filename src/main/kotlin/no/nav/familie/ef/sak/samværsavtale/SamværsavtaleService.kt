package no.nav.familie.ef.sak.samværsavtale

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.brev.BrevsignaturService
import no.nav.familie.ef.sak.brev.dto.Avsnitt
import no.nav.familie.ef.sak.brev.dto.FritekstBrevRequestDto
import no.nav.familie.ef.sak.brev.dto.FritekstBrevRequestMedSignatur
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.journalføring.JournalpostClient
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.samværsavtale.SamværsavtaleHelper.lagAvsnittFritekstbrev
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsavtale
import no.nav.familie.ef.sak.samværsavtale.domain.SamværsukeWrapper
import no.nav.familie.ef.sak.samværsavtale.dto.JournalførBeregnetSamværRequest
import no.nav.familie.ef.sak.samværsavtale.dto.SamværsavtaleDto
import no.nav.familie.ef.sak.samværsavtale.dto.tilDomene
import no.nav.familie.ef.sak.vedtak.domain.VedtakErUtenBeslutter
import no.nav.familie.ef.sak.vilkår.VurderingService.Companion.byggBarnMapFraTidligereTilNyId
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.kontrakter.felles.dokarkiv.Dokumenttype
import no.nav.familie.kontrakter.felles.dokarkiv.v2.ArkiverDokumentRequest
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Dokument
import no.nav.familie.kontrakter.felles.dokarkiv.v2.Filtype
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SamværsavtaleService(
    val samværsavtaleRepository: SamværsavtaleRepository,
    val behandlingService: BehandlingService,
    val tilordnetRessursService: TilordnetRessursService,
    val barnService: BarnService,
    val personopplysningerService: PersonopplysningerService,
    val journalpostClient: JournalpostClient,
    val brevClient: BrevClient,
    val arbeidsfordelingService: ArbeidsfordelingService,
    val brevsignaturService: BrevsignaturService,
) {
    fun hentSamværsavtalerForBehandling(behandlingId: UUID) = samværsavtaleRepository.findByBehandlingId(behandlingId)

    @Transactional
    fun opprettEllerErstattSamværsavtale(request: SamværsavtaleDto): Samværsavtale {
        val behandling = behandlingService.hentBehandling(request.behandlingId)
        val behandlingBarn = barnService.finnBarnPåBehandling(request.behandlingId)

        validerBehandling(behandling)
        validerRequest(request, behandlingBarn)

        val lagretSamværsavtale = hentSamværsavtaleEllerNull(request.behandlingId, request.behandlingBarnId)

        return if (lagretSamværsavtale == null) {
            samværsavtaleRepository.insert(request.tilDomene())
        } else {
            samværsavtaleRepository.update(lagretSamværsavtale.copy(uker = SamværsukeWrapper(uker = request.uker)))
        }
    }

    @Transactional
    fun slettSamværsavtale(
        behandlingId: UUID,
        behandlingBarnId: UUID,
    ) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        validerBehandling(behandling)
        samværsavtaleRepository.deleteByBehandlingIdAndBehandlingBarnId(behandlingId, behandlingBarnId)
    }

    @Transactional
    fun kopierSamværsavtalerTilNyBehandling(
        eksisterendeBehandlingId: UUID,
        nyBehandlingId: UUID,
        metadata: HovedregelMetadata,
    ) {
        val eksisterendeSamværsavtaler =
            hentSamværsavtalerForBehandling(eksisterendeBehandlingId).associateBy { it.behandlingBarnId }
        val barnPåForrigeBehandling = barnService.finnBarnPåBehandling(eksisterendeBehandlingId)
        val barnIdMap = byggBarnMapFraTidligereTilNyId(barnPåForrigeBehandling, metadata.barn)

        val nyeSamværsavtaler =
            barnIdMap.mapNotNull { (forrigeBehandlingBarnId, nåværendeBehandlingBarn) ->
                eksisterendeSamværsavtaler.get(forrigeBehandlingBarnId)?.copy(
                    id = UUID.randomUUID(),
                    behandlingId = nyBehandlingId,
                    behandlingBarnId = nåværendeBehandlingBarn.id,
                )
            }

        samværsavtaleRepository.insertAll(nyeSamværsavtaler)
    }

    fun journalførBeregnetSamvær(request: JournalførBeregnetSamværRequest): String {
        validerjournalføringRequest(request)
        val fritekstBrevRequest = lagFritekstBrevRequestMedSignatur(request)
        val dokument = brevClient.genererFritekstBrev(fritekstBrevRequest)

        val saksbehandler = SikkerhetContext.hentSaksbehandler()
        val arkiverDokumentRequest = lagArkiverDokumentRequest(dokument, request.personIdent)
        val respons = journalpostClient.arkiverDokument(arkiverDokumentRequest, saksbehandler)

        return respons.journalpostId
    }

    private fun lagFritekstBrevRequestMedSignatur(request: JournalførBeregnetSamværRequest): FritekstBrevRequestMedSignatur {
        val fritekstBrevRequest =
            FritekstBrevRequestDto(
                overskrift = "Beregnet samvær",
                personIdent = request.personIdent,
                navn = personopplysningerService.hentGjeldeneNavn(listOf(request.personIdent)).getValue(request.personIdent),
                avsnitt =
                    request.uker.mapIndexed { ukeIndex, samværsuke ->
                        lagAvsnittFritekstbrev(ukeIndex + 1, samværsuke)
                    } + Avsnitt(
                        deloverskrift = "Notat",
                        innhold = request.notat,
                    ),
            )
        val signatur = brevsignaturService.lagSaksbehandlerSignatur(request.personIdent, VedtakErUtenBeslutter(true))
        return FritekstBrevRequestMedSignatur(brevFraSaksbehandler = fritekstBrevRequest, saksbehandlersignatur = signatur.navn, enhet = signatur.enhet)
    }

    private fun lagArkiverDokumentRequest(
        pdf: ByteArray,
        personIdent: String,
    ): ArkiverDokumentRequest {
        val dokument =
            Dokument(
                dokument = pdf,
                filtype = Filtype.PDFA,
                tittel = "Beregnet samvær",
                dokumenttype = Dokumenttype.BEREGNET_SAMVÆR_NOTAT,
            )
        val journalførendeEnhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(personIdent)

        return ArkiverDokumentRequest(
            fnr = personIdent,
            forsøkFerdigstill = true,
            hoveddokumentvarianter = listOf(dokument),
            vedleggsdokumenter = listOf(),
            journalførendeEnhet = journalførendeEnhet,
        )
    }

    private fun hentSamværsavtaleEllerNull(
        behandlingId: UUID,
        behandlingBarnId: UUID,
    ): Samværsavtale? = samværsavtaleRepository.findByBehandlingIdAndBehandlingBarnId(behandlingId, behandlingBarnId)

    private fun validerBehandling(behandling: Behandling) {
        brukerfeilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Behandlingen er låst for videre redigering"
        }
        brukerfeilHvis(!tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandling.id)) {
            "Behandlingen eies av en annen saksbehandler"
        }
    }

    private fun validerRequest(
        request: SamværsavtaleDto,
        behandlingBarn: List<BehandlingBarn>,
    ) {
        brukerfeilHvis(request.uker.isEmpty()) {
            "Kan ikke opprette en samværsavtale uten noen uker. BehandlingId=${request.behandlingId}"
        }
        brukerfeilHvis(
            request
                .mapTilSamværsandelerPerDag()
                .any { samværsandeler -> samværsandeler.size > samværsandeler.toSet().size },
        ) {
            "Kan ikke ha duplikate samværsandeler innenfor en og samme dag. BehandlingId=${request.behandlingId}"
        }
        brukerfeilHvis(request.summerTilSamværsandelerVerdiPerDag().any { it > 8 }) {
            "Kan ikke ha mer enn 8 samværsandeler per dag. BehandlingId=${request.behandlingId}"
        }
        brukerfeilHvis(!behandlingBarn.map { it.id }.contains(request.behandlingBarnId)) {
            "Kan ikke opprette en samværsavtale for et barn som ikke eksisterer på behandlingen. BehandlingId=${request.behandlingId}"
        }
    }

    private fun validerjournalføringRequest(request: JournalførBeregnetSamværRequest){
        brukerfeilHvis(request.notat.isEmpty()) {
            "Kan ikke journalføre samværsavtale uten notat"
        }

    }
}
