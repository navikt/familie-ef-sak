package no.nav.familie.ef.sak.samværsavtale

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.brev.BrevsignaturService
import no.nav.familie.ef.sak.brev.dto.Avsnitt
import no.nav.familie.ef.sak.brev.dto.FritekstBrevMedSignaturRequest
import no.nav.familie.ef.sak.brev.dto.FritekstBrevRequestDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.journalføring.JournalpostClient
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.samværsavtale.SamværsavtaleHelper.lagAvsnitt
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsavtale
import no.nav.familie.ef.sak.samværsavtale.domain.SamværsukeWrapper
import no.nav.familie.ef.sak.samværsavtale.dto.JournalførBeregnetSamværRequest
import no.nav.familie.ef.sak.samværsavtale.dto.SamværsavtaleDto
import no.nav.familie.ef.sak.samværsavtale.dto.tilDomene
import no.nav.familie.ef.sak.vedtak.domain.VedtakErUtenBeslutter
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
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
    val fagsakService: FagsakService,
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
    fun opprettEllerErstattSamværsavtale(
        request: SamværsavtaleDto,
    ): Samværsavtale {
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
    fun gjenbrukSamværsavtaler(
        behandlingSomSkalOppdateresId: UUID,
        behandlingForGjenbrukId: UUID,
        metadata: HovedregelMetadata,
    ) {
        val samværsavtalerForGjenbruk =
            hentSamværsavtalerForBehandling(behandlingForGjenbrukId).associateBy { it.behandlingBarnId }
        val barnPåForrigeBehandling = barnService.finnBarnPåBehandling(behandlingForGjenbrukId)
        val barnIdMap = byggBarnMapFraTidligereTilNyId(barnPåForrigeBehandling, metadata.barn)

        val nyeSamværsavtaler =
            barnIdMap.mapNotNull { (forrigeBehandlingBarnId, nåværendeBehandlingBarn) ->
                samværsavtalerForGjenbruk.get(forrigeBehandlingBarnId)?.copy(
                    id = UUID.randomUUID(),
                    behandlingId = behandlingSomSkalOppdateresId,
                    behandlingBarnId = nåværendeBehandlingBarn.id,
                )
            }

        nyeSamværsavtaler.forEach { nySamværsavtale ->
            val lagretSamværsavtale = hentSamværsavtaleEllerNull(nySamværsavtale.behandlingId, nySamværsavtale.behandlingBarnId)

            if (lagretSamværsavtale == null) {
                samværsavtaleRepository.insert(nySamværsavtale)
            } else {
                samværsavtaleRepository.update(lagretSamværsavtale.copy(uker = nySamværsavtale.uker))
            }
        }
    }

    @Transactional
    fun gjenbrukSamværsavtale(
        behandlingSomSkalOppdateresId: UUID,
        behandlingForGjenbrukId: UUID,
        barnPåBehandlingSomSkalOppdateres: List<BehandlingBarn>,
        vilkårsvurderingSomSkalOppdateres: Vilkårsvurdering,
    ) {
        validerBehandlingerForGjenbruk(behandlingSomSkalOppdateresId, behandlingForGjenbrukId)
        validerVilkårsvurderingForGjenbruk(vilkårsvurderingSomSkalOppdateres)
        val barnPåBehandlingForGjenbruk = barnService.finnBarnPåBehandling(behandlingForGjenbrukId)
        val barnIdMap = byggBarnMapFraTidligereTilNyId(barnPåBehandlingForGjenbruk, barnPåBehandlingSomSkalOppdateres)
        val tilsvarendeBarnPåBehandlingForGjenbruk = barnIdMap.entries.find { it.value.id == vilkårsvurderingSomSkalOppdateres.barnId }?.key ?: error("Fant ikke barn på tidligere vilkårsvurdering")
        val samværsavtaleForGjenbruk = hentSamværsavtalerForBehandling(behandlingForGjenbrukId).find { it.behandlingBarnId == tilsvarendeBarnPåBehandlingForGjenbruk }

        if (samværsavtaleForGjenbruk == null) {
            samværsavtaleRepository.deleteByBehandlingIdAndBehandlingBarnId(behandlingSomSkalOppdateresId, vilkårsvurderingSomSkalOppdateres.barnId ?: error("Fant ikke barn på tidligere vilkårsvurdering"))
        } else {
            opprettEllerErstattSamværsavtale(
                request =
                    SamværsavtaleDto(
                        behandlingId = behandlingSomSkalOppdateresId,
                        behandlingBarnId = vilkårsvurderingSomSkalOppdateres.barnId ?: error("Mangler behandlingBarnId for gjenbruk av samværsavtale"),
                        uker = samværsavtaleForGjenbruk.uker.uker,
                    ),
            )
        }
    }

    fun journalførBeregnetSamvær(request: JournalførBeregnetSamværRequest): String {
        val eksternFagsakId = fagsakService.finnFagsaker(setOf(request.personIdent)).firstOrNull()?.eksternId
        validerjournalføringRequest(request, eksternFagsakId)

        val fritekstBrevRequest = lagFritekstBrevMedSignaturRequest(request)
        val dokument = brevClient.genererFritekstBrev(fritekstBrevRequest)

        val saksbehandler = SikkerhetContext.hentSaksbehandler()
        val arkiverDokumentRequest = lagArkiverDokumentRequest(dokument, request.personIdent, eksternFagsakId)
        val respons = journalpostClient.arkiverDokument(arkiverDokumentRequest, saksbehandler)

        return respons.journalpostId
    }

    private fun lagFritekstBrevMedSignaturRequest(request: JournalførBeregnetSamværRequest): FritekstBrevMedSignaturRequest {
        val fritekstBrevRequest =
            FritekstBrevRequestDto(
                overskrift = "Samværsberegning",
                personIdent = request.personIdent,
                navn = personopplysningerService.hentGjeldeneNavn(listOf(request.personIdent)).getValue(request.personIdent),
                avsnitt =
                    request.uker.mapIndexed { ukeIndex, samværsuke ->
                        lagAvsnitt(ukeIndex + 1, samværsuke)
                    } +
                        Avsnitt(deloverskrift = "Oppsummering", innhold = request.oppsummering) +
                        Avsnitt(
                            deloverskrift = "Notat",
                            innhold = request.notat,
                        ),
            )
        val signatur = brevsignaturService.lagSaksbehandlerSignatur(request.personIdent, VedtakErUtenBeslutter(true))
        return FritekstBrevMedSignaturRequest(brevFraSaksbehandler = fritekstBrevRequest, saksbehandlersignatur = signatur.navn, enhet = signatur.enhet, erSamværsberegning = true)
    }

    private fun lagArkiverDokumentRequest(
        pdf: ByteArray,
        personIdent: String,
        eksternFagsakId: Long?,
    ): ArkiverDokumentRequest {
        val dokument =
            Dokument(
                dokument = pdf,
                filtype = Filtype.PDFA,
                tittel = "Samværsberegning",
                dokumenttype = Dokumenttype.BEREGNET_SAMVÆR_NOTAT,
            )
        val journalførendeEnhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(personIdent)

        return ArkiverDokumentRequest(
            fnr = personIdent,
            forsøkFerdigstill = true,
            hoveddokumentvarianter = listOf(dokument),
            vedleggsdokumenter = listOf(),
            journalførendeEnhet = journalførendeEnhet,
            fagsakId = eksternFagsakId.toString(),
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

    private fun validerjournalføringRequest(
        request: JournalførBeregnetSamværRequest,
        eksternFagsakId: Long?,
    ) {
        brukerfeilHvis(request.notat.isEmpty()) {
            "Kan ikke journalføre samværsavtale uten notat"
        }
        brukerfeilHvis(request.oppsummering.isEmpty()) {
            "Kan ikke journalføre samværsavtale uten oppsumering"
        }
        brukerfeilHvis(eksternFagsakId == null) {
            "Kan ikke journalføre samværsavtale på person uten fagsak"
        }
    }

    private fun validerBehandlingerForGjenbruk(
        behandlingSomSkalOppdateresId: UUID,
        behandlingForGjenbrukId: UUID,
    ) {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingSomSkalOppdateresId)
        brukerfeilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Behandlingen er låst og samværsavtale kan ikke gjenbrukes på behandling med id=$behandlingSomSkalOppdateresId"
        }
        brukerfeilHvis(!tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingSomSkalOppdateresId)) {
            "Behandling med id=$behandlingSomSkalOppdateresId eies av noen andre og samværsavtale kan derfor ikke oppdateres av deg"
        }

        val fagsak: Fagsak = fagsakService.hentFagsakForBehandling(behandlingSomSkalOppdateresId)
        val behandlingerForGjenbruk: List<Behandling> =
            behandlingService.hentBehandlingerForGjenbrukAvVilkårOgSamværsavtaler(fagsak.fagsakPersonId)

        if (behandlingerForGjenbruk.isEmpty()) {
            throw Feil("Fant ingen tidligere behandlinger som kan benyttes til gjenbruk av samværsavtale for behandling med id=$behandlingSomSkalOppdateresId")
        }
        if (!behandlingerForGjenbruk.map { it.id }.contains(behandlingForGjenbrukId)) {
            throw Feil("Behandling med id=$behandlingForGjenbrukId kan ikke benyttes til gjenbruk av samværsavtale for behandling med id=$behandlingSomSkalOppdateresId")
        }
    }

    private fun validerVilkårsvurderingForGjenbruk(vilkårsvurdering: Vilkårsvurdering) {
        if (vilkårsvurdering.type !== VilkårType.ALENEOMSORG) {
            throw Feil("Kan ikke gjenbruke samværsavtale for et vilkår som ikke er av type ALENEOMSROG. behandlingId=${vilkårsvurdering.behandlingId}")
        }
        if (vilkårsvurdering.barnId == null) {
            throw Feil("Kan ikke gjenbruke samværsavtale for et vilkår som ikke har et barn. behandlingId=${vilkårsvurdering.behandlingId}")
        }
    }
}
