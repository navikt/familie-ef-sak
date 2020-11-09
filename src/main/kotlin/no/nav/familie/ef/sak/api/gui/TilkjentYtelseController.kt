package no.nav.familie.ef.sak.api.gui

import no.nav.familie.ef.sak.api.dto.AndelTilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseTestDTO
import no.nav.familie.ef.sak.integration.ØkonomiKlient
import no.nav.familie.ef.sak.mapper.tilTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.ef.sak.økonomi.UtbetalingsoppdragGenerator
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.oppdrag.OppdragStatus
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping(path = ["/api/tilkjentytelse"])
@ProtectedWithClaims(issuer = "azuread")
class TilkjentYtelseController(private val tilkjentYtelseService: TilkjentYtelseService,
                               private val økonomiKlient: ØkonomiKlient,
                               private val behandlingService: BehandlingService,
                               val fagsakService: FagsakService) {

    @PostMapping
    fun opprettTilkjentYtelse(@RequestBody tilkjentYtelseDTO: TilkjentYtelseDTO): ResponseEntity<Long> {

        tilkjentYtelseDTO.valider()

        val tilkjentYtelseId = tilkjentYtelseService.opprettTilkjentYtelse(tilkjentYtelseDTO)

        val location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .pathSegment(tilkjentYtelseId.toString())
                .build().toUri()

        return ResponseEntity.created(location).build()
    }

    @GetMapping("{tilkjentYtelseId}")
    fun hentTilkjentYtelse(@PathVariable tilkjentYtelseId: UUID): ResponseEntity<TilkjentYtelseDTO> {
        val tilkjentYtelseDto = tilkjentYtelseService.hentTilkjentYtelseDto(tilkjentYtelseId)

        return ResponseEntity.ok(tilkjentYtelseDto)
    }

    @PutMapping("{tilkjentYtelseId}/utbetaling")
    fun sørgForUtbetaling(@PathVariable tilkjentYtelseId: UUID): HttpStatus {
        tilkjentYtelseService.iverksettUtbetalingsoppdrag(tilkjentYtelseId)

        return HttpStatus.ACCEPTED
    }

    @DeleteMapping("{tilkjentYtelseId}/utbetaling")
    fun opphørUtbetaling(@PathVariable tilkjentYtelseId: UUID): ResponseEntity<Long> {
        val opphørtTilkjentYtelseId = tilkjentYtelseService.opphørUtbetalingsoppdrag(tilkjentYtelseId)

        val location = ServletUriComponentsBuilder.fromCurrentRequestUri()
                .pathSegment(opphørtTilkjentYtelseId.toString())
                .build().toUri()

        return ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(location).build()
    }

    @GetMapping("{tilkjentYtelseId}/utbetaling")
    fun hentStatusUtbetaling(@PathVariable tilkjentYtelseId: UUID): ResponseEntity<OppdragStatus> {
        val status = tilkjentYtelseService.hentStatus(tilkjentYtelseId)

        return ResponseEntity.ok(status)
    }

    @PostMapping("/test-mot-oppdrag")
    fun testMotOppdrag(@RequestBody tilkjentYtelseTestDTO: TilkjentYtelseTestDTO): Ressurs<TilkjentYtelse> {
        val nyTilkjentYtelse = tilkjentYtelseTestDTO.nyTilkjentYtelse
        val forrigeTilkjentYtelse = tilkjentYtelseTestDTO.forrigeTilkjentYtelse

        val fagsakDto = fagsakService.hentEllerOpprettFagsak(tilkjentYtelseTestDTO.nyTilkjentYtelse.personident,
                                                             stønadstype = Stønadstype.OVERGANGSSTØNAD)
        val eksternFagsakId = fagsakService.hentEksternId(fagsakDto.id)

        val behandling =
                behandlingService.opprettBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                    fagsakId = fagsakDto.id)
        val nyTilkjentYtelseMedEksternId = TilkjentYtelseMedMetaData(tilkjentYtelse = nyTilkjentYtelse,
                                                                     eksternBehandlingId = behandling.eksternId.id,
                                                                     eksternFagsakId = eksternFagsakId)
        val tilkjentYtelseMedUtbetalingsoppdrag = UtbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
                nyTilkjentYtelseMedMetaData = nyTilkjentYtelseMedEksternId,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse)

        økonomiKlient.iverksettOppdrag(tilkjentYtelseMedUtbetalingsoppdrag.utbetalingsoppdrag!!)
        return Ressurs.success(tilkjentYtelseMedUtbetalingsoppdrag)
    }

    @GetMapping("/dummy")
    fun dummyTilkjentYtelse(): Ressurs<TilkjentYtelse> {
        val søker = "12345678911"
        val andelTilkjentYtelseDto = AndelTilkjentYtelseDTO(personIdent = søker,
                                                            beløp = 1000,
                                                            stønadFom = LocalDate.now(),
                                                            stønadTom = LocalDate.now(),
                                                            type = YtelseType.OVERGANGSSTØNAD)
        val tilkjentYtelseDto = TilkjentYtelseDTO(søker = søker,
                                                  behandlingId = UUID.randomUUID(),
                                                  andelerTilkjentYtelse = listOf(andelTilkjentYtelseDto, andelTilkjentYtelseDto))
        return Ressurs.success(tilkjentYtelseDto.tilTilkjentYtelse(saksbehandler = "VL"))
    }

}