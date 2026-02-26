package no.nav.familie.ef.sak.iverksett

import no.nav.familie.ef.sak.felles.domain.Fil
import no.nav.familie.ef.sak.infrastruktur.config.JsonMapperProvider
import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.kontrakter.ef.felles.BehandlingType
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.felles.Opplysningskilde
import no.nav.familie.kontrakter.ef.felles.RegelId
import no.nav.familie.kontrakter.ef.felles.Revurderingsårsak
import no.nav.familie.kontrakter.ef.felles.Vedtaksresultat
import no.nav.familie.kontrakter.ef.felles.VilkårType
import no.nav.familie.kontrakter.ef.felles.Vilkårsresultat
import no.nav.familie.kontrakter.ef.iverksett.AdressebeskyttelseGradering
import no.nav.familie.kontrakter.ef.iverksett.AktivitetType
import no.nav.familie.kontrakter.ef.iverksett.AndelTilkjentYtelseDto
import no.nav.familie.kontrakter.ef.iverksett.BarnDto
import no.nav.familie.kontrakter.ef.iverksett.BehandlingKategori
import no.nav.familie.kontrakter.ef.iverksett.BehandlingsdetaljerDto
import no.nav.familie.kontrakter.ef.iverksett.Brevmottaker
import no.nav.familie.kontrakter.ef.iverksett.DelvilkårsvurderingDto
import no.nav.familie.kontrakter.ef.iverksett.FagsakdetaljerDto
import no.nav.familie.kontrakter.ef.iverksett.Grunnbeløp
import no.nav.familie.kontrakter.ef.iverksett.IverksettOvergangsstønadDto
import no.nav.familie.kontrakter.ef.iverksett.SvarId
import no.nav.familie.kontrakter.ef.iverksett.SøkerDto
import no.nav.familie.kontrakter.ef.iverksett.TilkjentYtelseDto
import no.nav.familie.kontrakter.ef.iverksett.VedtaksdetaljerOvergangsstønadDto
import no.nav.familie.kontrakter.ef.iverksett.VedtaksperiodeOvergangsstønadDto
import no.nav.familie.kontrakter.ef.iverksett.VedtaksperiodeType
import no.nav.familie.kontrakter.ef.iverksett.VilkårsvurderingDto
import no.nav.familie.kontrakter.ef.iverksett.VurderingDto
import no.nav.familie.kontrakter.ef.iverksett.ÅrsakRevurderingDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class IverksettSerialiseringTest {
    private val jsonMapper = JsonMapperProvider.jsonMapper

    @Test
    fun `serialisering og deserialisering av IverksettOvergangsstønadDto med alle felter bevarer data`() {
        val original = lagKomplettIverksettDto()

        val json = jsonMapper.writeValueAsString(original)
        val deserialisert: IverksettOvergangsstønadDto = jsonMapper.readValue(json)

        assertThat(deserialisert).usingRecursiveComparison().isEqualTo(original)
    }

    @Test
    fun `serialisering av IverksettMedBrevRequest bevarer alle felter inkludert årsakRevurdering`() {
        val iverksettDto = lagKomplettIverksettDto()
        val fil = Fil("test-pdf-innhold".toByteArray())
        val request = IverksettMedBrevRequest(iverksettDto, fil.bytes)

        val json = jsonMapper.writeValueAsString(request)

        // Verifiser at alle viktige felter er med i JSON
        assertThat(json).contains("årsakRevurdering")
        assertThat(json).contains("opplysningskilde")
        assertThat(json).contains("MELDING_MODIA")
        assertThat(json).contains("ENDRING_INNTEKT")
        assertThat(json).contains("iverksettDto")
        assertThat(json).contains("fil")

        // Verifiser at behandlingsdetaljer er med
        assertThat(json).contains("behandlingId")
        assertThat(json).contains("forrigeBehandlingId")
        assertThat(json).contains("kravMottatt")

        // Verifiser at vedtaksdetaljer er med
        assertThat(json).contains("vedtakstidspunkt")
        assertThat(json).contains("tilkjentYtelse")
        assertThat(json).contains("vedtaksperioder")

        // Verifiser at søkerdata er med
        assertThat(json).contains("personIdent")
        assertThat(json).contains("tilhørendeEnhet")

        // Verifiser at fil er base64-encoded (inneholder ikke raw bytes)
        val filBase64 =
            java.util.Base64
                .getEncoder()
                .encodeToString(fil.bytes)
        assertThat(json).contains(filBase64)
    }

    @Test
    fun `serialisering bevarer alle felter i behandlingsdetaljer`() {
        val original = lagKomplettIverksettDto()

        val json = jsonMapper.writeValueAsString(original)
        val deserialisert: IverksettOvergangsstønadDto = jsonMapper.readValue(json)

        val originalBehandling = original.behandling
        val deserialiserBehandling = deserialisert.behandling

        assertThat(deserialiserBehandling.behandlingId).isEqualTo(originalBehandling.behandlingId)
        assertThat(deserialiserBehandling.forrigeBehandlingId).isEqualTo(originalBehandling.forrigeBehandlingId)
        assertThat(deserialiserBehandling.forrigeBehandlingEksternId).isEqualTo(originalBehandling.forrigeBehandlingEksternId)
        assertThat(deserialiserBehandling.eksternId).isEqualTo(originalBehandling.eksternId)
        assertThat(deserialiserBehandling.behandlingType).isEqualTo(originalBehandling.behandlingType)
        assertThat(deserialiserBehandling.behandlingÅrsak).isEqualTo(originalBehandling.behandlingÅrsak)
        assertThat(deserialiserBehandling.kravMottatt).isEqualTo(originalBehandling.kravMottatt)
        assertThat(deserialiserBehandling.kategori).isEqualTo(originalBehandling.kategori)
        assertThat(deserialiserBehandling.aktivitetspliktInntrefferDato).isEqualTo(originalBehandling.aktivitetspliktInntrefferDato)
        assertThat(deserialiserBehandling.årsakRevurdering).isEqualTo(originalBehandling.årsakRevurdering)
        assertThat(deserialiserBehandling.vilkårsvurderinger).hasSize(originalBehandling.vilkårsvurderinger.size)
    }

    @Test
    fun `serialisering bevarer alle felter i vedtaksdetaljer`() {
        val original = lagKomplettIverksettDto()

        val json = jsonMapper.writeValueAsString(original)
        val deserialisert: IverksettOvergangsstønadDto = jsonMapper.readValue(json)

        val originalVedtak = original.vedtak
        val deserialiserVedtak = deserialisert.vedtak

        assertThat(deserialiserVedtak.resultat).isEqualTo(originalVedtak.resultat)
        assertThat(deserialiserVedtak.vedtakstidspunkt).isEqualTo(originalVedtak.vedtakstidspunkt)
        assertThat(deserialiserVedtak.saksbehandlerId).isEqualTo(originalVedtak.saksbehandlerId)
        assertThat(deserialiserVedtak.beslutterId).isEqualTo(originalVedtak.beslutterId)
        assertThat(deserialiserVedtak.opphørÅrsak).isEqualTo(originalVedtak.opphørÅrsak)
        assertThat(deserialiserVedtak.avslagÅrsak).isEqualTo(originalVedtak.avslagÅrsak)
        assertThat(deserialiserVedtak.vedtaksperioder).hasSize(originalVedtak.vedtaksperioder.size)
        assertThat(deserialiserVedtak.tilkjentYtelse).isNotNull
        assertThat(deserialiserVedtak.brevmottakere).hasSize(originalVedtak.brevmottakere.size)
        assertThat(deserialiserVedtak.grunnbeløp).isEqualTo(originalVedtak.grunnbeløp)
    }

    @Test
    fun `serialisering bevarer søkerdata inkludert barn`() {
        val original = lagKomplettIverksettDto()

        val json = jsonMapper.writeValueAsString(original)
        val deserialisert: IverksettOvergangsstønadDto = jsonMapper.readValue(json)

        val originalSøker = original.søker
        val deserialiserSøker = deserialisert.søker

        assertThat(deserialiserSøker.personIdent).isEqualTo(originalSøker.personIdent)
        assertThat(deserialiserSøker.tilhørendeEnhet).isEqualTo(originalSøker.tilhørendeEnhet)
        assertThat(deserialiserSøker.adressebeskyttelse).isEqualTo(originalSøker.adressebeskyttelse)
        assertThat(deserialiserSøker.barn).hasSize(originalSøker.barn.size)
        assertThat(deserialiserSøker.barn.first().personIdent).isEqualTo(originalSøker.barn.first().personIdent)
        assertThat(deserialiserSøker.barn.first().termindato).isEqualTo(originalSøker.barn.first().termindato)
    }

    @Test
    fun `serialisering bevarer vilkårsvurderinger med delvilkår`() {
        val original = lagKomplettIverksettDto()

        val json = jsonMapper.writeValueAsString(original)
        val deserialisert: IverksettOvergangsstønadDto = jsonMapper.readValue(json)

        val originalVilkår = original.behandling.vilkårsvurderinger.first()
        val deserialiserVilkår = deserialisert.behandling.vilkårsvurderinger.first()

        assertThat(deserialiserVilkår.vilkårType).isEqualTo(originalVilkår.vilkårType)
        assertThat(deserialiserVilkår.resultat).isEqualTo(originalVilkår.resultat)
        assertThat(deserialiserVilkår.delvilkårsvurderinger).hasSize(originalVilkår.delvilkårsvurderinger.size)

        val originalDelvilkår = originalVilkår.delvilkårsvurderinger.first()
        val deserialiserDelvilkår = deserialiserVilkår.delvilkårsvurderinger.first()
        assertThat(deserialiserDelvilkår.resultat).isEqualTo(originalDelvilkår.resultat)
        assertThat(deserialiserDelvilkår.vurderinger).hasSize(originalDelvilkår.vurderinger.size)
    }

    private fun lagKomplettIverksettDto(): IverksettOvergangsstønadDto {
        val behandlingId = UUID.randomUUID()
        val fagsakId = UUID.randomUUID()
        val forrigeBehandlingId = UUID.randomUUID()
        val vedtakstidspunkt = LocalDateTime.of(2024, 1, 15, 10, 30, 0)

        return IverksettOvergangsstønadDto(
            fagsak =
                FagsakdetaljerDto(
                    fagsakId = fagsakId,
                    eksternId = 12345L,
                    stønadstype = StønadType.OVERGANGSSTØNAD,
                ),
            behandling =
                BehandlingsdetaljerDto(
                    behandlingId = behandlingId,
                    forrigeBehandlingId = forrigeBehandlingId,
                    forrigeBehandlingEksternId = 54321L,
                    eksternId = 67890L,
                    behandlingType = BehandlingType.REVURDERING,
                    behandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
                    vilkårsvurderinger =
                        listOf(
                            VilkårsvurderingDto(
                                vilkårType = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                                resultat = Vilkårsresultat.OPPFYLT,
                                delvilkårsvurderinger =
                                    listOf(
                                        DelvilkårsvurderingDto(
                                            resultat = Vilkårsresultat.OPPFYLT,
                                            vurderinger =
                                                listOf(
                                                    VurderingDto(
                                                        regelId = RegelId.SØKER_MEDLEM_I_FOLKETRYGDEN,
                                                        svar = SvarId.JA,
                                                        begrunnelse = "Søker er medlem i folketrygden",
                                                    ),
                                                ),
                                        ),
                                    ),
                            ),
                            VilkårsvurderingDto(
                                vilkårType = VilkårType.LOVLIG_OPPHOLD,
                                resultat = Vilkårsresultat.OPPFYLT,
                                delvilkårsvurderinger = emptyList(),
                            ),
                        ),
                    aktivitetspliktInntrefferDato = LocalDate.of(2024, 6, 1),
                    kravMottatt = LocalDate.of(2024, 1, 10),
                    årsakRevurdering =
                        ÅrsakRevurderingDto(
                            opplysningskilde = Opplysningskilde.MELDING_MODIA,
                            årsak = Revurderingsårsak.ENDRING_INNTEKT,
                        ),
                    kategori = BehandlingKategori.NASJONAL,
                ),
            søker =
                SøkerDto(
                    personIdent = "12345678901",
                    barn =
                        listOf(
                            BarnDto(
                                personIdent = "01234567890",
                                termindato = LocalDate.of(2024, 3, 15),
                            ),
                            BarnDto(
                                personIdent = "09876543210",
                                termindato = null,
                            ),
                        ),
                    tilhørendeEnhet = "4489",
                    adressebeskyttelse = AdressebeskyttelseGradering.UGRADERT,
                ),
            vedtak =
                VedtaksdetaljerOvergangsstønadDto(
                    resultat = Vedtaksresultat.INNVILGET,
                    vedtakstidspunkt = vedtakstidspunkt,
                    opphørÅrsak = null,
                    saksbehandlerId = "Z123456",
                    beslutterId = "Z654321",
                    tilkjentYtelse =
                        TilkjentYtelseDto(
                            andelerTilkjentYtelse =
                                listOf(
                                    AndelTilkjentYtelseDto(
                                        beløp = 15000,
                                        periode =
                                            no.nav.familie.kontrakter.felles.Månedsperiode(
                                                fom = YearMonth.of(2024, 2),
                                                tom = YearMonth.of(2024, 12),
                                            ),
                                        inntekt = 300000,
                                        inntektsreduksjon = 5000,
                                        samordningsfradrag = 0,
                                        kildeBehandlingId = behandlingId,
                                    ),
                                ),
                            startdato = LocalDate.of(2024, 2, 1),
                        ),
                    vedtaksperioder =
                        listOf(
                            VedtaksperiodeOvergangsstønadDto(
                                periode =
                                    no.nav.familie.kontrakter.felles.Månedsperiode(
                                        fom = YearMonth.of(2024, 2),
                                        tom = YearMonth.of(2024, 12),
                                    ),
                                aktivitet = AktivitetType.BARN_UNDER_ETT_ÅR,
                                periodeType = VedtaksperiodeType.HOVEDPERIODE,
                            ),
                        ),
                    tilbakekreving = null,
                    brevmottakere =
                        listOf(
                            Brevmottaker(
                                ident = "12345678901",
                                navn = "Ola Nordmann",
                                identType = Brevmottaker.IdentType.PERSONIDENT,
                                mottakerRolle = Brevmottaker.MottakerRolle.BRUKER,
                            ),
                        ),
                    avslagÅrsak = null,
                    grunnbeløp =
                        Grunnbeløp(
                            periode =
                                no.nav.familie.kontrakter.felles.Månedsperiode(
                                    fom = YearMonth.of(2024, 5),
                                    tom = YearMonth.of(2025, 4),
                                ),
                            grunnbeløp = BigDecimal("124028"),
                        ),
                ),
        )
    }
}
