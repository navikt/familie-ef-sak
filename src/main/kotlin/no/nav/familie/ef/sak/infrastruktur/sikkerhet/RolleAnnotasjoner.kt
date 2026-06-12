package no.nav.familie.ef.sak.infrastruktur.sikkerhet

import org.springframework.security.access.prepost.PreAuthorize

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAnyRole('SAKSBEHANDLER', 'BESLUTTER', 'APPLICATION')")
annotation class HarRolleSaksbehandlerEllerApplikasjon

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasRole('FORVALTER')")
annotation class HarRolleForvalter
