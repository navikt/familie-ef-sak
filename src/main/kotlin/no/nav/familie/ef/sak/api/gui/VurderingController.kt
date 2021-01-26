package no.nav.familie.ef.sak.api.gui

import net.minidev.json.JSONObject
import no.nav.familie.ef.sak.api.dto.BrevRequest
import no.nav.familie.ef.sak.api.dto.InngangsvilkårDto
import no.nav.familie.ef.sak.api.dto.VilkårsvurderingDto
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.ef.sak.service.VurderingService
import no.nav.familie.ef.sak.service.steg.StegService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.converter.ByteArrayHttpMessageConverter
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestTemplate
import java.util.*


@RestController
@RequestMapping(path = ["/api/vurdering"], produces = [MediaType.APPLICATION_JSON_VALUE])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class VurderingController(private val vurderingService: VurderingService,
                          private val stegService: StegService,
                          private val behandlingService: BehandlingService,
                          private val tilgangService: TilgangService) {

    @PostMapping("inngangsvilkar")
    fun oppdaterVurderingInngangsvilkår(@RequestBody vilkårsvurdering: VilkårsvurderingDto): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(vilkårsvurdering.behandlingId)
        return Ressurs.success(vurderingService.oppdaterVilkår(vilkårsvurdering))
    }

    @GetMapping("{behandlingId}/inngangsvilkar")
    fun getInngangsvilkår(@PathVariable behandlingId: UUID): Ressurs<InngangsvilkårDto> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        return Ressurs.success(vurderingService.hentInngangsvilkår(behandlingId))
    }

    @PostMapping("/{behandlingId}/inngangsvilkar/fullfor")
    fun fullførInngangsvilkår(@PathVariable behandlingId: UUID): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        // TODO; Trenger vi registrer opplysninger?
        val oppdatertBehandling = stegService.håndterRegistrerOpplysninger(behandling, null)
        return Ressurs.success(stegService.håndterInngangsvilkår(oppdatertBehandling).id)
    }
    @PostMapping("/{behandlingId}/overgangsstonad/fullfor")
    fun fullførStønadsvilkår(@PathVariable behandlingId: UUID): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)
        return Ressurs.success(stegService.håndterStønadsvilkår(behandling).id)
    }

    @PostMapping("/{behandlingId}/lagBrev")
    fun lagBrev(@PathVariable behandlingId: UUID, @RequestBody brevParams: BrevRequest): Ressurs<String> {
        tilgangService.validerTilgangTilBehandling(behandlingId)
        val behandling = behandlingService.hentBehandling(behandlingId)

        val uri = "http://localhost:8001/api/ba-brev/dokument/bokmaal/innhenteOpplysninger/pdf"

        //TODO: Autowire the RestTemplate in all the examples
        var restTemplate = RestTemplate()

        val byteArrayHttpMessageConverter = ByteArrayHttpMessageConverter()

        val supportedApplicationTypes: MutableList<MediaType> = ArrayList()
        val pdfApplication = MediaType("application", "pdf")
        supportedApplicationTypes.add(pdfApplication)

        byteArrayHttpMessageConverter.supportedMediaTypes = supportedApplicationTypes
        val messageConverters: MutableList<HttpMessageConverter<*>> = ArrayList()
        messageConverters.add(byteArrayHttpMessageConverter)
        restTemplate = RestTemplate()
        restTemplate.messageConverters = messageConverters

        val brev = "{\r\n    \"flettefelter\": {\r\n        \"navn\": [\r\n            \"Navn Navnesen\"\r\n        ],\r\n        \"fodselsnummer\": [\r\n            \"1123456789\"\r\n        ],\r\n        \"dato\": [\r\n            \"01.01.1986\"\r\n        ],\r\n        \"dokumentliste\": [\r\n            \"tekst 1\",\r\n            \"tekst 2\",\r\n            \"tekst 3\"\r\n        ]\r\n    },\r\n    \"delmalData\": {\r\n        \"signatur\": {\r\n            \"enhet\": [\r\n                \"Nav arbeid og ... - OSLO\"\r\n            ],\r\n            \"saksbehandler\": [\r\n                \"Saksbehandler Saksbehandlersen\"\r\n            ]\r\n        }\r\n    }\r\n}"

        val headers = HttpHeaders()
        headers.setContentType(MediaType.APPLICATION_JSON)
        val entity: HttpEntity<String> = HttpEntity<String>(brev, headers)

        println("BREEEEEEEV")

        val result: Any = restTemplate.postForObject(uri, entity, byteArrayHttpMessageConverter::class.java)
        val resultByteArr = result as ByteArray

        println("HALLJH IUEFEJOFJ EWOF JEOFEOIF JEWOF EOF EFIOEW JFOEW FOIE HFIUWF EWHFO EWHIOFEW IHOFE EIOWF WE")
        println(result)
        println(resultByteArr)
        println("HALLJH IUEFEJOFJ EWOF JEOFEOIF JEWOF EOF EFIOEW JFOEW FOIE HFIUWF EWHFO EWHIOFEW IHOFE EIOWF WE")

        // 1. lag brev request
        //
        // 2. send til brev-klient/familie-brev
        //
        // 3. motta generert brev
        //
        // 4. returner generert brev

        return  Ressurs.success("lagBrev-dummy")
    }

    private fun readFile(filnavn: String): String {
        return this::class.java.getResource("test/resources/json/$filnavn").readText()
    }
}
