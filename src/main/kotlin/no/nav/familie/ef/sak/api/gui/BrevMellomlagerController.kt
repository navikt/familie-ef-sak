package no.nav.familie.ef.sak.api.gui

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.api.dto.MellomlagretBrevDto
import no.nav.familie.ef.sak.repository.domain.MellomlagretBrev
import no.nav.familie.ef.sak.service.MellomlagringBrevService
import no.nav.familie.ef.sak.service.TilgangService
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping(path = ["/api/brev/mellomlager/"])
@ProtectedWithClaims(issuer = "azuread")
class BrevMellomlagerController(private val tilgangService: TilgangService,
                                private val mellomlagringBrevService: MellomlagringBrevService) {

    @PostMapping("/{behandlingId}")
    fun mellomlagreBrevverdier(@PathVariable behandlingId: UUID,
                               @RequestBody mellomlagretBrev: MellomlagretBrevDto): Ressurs<UUID> {
        tilgangService.validerTilgangTilBehandling(behandlingId)

        return Ressurs.success(mellomlagringBrevService.mellomLagreBrev(MellomlagretBrev(behandlingId,
                                                                                         mellomlagretBrev.brevverdier,
                                                                                         mellomlagretBrev.brevmal,
                                                                                         SikkerhetContext.hentSaksbehandler(strict = true),
                                                                                         "1")))
    }


    @GetMapping("/{behandlingId}")
    fun hentMellomlagretBrevverdier(@PathVariable behandlingId: UUID): Ressurs<String?> {
        tilgangService.validerTilgangTilBehandling(behandlingId)

//        val data =
//                """[{"_ref":"079fe698-ed8d-46a8-8cf8-3c009ee3bc86","verdi":null},{"_ref":"084f8be5-286c-470e-9749-dd1aaa969b44","verdi":null},{"_ref":"0980eb62-6f0f-4019-aa78-98974aa8b519","verdi":null},{"_ref":"0dc38adf-727d-459b-82be-4fcbb2986050","verdi":null},{"_ref":"0dfaeb6f-d270-4649-b179-761eb4ac6e73","verdi":null},{"_ref":"1491cef3-9cee-4036-b732-e9cb46ed4ff5","verdi":null},{"_ref":"1a81e574-30ab-4782-8242-233915d985c3","verdi":null},{"_ref":"211b4b77-225b-4f55-8c7a-153c055c9773","verdi":null},{"_ref":"225023aa-b117-4b2d-9be1-23c0f5144711","verdi":null},{"_ref":"23068ba1-a94e-4dc4-9488-07d0ff45db72","verdi":null},{"_ref":"26bf695a-1dd2-4798-b372-3199f1531b37","verdi":null},{"_ref":"2cae76f0-f839-4268-b408-0f8bbebb3ec1","verdi":null},{"_ref":"2ecf5a87-ba8f-4727-b230-e8b8ef5f1ab8","verdi":null},{"_ref":"2fc82ccc-755e-494f-b2f4-a423d6acc972","verdi":null},{"_ref":"314e37ef-bbbb-437e-bf8a-b15f66ea2a45","verdi":null},{"_ref":"33a0c413-a0e3-49c9-b542-850374d38497","verdi":null},{"_ref":"346ff1c6-a085-40e0-9217-4da318b25970","verdi":null},{"_ref":"35447d32-1cd0-4774-a8ed-9a1a461b499f","verdi":null},{"_ref":"51f7ff3a-280d-48f9-b3cb-507102c2506b","verdi":null},{"_ref":"522217b4-a2f9-497c-beaf-223fd5abd87f","verdi":null},{"_ref":"52364852-ad9c-4af0-98f4-e5b7fa17d383","verdi":null},{"_ref":"53cae7f4-eae2-4d81-b520-c052664feb4d","verdi":null},{"_ref":"53df6f4e-dba5-4218-aec9-9b040d036a3e","verdi":null},{"_ref":"5ea8fdae-d156-43b4-95b8-40eec418d927","verdi":null},{"_ref":"5ebf11f8-98b5-4501-b339-99a2d2b3fd41","verdi":null},{"_ref":"62a39a12-9372-49a8-9b7b-347e6e4d11ee","verdi":null},{"_ref":"64deafee-1edf-4763-a049-d699ef8de708","verdi":null},{"_ref":"6d89fa19-d5c1-4e16-b448-f0faf531242b","verdi":null},{"_ref":"723e2735-04cf-4adc-b323-1c7f2e8588fb","verdi":null},{"_ref":"7e7a12e2-96d1-43fd-b781-680c14edb5b8","verdi":null},{"_ref":"8b0461eb-a01e-41c6-860e-c90937afb007","verdi":null},{"_ref":"901c9759-0166-4914-9930-c563e7ff03a9","verdi":null},{"_ref":"96fda846-f517-466f-9dce-d594a8994787","verdi":null},{"_ref":"992f87d9-0734-4e34-bcff-fcd79c3b7f30","verdi":null},{"_ref":"994c29b3-5dca-45b5-953d-869b0f3c532a","verdi":null},{"_ref":"ab6053e0-7aeb-4610-98b6-cea27a6c39f3","verdi":null},{"_ref":"aeb93d20-1d63-47fe-8ae5-57af1a66c926","verdi":null},{"_ref":"bd6517bb-a31d-4aaf-9586-0f09ab02ae3c","verdi":null},{"_ref":"bd69f758-6f5b-449c-b3a1-13b7f72d65b5","verdi":null},{"_ref":"c1d40960-5470-4959-8ecf-e13a5fe81935","verdi":null},{"_ref":"c5c4c99f-65cf-45ae-a19c-89522d76d8d7","verdi":null},{"_ref":"c6817641-b4f9-43ed-a822-f930c1bbccff","verdi":null},{"_ref":"cf95b423-9e6b-4adb-a9a2-783cd6961c86","verdi":null},{"_ref":"d3ce63d0-28b1-4076-afb7-3095cb964ca8","verdi":null},{"_ref":"d604bb29-cfba-4bcb-839f-836df3c36351","verdi":null},{"_ref":"d6e05dd5-f9ba-4c8d-b49f-71d49eb3e2c9","verdi":null},{"_ref":"d96a5849-4da4-487c-af44-68f3759028ae","verdi":"ÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆÆ"},{"_ref":"db3997ac-a19b-4cfb-874c-c4d5a6c0b9a0","verdi":null},{"_ref":"dead160f-239c-49f8-b699-56bbc9d0f75c","verdi":null},{"_ref":"e22a3397-e6f9-4153-b15b-50a03f945a52","verdi":null},{"_ref":"e367a466-af4f-44e2-a4e1-3a1002cbcebc","verdi":null},{"_ref":"e458fa6c-8942-4bdc-a0f8-890c2c0e679f","verdi":null},{"_ref":"e494bd4a-31c7-4601-873e-d277401f6795","verdi":null},{"_ref":"e52b179d-928f-4703-adc4-71dc35c34993","verdi":"ÅÅÅÅÅÅÅÅÅÅÅÅÅÅÅÅÅÅÅÅÅÅÅÅÅÅÅÅÅ"},{"_ref":"e7df971b-cea6-427f-9031-c9ba19e72a02","verdi":null},{"_ref":"e9a9bd16-24ca-430b-a867-aa25bfdc89ff","verdi":null},{"_ref":"f8b6f567-e024-4604-b9e2-08bb408add61","verdi":null},{"_ref":"fd0b4446-1da3-456d-9fa0-67eb73f9e022","verdi":null},{"_ref":"fd7f571b-5a13-48c1-b86d-fe4fe18a7ac9","verdi":null},{"_ref":"fef9eaf0-df7e-4e79-bc67-3dea9d8b0c7b","verdi":null}]"""
//        val jsonData: JsonNode = objectMapper.readValue(data)

        return Ressurs.success(mellomlagringBrevService.hentMellomlagretBrev(behandlingId)?.brevverdier)
    }


}
