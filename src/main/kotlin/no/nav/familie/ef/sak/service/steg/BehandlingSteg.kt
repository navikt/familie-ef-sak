package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType

interface BehandlingSteg<T> {

    fun utførStegOgAngiNeste(behandling: Behandling,
                             data: T): StegType

    fun stegType(): StegType


    fun hentNesteSteg(behandling: Behandling): StegType {
        return behandling.steg.hentNesteSteg(utførendeStegType = this.stegType(), behandlingType = behandling.type)
    }

    fun preValiderSteg(behandling: Behandling, stegService: StegService? = null) {}
    fun postValiderSteg(behandling: Behandling) {}

}

enum class StegType(val rekkefølge: Int,
                    val tillattFor: List<BehandlerRolle>,
                    private val gyldigIKombinasjonMedStatus: List<BehandlingStatus>) {

    REGISTRERE_SØKNAD(
            rekkefølge = 1,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)),
    VILKÅRSVURDERE_INNGANGSVILKÅR(
            rekkefølge = 2,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)),
    VILKÅRSVURDERE_STØNAD(
            rekkefølge = 3,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)),
    BEREGNE_YTELSE(
            rekkefølge = 4,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)),
    SEND_TIL_BESLUTTER(
            rekkefølge = 5,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)),
    BESLUTTE_VEDTAK(
            rekkefølge = 6,
            tillattFor = listOf(BehandlerRolle.SYSTEM, BehandlerRolle.SAKSBEHANDLER),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.FATTER_VEDTAK)),
    IVERKSETT_MOT_OPPDRAG(
            rekkefølge = 7,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK)),
    VENTE_PÅ_STATUS_FRA_ØKONOMI(
            rekkefølge = 8,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK)
    ),
    JOURNALFØR_VEDTAKSBREV(
            rekkefølge = 9,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK)
    ),
    DISTRIBUER_VEDTAKSBREV(
            rekkefølge = 10,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK)
    ),
    FERDIGSTILLE_BEHANDLING(
            rekkefølge = 11,
            tillattFor = listOf(BehandlerRolle.SYSTEM),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK)),
    BEHANDLING_FERDIGSTILT(
            rekkefølge = 12,
            tillattFor = emptyList(),
            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.FERDIGSTILT));

    fun displayName(): String {
        return this.name.replace('_', ' ').toLowerCase().capitalize()
    }

    fun kommerEtter(steg: StegType): Boolean {
        return this.rekkefølge > steg.rekkefølge
    }

    fun erGyldigIKombinasjonMedStatus(behandlingStatus: BehandlingStatus): Boolean {
        return this.gyldigIKombinasjonMedStatus.contains(behandlingStatus)
    }

    fun erSaksbehandlerSteg(): Boolean {
        return this.tillattFor.any { it == BehandlerRolle.SAKSBEHANDLER || it == BehandlerRolle.BESLUTTER }
    }

    fun hentNesteSteg(utførendeStegType: StegType,
                      behandlingType: BehandlingType? = null): StegType {

        return when (behandlingType) {
            BehandlingType.TEKNISK_OPPHØR ->
                when (utførendeStegType) {
                    REGISTRERE_SØKNAD -> VILKÅRSVURDERE_INNGANGSVILKÅR
                    VILKÅRSVURDERE_INNGANGSVILKÅR -> VILKÅRSVURDERE_STØNAD
                    VILKÅRSVURDERE_STØNAD -> BEREGNE_YTELSE
                    BEREGNE_YTELSE -> SEND_TIL_BESLUTTER
                    SEND_TIL_BESLUTTER -> BESLUTTE_VEDTAK
                    BESLUTTE_VEDTAK -> IVERKSETT_MOT_OPPDRAG
                    IVERKSETT_MOT_OPPDRAG -> VENTE_PÅ_STATUS_FRA_ØKONOMI
                    VENTE_PÅ_STATUS_FRA_ØKONOMI -> JOURNALFØR_VEDTAKSBREV
                    FERDIGSTILLE_BEHANDLING -> BEHANDLING_FERDIGSTILT
                    BEHANDLING_FERDIGSTILT -> BEHANDLING_FERDIGSTILT
                    else -> throw IllegalStateException("StegType ${utførendeStegType.displayName()} ugyldig ved teknisk opphør")
                }
            else ->
                when (utførendeStegType) {
                REGISTRERE_SØKNAD -> VILKÅRSVURDERE_INNGANGSVILKÅR
                VILKÅRSVURDERE_INNGANGSVILKÅR -> VILKÅRSVURDERE_STØNAD
                VILKÅRSVURDERE_STØNAD -> BEREGNE_YTELSE
                BEREGNE_YTELSE -> SEND_TIL_BESLUTTER
                SEND_TIL_BESLUTTER -> BESLUTTE_VEDTAK
                BESLUTTE_VEDTAK -> IVERKSETT_MOT_OPPDRAG
                IVERKSETT_MOT_OPPDRAG -> VENTE_PÅ_STATUS_FRA_ØKONOMI
                VENTE_PÅ_STATUS_FRA_ØKONOMI -> JOURNALFØR_VEDTAKSBREV
                JOURNALFØR_VEDTAKSBREV -> DISTRIBUER_VEDTAKSBREV
                DISTRIBUER_VEDTAKSBREV -> FERDIGSTILLE_BEHANDLING
                FERDIGSTILLE_BEHANDLING -> BEHANDLING_FERDIGSTILT
                BEHANDLING_FERDIGSTILT -> BEHANDLING_FERDIGSTILT
            }
        }
    }
}

enum class BehandlerRolle(val nivå: Int) {
    SYSTEM(4),
    BESLUTTER(3),
    SAKSBEHANDLER(2),
    VEILEDER(1),
    UKJENT(0)
}