package no.nav.familie.ef.sak.no.nav.familie.ef.sak.opplysninger.personopplysninger.medl

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.jsonMapper
import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.ef.sak.opplysninger.personopplysninger.medl.MedlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.medl.MedlService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.medl.Medlemskapsunntak
import no.nav.familie.kontrakter.felles.medlemskap.PeriodeStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MedlemskapServiceTest {
    private lateinit var medlemskapService: MedlService
    private val medlClient: MedlClient = mockk()

    @BeforeEach
    fun setUp() {
        medlemskapService = MedlService(medlClient)
    }

    @Test
    fun `skal gi tomt objekt ved ingen treff i MEDL`() {
        every { medlClient.hentMedlemskapsUnntak(any()) }
            .returns(emptyList())

        val respons = medlemskapService.hentMedlemskapsunntak(TEST_IDENT)

        assertThat(respons).isNotNull
        assertThat(respons.personIdent).isEqualTo("")
        assertThat(respons.avvistePerioder).isEqualTo(emptyList<Any>())
        assertThat(respons.gyldigePerioder).isEqualTo(emptyList<Any>())
        assertThat(respons.uavklartePerioder).isEqualTo(emptyList<Any>())
    }

    @Test
    fun `skal gruppere perioder ved treff`() {
        every { medlClient.hentMedlemskapsUnntak(any()) }
            .returns(jsonMapper.readValue<List<Medlemskapsunntak>>(medlResponse))
        val respons = medlemskapService.hentMedlemskapsunntak(TEST_IDENT)

        assertThat(respons).isNotNull
        assertThat(respons.uavklartePerioder.size).isEqualTo(1)
        assertThat(respons.gyldigePerioder.size).isEqualTo(7)
        assertThat(respons.avvistePerioder.size).isEqualTo(4)
        assertThat(respons.gyldigePerioder[0].periodeStatusÅrsak).isNull()
        assertThat(respons.avvistePerioder[0].periodeStatusÅrsak).isNotNull
        assertThat(respons.gyldigePerioder[0].periodeStatus).isEqualTo(PeriodeStatus.GYLD)
        assertThat(respons.avvistePerioder[0].periodeStatus).isEqualTo(PeriodeStatus.AVST)
        assertThat(respons.uavklartePerioder[0].periodeStatus).isEqualTo(PeriodeStatus.UAVK)
    }

    @Test
    fun `periodeInfo har påkrevde felter`() {
        every { medlClient.hentMedlemskapsUnntak(any()) }
            .returns(jsonMapper.readValue<List<Medlemskapsunntak>>(medlResponse))
        val respons = medlemskapService.hentMedlemskapsunntak(TEST_IDENT)

        assertThat(respons).isNotNull
        val gyldigPeriode = respons.gyldigePerioder[0]
        assertThat(gyldigPeriode.fom).isNotNull()
        assertThat(gyldigPeriode.tom).isNotNull()
        assertThat(gyldigPeriode.grunnlag).isNotNull()
        assertThat(gyldigPeriode.gjelderMedlemskapIFolketrygden).isNotNull()
    }

    @Test
    fun `skal kaste oppslagException ved feil`() {
        every { medlClient.hentMedlemskapsUnntak(any()) }
            .throws(RuntimeException("Feil ved kall til Medl"))

        assertThrows<Exception> { medlemskapService.hentMedlemskapsunntak(TEST_IDENT) }
    }

    companion object {
        private const val TEST_IDENT = "01010199999"
    }
}

