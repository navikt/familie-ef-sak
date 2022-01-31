package no.nav.familie.ef.sak.brev.dto

import no.nav.familie.ef.sak.brev.domain.FRITEKST


data class VedtaksbrevDto(val saksbehandlerBrevrequest: String,
                          val brevmal: String,
                          val saksbehandlersignatur: String,
                          val besluttersignatur: String? = null,
                          val enhet: String? = null,
                          val skjulBeslutterSignatur: Boolean)

fun VedtaksbrevDto.erFritekstType(): Boolean = this.brevmal == FRITEKST