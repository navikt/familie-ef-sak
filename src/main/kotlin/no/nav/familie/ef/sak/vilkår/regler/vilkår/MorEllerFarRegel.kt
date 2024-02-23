package no.nav.familie.ef.sak.vilkår.regler.vilkår

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.felles.util.norskFormat
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vurdering
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.RegelSteg
import no.nav.familie.ef.sak.vilkår.regler.SluttSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.familie.ef.sak.vilkår.regler.regelIder
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

val secureLogger: Logger = LoggerFactory.getLogger("secureLogger")

class MorEllerFarRegel : Vilkårsregel(
    vilkårType = VilkårType.MOR_ELLER_FAR,
    regler = setOf(OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN),
    hovedregler = regelIder(OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN),
) {

    override fun initiereDelvilkårsvurdering(
        metadata: HovedregelMetadata,
        resultat: Vilkårsresultat,
        barnId: UUID?,
    ): List<Delvilkårsvurdering> {
        if (resultat != Vilkårsresultat.IKKE_TATT_STILLING_TIL) {
            return super.initiereDelvilkårsvurdering(metadata, resultat, barnId)
        }

        return hovedregler.map {
            if (it == RegelId.OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN && erMorEllerFarForAlleBarn(metadata)) {
                automatiskOppfyllErMorEllerFar()
            } else {
                Delvilkårsvurdering(resultat, vurderinger = listOf(Vurdering(it)))
            }
        }
    }

    fun erMorEllerFarForAlleBarn(metadata: HovedregelMetadata): Boolean {
        secureLogger.info("Beregner er mor eller far for alle barn. Metadata: " + objectMapper.writeValueAsString(metadata))
        return metadata.behandling.årsak == BehandlingÅrsak.SØKNAD &&
                metadata.vilkårgrunnlagDto.barnMedSamvær.all { it.registergrunnlag.fødselsnummer != null }
    }

    private fun automatiskOppfyllErMorEllerFar(): Delvilkårsvurdering {
        val beskrivelse = "Automatisk vurdert: Den ${LocalDate.now().norskFormat()} er det" +
            " automatisk vurdert at bruker søker stønad for egne/adopterte barn?."
        return Delvilkårsvurdering(
            resultat = Vilkårsresultat.AUTOMATISK_OPPFYLT,
            listOf(
                Vurdering(
                    regelId = RegelId.OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN,
                    svar = SvarId.JA,
                    begrunnelse = beskrivelse,
                ),
            ),
        )
    }

    companion object {

        private val OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN =
            RegelSteg(
                regelId = RegelId.OMSORG_FOR_EGNE_ELLER_ADOPTERTE_BARN,
                svarMapping = jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                    hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                ),
            )
    }
}


