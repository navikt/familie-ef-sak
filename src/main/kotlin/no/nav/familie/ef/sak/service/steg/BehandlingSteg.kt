package no.nav.familie.ef.sak.service.steg

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType

interface BehandlingSteg<T> {

    fun validerSteg(behandling: Behandling) {}

    /**
     * Hvis man trenger å overridea vanlige flytet og returnere en annen stegtype kan man overridea denne metoden,
     * hvis ikke kalles utførSteg uten å returnere en stegType
     */
    fun utførOgReturnerNesteSteg(behandling: Behandling, data: T): StegType {
        utførSteg(behandling, data)
        return stegType().hentNesteSteg(behandling.type)
    }

    fun utførSteg(behandling: Behandling, data: T)

    fun stegType(): StegType

    fun settInnHistorikk() = true

}

enum class StegType(val rekkefølge: Int,
                    val tillattFor: BehandlerRolle,
                    private val gyldigIKombinasjonMedStatus: List<BehandlingStatus>) {

    REGISTRERE_OPPLYSNINGER(rekkefølge = 1,
                            tillattFor = BehandlerRolle.SAKSBEHANDLER,
                            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)),
    VILKÅRSVURDERE_INNGANGSVILKÅR(rekkefølge = 2,
                                  tillattFor = BehandlerRolle.SAKSBEHANDLER,
                                  gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)),
    VILKÅRSVURDERE_STØNAD(rekkefølge = 3,
                          tillattFor = BehandlerRolle.SAKSBEHANDLER,
                          gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)),
    BEREGNE_YTELSE(rekkefølge = 4,
                   tillattFor = BehandlerRolle.SAKSBEHANDLER,
                   gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)),
    SEND_TIL_BESLUTTER(rekkefølge = 5,
                       tillattFor = BehandlerRolle.SAKSBEHANDLER,
                       gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.UTREDES)),
    BESLUTTE_VEDTAK(rekkefølge = 6,
                    tillattFor = BehandlerRolle.BESLUTTER,
                    gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.FATTER_VEDTAK)),
    IVERKSETT_MOT_OPPDRAG(rekkefølge = 7,
                          tillattFor = BehandlerRolle.SYSTEM,
                          gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK)),
    VENTE_PÅ_STATUS_FRA_ØKONOMI(rekkefølge = 8,
                                tillattFor = BehandlerRolle.SYSTEM,
                                gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK)),
    JOURNALFØR_VEDTAKSBREV(rekkefølge = 9,
                           tillattFor = BehandlerRolle.SYSTEM,
                           gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK)),
    DISTRIBUER_VEDTAKSBREV(rekkefølge = 10,
                           tillattFor = BehandlerRolle.SYSTEM,
                           gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK)),
    FERDIGSTILLE_BEHANDLING(rekkefølge = 11,
                            tillattFor = BehandlerRolle.SYSTEM,
                            gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.IVERKSETTER_VEDTAK)),
    BEHANDLING_FERDIGSTILT(rekkefølge = 12,
                           tillattFor = BehandlerRolle.SYSTEM,
                           gyldigIKombinasjonMedStatus = listOf(BehandlingStatus.FERDIGSTILT));

    fun displayName(): String {
        return this.name.replace('_', ' ').toLowerCase().capitalize()
    }

    fun kommerEtter(steg: StegType, behandlingType: BehandlingType): Boolean {
        return this.rekkefølge > steg.rekkefølge
    }

    fun erGyldigIKombinasjonMedStatus(behandlingStatus: BehandlingStatus): Boolean {
        return this.gyldigIKombinasjonMedStatus.contains(behandlingStatus)
    }

    fun hentNesteSteg(behandlingType: BehandlingType): StegType {
        return when (behandlingType) {
            BehandlingType.TEKNISK_OPPHØR ->
                when (this) {
                    REGISTRERE_OPPLYSNINGER -> VILKÅRSVURDERE_INNGANGSVILKÅR
                    VILKÅRSVURDERE_INNGANGSVILKÅR -> VILKÅRSVURDERE_STØNAD
                    VILKÅRSVURDERE_STØNAD -> BEREGNE_YTELSE
                    BEREGNE_YTELSE -> SEND_TIL_BESLUTTER
                    SEND_TIL_BESLUTTER -> BESLUTTE_VEDTAK
                    BESLUTTE_VEDTAK -> IVERKSETT_MOT_OPPDRAG
                    IVERKSETT_MOT_OPPDRAG -> VENTE_PÅ_STATUS_FRA_ØKONOMI
                    VENTE_PÅ_STATUS_FRA_ØKONOMI -> FERDIGSTILLE_BEHANDLING
                    FERDIGSTILLE_BEHANDLING -> BEHANDLING_FERDIGSTILT
                    BEHANDLING_FERDIGSTILT -> BEHANDLING_FERDIGSTILT
                    else -> throw IllegalStateException("StegType ${displayName()} ugyldig ved teknisk opphør")
                }
            else ->
                when (this) {
                    REGISTRERE_OPPLYSNINGER -> VILKÅRSVURDERE_INNGANGSVILKÅR
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