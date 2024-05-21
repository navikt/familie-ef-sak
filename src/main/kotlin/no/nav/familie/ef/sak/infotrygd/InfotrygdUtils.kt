package no.nav.familie.ef.sak.infotrygd

import no.nav.familie.kontrakter.ef.infotrygd.InfotrygdSakType

object InfotrygdUtils {
    val KLAGETYPER =
        setOf(
            InfotrygdSakType.KLAGE,
            InfotrygdSakType.KLAGE_TILBAKEBETALING,
            InfotrygdSakType.KLAGE_AVREGNING,
            InfotrygdSakType.KLAGE_ETTERGIVELSE,
        )
}
