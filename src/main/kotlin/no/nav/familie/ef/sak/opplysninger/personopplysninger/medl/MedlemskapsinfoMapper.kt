package no.nav.familie.ef.sak.opplysninger.personopplysninger.medl

import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import no.nav.familie.kontrakter.felles.medlemskap.PeriodeInfo
import no.nav.familie.kontrakter.felles.medlemskap.PeriodeStatus
import no.nav.familie.kontrakter.felles.medlemskap.PeriodeStatusÅrsak

object MedlemskapsinfoMapper {
    fun tilMedlemskapsInfo(responsListe: List<Medlemskapsunntak>): Medlemskapsinfo {
        val gyldigePerioder: List<PeriodeInfo> =
            responsListe
                .filter { PeriodeStatus.GYLD.name == it.status }
                .map(this::tilPeriodeInfo)
        val avvistePerioder: List<PeriodeInfo> =
            responsListe
                .filter { PeriodeStatus.AVST.name == it.status }
                .map(this::tilPeriodeInfo)
        val uavklartePerioder: List<PeriodeInfo> =
            responsListe
                .filter { PeriodeStatus.UAVK.name == it.status }
                .map(this::tilPeriodeInfo)
        return Medlemskapsinfo(
            gyldigePerioder = gyldigePerioder,
            avvistePerioder = avvistePerioder,
            uavklartePerioder = uavklartePerioder,
            personIdent = tilPersonIdent(responsListe),
        )
    }

    private fun tilPeriodeInfo(medlemskapsunntak: Medlemskapsunntak): PeriodeInfo =
        PeriodeInfo(
            periodeStatus = PeriodeStatus.valueOf(medlemskapsunntak.status),
            fom = medlemskapsunntak.fraOgMed,
            tom = medlemskapsunntak.tilOgMed,
            dekning = medlemskapsunntak.dekning,
            grunnlag = medlemskapsunntak.grunnlag,
            gjelderMedlemskapIFolketrygden = medlemskapsunntak.medlem,
            periodeStatusÅrsak =
                if (medlemskapsunntak.statusaarsak == null) null else PeriodeStatusÅrsak.valueOf(medlemskapsunntak.statusaarsak),
        )

    private fun tilPersonIdent(responseList: List<Medlemskapsunntak>): String {
        val alleIdenter = responseList.map(Medlemskapsunntak::ident).toSet()
        return if (alleIdenter.size == 1) alleIdenter.first() else ""
    }
}