fun main() {

    val json = """
        {"sivilstandSøknad":{"erUformeltGift":false,"erUformeltGiftDokumentasjon":null,"erUformeltSeparertEllerSkilt":true,"erUformeltSeparertEllerSkiltDokumentasjon":{"harSendtInnTidligere":false,"dokumenter":[]},"søktOmSkilsmisseSeparasjon":null,"datoSøktSeparasjon":null,"separasjonsbekreftelse":null,"årsakEnslig":"dødsfall","samlivsbruddsdokumentasjon":null,"samlivsbruddsdato":null,"fraflytningsdato":null,"endringSamværsordningDato":null,"tidligereSamboer":null},"sivilstandstype":"UGIFT","erMigrering":false,"barn":[{"id":"b4c3a216-c411-44a6-b624-7d957a331c7c","behandlingId":"86cc63d9-6910-4ef9-8d46-5c7cb5907632","søknadBarnId":"7b285a4c-725c-4e7b-92f4-7ae79c63aba4","personIdent":"18472184098","navn":"FORNUFTIG MØLLE","fødselTermindato":"2021-07-18","sporbar":{"opprettetAv":"VL","opprettetTid":"2024-02-23T09:44:40.422","endret":{"endretAv":"VL","endretTid":"2024-02-23T09:44:40.428"}}},{"id":"2bc46e08-c247-4cc2-bada-0703bb090dc4","behandlingId":"86cc63d9-6910-4ef9-8d46-5c7cb5907632","søknadBarnId":"1f1c2e56-540c-4640-9bc3-e492fafa7992","personIdent":"10461884941","navn":"RAKRYGGET SENTRUM","fødselTermindato":"2018-06-10","sporbar":{"opprettetAv":"VL","opprettetTid":"2024-02-23T09:44:40.422","endret":{"endretAv":"VL","endretTid":"2024-02-23T09:44:40.44"}}}],"søktOmBarnetilsyn":[],"langAvstandTilSøker":[{"barnId":"b4c3a216-c411-44a6-b624-7d957a331c7c","langAvstandTilSøker":"UKJENT"},{"barnId":"2bc46e08-c247-4cc2-bada-0703bb090dc4","langAvstandTilSøker":"UKJENT"}],"vilkårgrunnlagDto":{"personalia":{"navn":{"fornavn":"USJENERT","mellomnavn":null,"etternavn":"JORDE","visningsnavn":"USJENERT JORDE"},"personIdent":"02509946186","bostedsadresse":{"visningsadresse":"Torbergjorda 76, 8340 STAMSUND","type":"BOSTEDADRESSE","gyldigFraOgMed":"2021-07-18","gyldigTilOgMed":null,"angittFlyttedato":null,"erGjeldende":true}},"tidligereVedtaksperioder":{"infotrygd":{"harTidligereOvergangsstønad":false,"harTidligereBarnetilsyn":false,"harTidligereSkolepenger":false,"periodeHistorikkOvergangsstønad":[],"periodeHistorikkBarnetilsyn":[]},"sak":{"harTidligereOvergangsstønad":false,"harTidligereBarnetilsyn":false,"harTidligereSkolepenger":false,"periodeHistorikkOvergangsstønad":[],"periodeHistorikkBarnetilsyn":[]},"historiskPensjon":false},"medlemskap":{"søknadsgrunnlag":{"bosattNorgeSisteÅrene":true,"oppholderDuDegINorge":true,"oppholdsland":null,"utenlandsopphold":[]},"registergrunnlag":{"nåværendeStatsborgerskap":["NORGE"],"statsborgerskap":[{"land":"NORGE","gyldigFraOgMedDato":null,"gyldigTilOgMedDato":null}],"oppholdstatus":[],"bostedsadresse":[{"visningsadresse":"Torbergjorda 76, 8340 STAMSUND","type":"BOSTEDADRESSE","gyldigFraOgMed":"2021-07-18","gyldigTilOgMed":null,"angittFlyttedato":null,"erGjeldende":true}],"innflytting":[],"utflytting":[],"folkeregisterpersonstatus":"BOSATT","medlUnntak":{"gyldigeVedtaksPerioder":[]}}},"sivilstand":{"søknadsgrunnlag":{"samlivsbruddsdato":null,"endringSamværsordningDato":null,"fraflytningsdato":null,"erUformeltGift":false,"erUformeltSeparertEllerSkilt":true,"datoSøktSeparasjon":null,"søktOmSkilsmisseSeparasjon":null,"årsakEnslig":"dødsfall","tidligereSamboer":null},"registergrunnlag":{"type":"UGIFT","personIdent":null,"navn":null,"gyldigFraOgMed":"1999-10-02"}},"bosituasjon":{"delerDuBolig":"borAleneMedBarnEllerGravid","samboer":null,"sammenflyttingsdato":null,"datoFlyttetFraHverandre":null,"tidligereSamboerFortsattRegistrertPåAdresse":null},"barnMedSamvær":[{"barnId":"b4c3a216-c411-44a6-b624-7d957a331c7c","søknadsgrunnlag":{"id":"b4c3a216-c411-44a6-b624-7d957a331c7c","navn":"FORNUFTIG MØLLE","fødselTermindato":"2021-07-18","harSammeAdresse":true,"skalBoBorHosSøker":null,"forelder":{"navn":"DOGMATISK FANTASI","fødselsnummer":"30419511052","fødselsdato":null,"bosattINorge":true,"land":null,"visningsadresse":null,"dødsfall":null,"tidligereVedtaksperioder":null,"avstandTilSøker":{"avstandIKm":null,"langAvstandTilSøker":"UKJENT"}},"ikkeOppgittAnnenForelderBegrunnelse":null,"spørsmålAvtaleOmDeltBosted":false,"skalAnnenForelderHaSamvær":"nei","harDereSkriftligAvtaleOmSamvær":null,"hvordanPraktiseresSamværet":null,"borAnnenForelderISammeHus":"nei","borAnnenForelderISammeHusBeskrivelse":null,"harDereTidligereBoddSammen":false,"nårFlyttetDereFraHverandre":null,"hvorMyeErDuSammenMedAnnenForelder":"møtesIkke","beskrivSamværUtenBarn":null},"registergrunnlag":{"id":"b4c3a216-c411-44a6-b624-7d957a331c7c","navn":"FORNUFTIG MØLLE","fødselsnummer":"18472184098","harSammeAdresse":true,"deltBostedPerioder":[],"harDeltBostedVedGrunnlagsdataopprettelse":false,"forelder":{"navn":"DOGMATISK FANTASI","fødselsnummer":"30419511052","fødselsdato":"1995-01-30","bosattINorge":true,"land":null,"visningsadresse":"Torbergjorda 76, 8340 STAMSUND","dødsfall":null,"tidligereVedtaksperioder":{"infotrygd":{"harTidligereOvergangsstønad":false,"harTidligereBarnetilsyn":false,"harTidligereSkolepenger":false,"periodeHistorikkOvergangsstønad":[],"periodeHistorikkBarnetilsyn":[]},"sak":{"harTidligereOvergangsstønad":false,"harTidligereBarnetilsyn":false,"harTidligereSkolepenger":false,"periodeHistorikkOvergangsstønad":[],"periodeHistorikkBarnetilsyn":[]},"historiskPensjon":false},"avstandTilSøker":{"avstandIKm":0,"langAvstandTilSøker":"UKJENT"}},"dødsdato":null,"fødselsdato":"2021-07-18"},"barnepass":null},{"barnId":"2bc46e08-c247-4cc2-bada-0703bb090dc4","søknadsgrunnlag":{"id":"2bc46e08-c247-4cc2-bada-0703bb090dc4","navn":"RAKRYGGET SENTRUM","fødselTermindato":"2018-06-10","harSammeAdresse":true,"skalBoBorHosSøker":null,"forelder":{"navn":"DOGMATISK FANTASI","fødselsnummer":"30419511052","fødselsdato":null,"bosattINorge":true,"land":null,"visningsadresse":null,"dødsfall":null,"tidligereVedtaksperioder":null,"avstandTilSøker":{"avstandIKm":null,"langAvstandTilSøker":"UKJENT"}},"ikkeOppgittAnnenForelderBegrunnelse":null,"spørsmålAvtaleOmDeltBosted":true,"skalAnnenForelderHaSamvær":"nei","harDereSkriftligAvtaleOmSamvær":null,"hvordanPraktiseresSamværet":null,"borAnnenForelderISammeHus":"nei","borAnnenForelderISammeHusBeskrivelse":null,"harDereTidligereBoddSammen":false,"nårFlyttetDereFraHverandre":null,"hvorMyeErDuSammenMedAnnenForelder":"møtesIkke","beskrivSamværUtenBarn":null},"registergrunnlag":{"id":"2bc46e08-c247-4cc2-bada-0703bb090dc4","navn":"RAKRYGGET SENTRUM","fødselsnummer":"10461884941","harSammeAdresse":true,"deltBostedPerioder":[],"harDeltBostedVedGrunnlagsdataopprettelse":false,"forelder":{"navn":"DOGMATISK FANTASI","fødselsnummer":"30419511052","fødselsdato":"1995-01-30","bosattINorge":true,"land":null,"visningsadresse":"Torbergjorda 76, 8340 STAMSUND","dødsfall":null,"tidligereVedtaksperioder":{"infotrygd":{"harTidligereOvergangsstønad":false,"harTidligereBarnetilsyn":false,"harTidligereSkolepenger":false,"periodeHistorikkOvergangsstønad":[],"periodeHistorikkBarnetilsyn":[]},"sak":{"harTidligereOvergangsstønad":false,"harTidligereBarnetilsyn":false,"harTidligereSkolepenger":false,"periodeHistorikkOvergangsstønad":[],"periodeHistorikkBarnetilsyn":[]},"historiskPensjon":false},"avstandTilSøker":{"avstandIKm":0,"langAvstandTilSøker":"UKJENT"}},"dødsdato":null,"fødselsdato":"2018-06-10"},"barnepass":null}],"sivilstandsplaner":{"harPlaner":false,"fraDato":null,"vordendeSamboerEktefelle":null},"aktivitet":{"arbeidssituasjon":["erHverkenIArbeidUtdanningEllerArbeidssøker"],"arbeidsforhold":[],"selvstendig":[],"aksjeselskap":[],"arbeidssøker":null,"underUtdanning":null,"virksomhet":null,"tidligereUtdanninger":[],"gjelderDeg":["nei"],"særligeTilsynsbehov":[],"datoOppstartJobb":null,"erIArbeid":null},"sagtOppEllerRedusertStilling":{"sagtOppEllerRedusertStilling":"nei","årsak":null,"dato":null},"registeropplysningerOpprettetTid":"2024-02-23T09:44:40.324","adresseopplysninger":{"søkerBorPåRegistrertAdresse":true,"adresse":"Torbergjorda 76, 8340 STAMSUND","harMeldtAdresseendring":null},"dokumentasjon":{"erIArbeid":null,"virksomhet":null,"ikkeVilligTilÅTaImotTilbudOmArbeid":null,"tidligereSamboerFortsattRegistrertPåAdresse":null,"uformeltGift":null,"uformeltSeparertEllerSkilt":{"harSendtInn":false,"vedlegg":[]},"separasjonsbekreftelse":null,"samlivsbrudd":null,"avtaleOmDeltBosted":{"harSendtInn":false,"vedlegg":[]},"samværsavtale":null,"skalBarnetBoHosSøkerMenAnnenForelderSamarbeiderIkke":null,"erklæringOmSamlivsbrudd":null,"terminbekreftelse":null,"barnepassordningFaktura":null,"avtaleBarnepasser":null,"arbeidstid":null,"roterendeArbeidstid":null,"spesielleBehov":null,"sykdom":null,"barnsSykdom":null,"manglendeBarnepass":null,"barnMedSærligeBehov":null,"arbeidskontrakt":null,"lærlingkontrakt":null,"utdanningstilbud":null,"reduksjonAvArbeidsforhold":null,"oppsigelse":null,"utdanningsutgifter":null,"meldtAdresseendring":null},"harAvsluttetArbeidsforhold":false},"behandling":{"id":"86cc63d9-6910-4ef9-8d46-5c7cb5907632","fagsakId":"5652fcb4-75ba-4d41-977c-6dce975e776f","forrigeBehandlingId":null,"eksternId":{"id":18135},"versjon":0,"type":"FØRSTEGANGSBEHANDLING","status":"OPPRETTET","steg":"VILKÅR","kategori":"NASJONAL","årsak":"SØKNAD","kravMottatt":null,"sporbar":{"opprettetAv":"VL","opprettetTid":"2024-02-23T09:44:38.632","endret":{"endretAv":"VL","endretTid":"2024-02-23T09:44:38.643"}},"resultat":"IKKE_SATT","henlagtÅrsak":null,"vedtakstidspunkt":null}}
    """.trimIndent()

    val test = objectMapper.readValue<HovedregelMetadata>(json)
    println(test)
}