val medlResponse =
    """
    [
      {
        "unntakId": 3365198,
        "ident": "12345678911",
        "fraOgMed": "2014-01-01",
        "tilOgMed": "2014-12-31",
        "status": "AVST",
        "statusaarsak": "Feilregistrert",
        "dekning": "IHT_Avtale",
        "helsedel": true,
        "medlem": false,
        "lovvalgsland": "USA",
        "lovvalg": "ENDL",
        "grunnlag": "USA_5_2"
      },
      {
        "unntakId": 3402992,
        "ident": "12345678911",
        "fraOgMed": "2014-12-31",
        "tilOgMed": "2015-01-01",
        "status": "GYLD",
        "dekning": "Unntatt",
        "helsedel": false,
        "medlem": false,
        "lovvalgsland": "DNK",
        "lovvalg": "ENDL",
        "grunnlag": "FO_12_1"
      },
      {
        "unntakId": 3397440,
        "ident": "12345678911",
        "fraOgMed": "2014-12-23",
        "tilOgMed": "2014-12-24",
        "status": "GYLD",
        "dekning": "IHT_Avtale",
        "helsedel": true,
        "medlem": false,
        "lovvalgsland": "HRV",
        "lovvalg": "ENDL",
        "grunnlag": "IMED"
      },
      {
        "unntakId": 3408584,
        "ident": "12345678911",
        "fraOgMed": "2016-01-01",
        "tilOgMed": "2016-04-30",
        "status": "AVST",
        "statusaarsak": "Feilregistrert",
        "dekning": "IHT_Avtale",
        "helsedel": true,
        "medlem": false,
        "lovvalgsland": "CHN",
        "lovvalg": "ENDL",
        "grunnlag": "FTL_2-13_2_ledd"
      },
      {
        "unntakId": 3402984,
        "ident": "12345678911",
        "fraOgMed": "2014-12-25",
        "tilOgMed": "2014-12-27",
        "status": "GYLD",
        "dekning": "Unntatt",
        "helsedel": false,
        "medlem": false,
        "lovvalgsland": "DNK",
        "lovvalg": "ENDL",
        "grunnlag": "FO_12_1"
      },
      {
        "unntakId": 3380517,
        "ident": "12345678911",
        "fraOgMed": "2013-01-01",
        "tilOgMed": "2013-12-31",
        "status": "AVST",
        "statusaarsak": "Feilregistrert",
        "dekning": "Unntatt",
        "helsedel": false,
        "medlem": false,
        "lovvalgsland": "CHE",
        "lovvalg": "ENDL",
        "grunnlag": "Sveits"
      },
      {
        "unntakId": 3491094,
        "ident": "12345678911",
        "fraOgMed": "2012-06-02",
        "tilOgMed": "2014-06-01",
        "status": "AVST",
        "statusaarsak": "Feilregistrert",
        "dekning": "Full",
        "helsedel": true,
        "medlem": true,
        "lovvalgsland": "NOR",
        "lovvalg": "ENDL",
        "grunnlag": "FO_12_1"
      },
      {
        "unntakId": 3424733,
        "ident": "12345678911",
        "fraOgMed": "2010-01-01",
        "tilOgMed": "2010-03-31",
        "status": "GYLD",
        "dekning": "Unntatt",
        "helsedel": false,
        "medlem": false,
        "lovvalgsland": "NLD",
        "lovvalg": "ENDL",
        "grunnlag": "Nederland"
      },
      {
        "unntakId": 3435210,
        "ident": "12345678911",
        "fraOgMed": "1985-01-01",
        "tilOgMed": "1986-01-01",
        "status": "UAVK",
        "dekning": "Unntatt",
        "helsedel": false,
        "medlem": false,
        "lovvalgsland": "CHL",
        "lovvalg": "FORL",
        "grunnlag": "Chile"
      },
      {
        "unntakId": 3537947,
        "ident": "12345678911",
        "fraOgMed": "2016-01-01",
        "tilOgMed": "2016-12-31",
        "status": "GYLD",
        "dekning": "Full",
        "helsedel": true,
        "medlem": true,
        "lovvalgsland": "NOR",
        "lovvalg": "ENDL",
        "grunnlag": "FO_12_1"
      },
      {
        "unntakId": 100228323,
        "ident": "12345678911",
        "fraOgMed": "2018-01-01",
        "tilOgMed": "2018-12-31",
        "status": "GYLD",
        "dekning": "IKKEPENDEL",
        "helsedel": true,
        "medlem": false,
        "lovvalgsland": "CAN",
        "lovvalg": "ENDL",
        "grunnlag": "Canada"
      },
      {
        "unntakId": 100297029,
        "ident": "12345678911",
        "fraOgMed": "2020-01-01",
        "tilOgMed": "2022-12-31",
        "status": "GYLD",
        "dekning": "Full",
        "helsedel": true,
        "medlem": true,
        "lovvalgsland": "NOR",
        "lovvalg": "ENDL",
        "grunnlag": "FTL_2-5"
      }
    ]
    """.trimIndent()
