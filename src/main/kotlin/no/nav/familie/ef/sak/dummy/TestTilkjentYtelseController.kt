package no.nav.familie.ef.sak.dummy

import no.nav.familie.ef.sak.api.dto.AndelTilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseDTO
import no.nav.familie.ef.sak.api.dto.TilkjentYtelseTestDTO
import no.nav.familie.ef.sak.integration.ØkonomiKlient
import no.nav.familie.ef.sak.mapper.tilTilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelse
import no.nav.familie.ef.sak.repository.domain.TilkjentYtelseMedMetaData
import no.nav.familie.ef.sak.repository.domain.YtelseType
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.TilkjentYtelseService
import no.nav.familie.ef.sak.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.økonomi.UtbetalingsoppdragGenerator
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping(path = ["/api/test/"])
@ProtectedWithClaims(issuer = "azuread")
class TestTilkjentYtelseController(private val økonomiKlient: ØkonomiKlient,
                                   private val tilkjentYtelseService: TilkjentYtelseService,
                                   private val behandlingService: BehandlingService,
                                   private val fagsakService: FagsakService,
                                   private val testTilkjentYtelseService: TestTilkjentYtelseService) {

    @PostMapping("/send-til-oppdrag")
    fun testMotOppdrag(@RequestBody tilkjentYtelseTestDTO: TilkjentYtelseTestDTO): Ressurs<TilkjentYtelse> {
        val behandling = behandlingService.opprettBehandling(behandlingType = BehandlingType.FØRSTEGANGSBEHANDLING, fagsakId = UUID.randomUUID())

        val nyTilkjentYtelse = tilkjentYtelseTestDTO.nyTilkjentYtelse
        val forrigeTilkjentYtelse = tilkjentYtelseTestDTO.forrigeTilkjentYtelse

        val nyTilkjentYtelseMedEksternId = TilkjentYtelseMedMetaData(nyTilkjentYtelse, eksternBehandlingId = behandling.eksternId.id);
        val tilkjentYtelseMedUtbetalingsoppdrag = UtbetalingsoppdragGenerator.lagTilkjentYtelseMedUtbetalingsoppdrag(
                nyTilkjentYtelseMedMetaData = nyTilkjentYtelseMedEksternId,
                forrigeTilkjentYtelse = forrigeTilkjentYtelse)

        økonomiKlient.iverksettOppdrag(tilkjentYtelseMedUtbetalingsoppdrag.utbetalingsoppdrag!!)
        return Ressurs.success(tilkjentYtelseMedUtbetalingsoppdrag)
    }

    @PostMapping("/dummy-med-behandling")
    fun testMotOppdragMedFagsakOgBehandling(@RequestBody dummyIverksettingDTO: DummyIverksettingDTO): Ressurs<UUID> {
        val fagsak = fagsakService.hentEllerOpprettFagsak(dummyIverksettingDTO.personIdent, dummyIverksettingDTO.stønadstype)
        val behandling = behandlingService.opprettBehandling(behandlingType = dummyIverksettingDTO.behandlingType, fagsakId = fagsak.id)
        val tilkjentYtelseId = tilkjentYtelseService.opprettTilkjentYtelse(tilkjentYtelseDTO = TilkjentYtelseDTO(
                søker = dummyIverksettingDTO.personIdent,
                saksnummer = "er dette en fagsak ??",
                behandlingId = behandling.id,
                andelerTilkjentYtelse = listOf(
                        AndelTilkjentYtelseDTO(
                                beløp = dummyIverksettingDTO.beløp,
                                type = YtelseType.OVERGANGSSTØNAD,
                                stønadFom = dummyIverksettingDTO.stønadFom,
                                stønadTom = dummyIverksettingDTO.stønadTom,
                                personIdent = dummyIverksettingDTO.personIdent))
        ))

        tilkjentYtelseService.iverksettUtbetalingsoppdrag(tilkjentYtelseId)

        return Ressurs.success(testTilkjentYtelseService.iverksettBehandling(dummyIverksettingDTO))
    }

    @GetMapping("/dummy")
    fun dummyTilkjentYtelse(@RequestParam(defaultValue = "12345678911") fnr: String): Ressurs<TilkjentYtelse> {
        val søker = fnr
        val saksbehandler = SikkerhetContext.hentSaksbehandler()
        val andelTilkjentYtelseDto = AndelTilkjentYtelseDTO(personIdent = søker,
                beløp = 1000,
                stønadFom = LocalDate.now(),
                stønadTom = LocalDate.now(),
                type = YtelseType.OVERGANGSSTØNAD)
        val tilkjentYtelseDto = TilkjentYtelseDTO(søker = søker,
                saksnummer = "12345",
                behandlingId = UUID.randomUUID(),
                andelerTilkjentYtelse = listOf(andelTilkjentYtelseDto, andelTilkjentYtelseDto))
        return Ressurs.success(tilkjentYtelseDto.tilTilkjentYtelse(saksbehandler = saksbehandler))
    }

}