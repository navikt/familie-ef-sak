package no.nav.familie.ef.sak.no.nav.familie.ef.sak.opplysninger.personopplysninger.inntekt

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.amelding.InntektResponse
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.opplysninger.personopplysninger.inntekt.GrunnlagsdataInntekt
import no.nav.familie.ef.sak.opplysninger.personopplysninger.inntekt.GrunnlagsdataInntektRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.felles.objectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class GrunnlagsdataInntektRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var grunnlagsdataInntektRepository: GrunnlagsdataInntektRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Test
    fun `insert med gyldige verdier`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val inntektResponse = objectMapper.readValue<InntektResponse>(inntektResponseJson)

        val grunnlagsdataInntekt = GrunnlagsdataInntekt(behandling.id, inntektResponse)
        grunnlagsdataInntektRepository.insert(grunnlagsdataInntekt)

        assertThat(grunnlagsdataInntektRepository.findById(behandling.id).get().inntektsdata).isEqualTo(inntektResponse)
    }
}

val inntektResponseJson =
    """
    {
      "data": [
        {
          "maaned": "2024-07",
          "opplysningspliktig": "1",
          "underenhet": "2",
          "norskident": "3",
          "oppsummeringstidspunkt": "2022-03-01T00:00:00Z",
          "inntektListe": [
            {
              "type": "Loennsinntekt",
              "beloep": 10000.0,
              "fordel": "kontantytelse",
              "beskrivelse": "fastloenn",
              "inngaarIGrunnlagForTrekk": true,
              "utloeserArbeidsgiveravgift": true,
              "skatteOgAvgiftsregel": "InngaarAlltid",
              "opptjeningsperiodeFom": "2022-01-01",
              "opptjeningsperiodeTom": "2022-01-31",
              "tilleggsinformasjon": {
                "type": "Nettoloennsordning"
              },
              "manuellVurdering": false,
              "antall": null,
              "skattemessigBosattLand": null,
              "opptjeningsland": null
            },
            {
              "type": "Loennsinntekt",
              "beloep": 10000.67,
              "fordel": "kontantytelse",
              "beskrivelse": "fastloenn",
              "inngaarIGrunnlagForTrekk": true,
              "utloeserArbeidsgiveravgift": true,
              "skatteOgAvgiftsregel": "InngaarAlltid",
              "opptjeningsperiodeFom": "2022-02-01",
              "opptjeningsperiodeTom": "2022-02-28",
              "tilleggsinformasjon": {
                "type": "Nettoloennsordning"
              },
              "manuellVurdering": false,
              "antall": null,
              "skattemessigBosattLand": "NO",
              "opptjeningsland": "NO"
            },
            {
              "type": "YtelseFraOffentlige",
              "beloep": 10000.0,
              "fordel": "kontantytelse",
              "beskrivelse": "sykepenger",
              "inngaarIGrunnlagForTrekk": true,
              "utloeserArbeidsgiveravgift": false,
              "skatteOgAvgiftsregel": "InngaarAlltid",
              "opptjeningsperiodeFom": null,
              "opptjeningsperiodeTom": null,
              "tilleggsinformasjon": {
                "type": "ETTERBETALINGSPERIODE"
              },
              "manuellVurdering": false,
              "antall": null,
              "skattemessigBosattLand": null,
              "opptjeningsland": null
            },
            {
              "type": "YtelseFraOffentlige",
              "beloep": 5000.0,
              "fordel": "kontantytelse",
              "beskrivelse": "overgangsstoenadTilEnsligMorEllerFarSomBegynteAaLoepe1April2014EllerSenere",
              "inngaarIGrunnlagForTrekk": true,
              "utloeserArbeidsgiveravgift": false,
              "skatteOgAvgiftsregel": "InngaarAlltid",
              "opptjeningsperiodeFom": null,
              "opptjeningsperiodeTom": null,
              "tilleggsinformasjon": {
                "type": "Nettoloennsordning"
              },
              "manuellVurdering": false,
              "antall": null,
              "skattemessigBosattLand": null,
              "opptjeningsland": null
            }
          ],
          "forskuddstrekkListe": [],
          "avvikListe": []
        }
      ]
    }
    """.trimIndent()
