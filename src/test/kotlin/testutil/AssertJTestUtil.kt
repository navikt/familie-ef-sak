package no.nav.familie.ef.sak.testutil

import org.assertj.core.api.Assertions
import org.assertj.core.api.Condition

fun hasCauseMessageContaining(msg: String) =
        Condition<Throwable>({
                                 val message = it.cause?.message ?: error("Savner cause/message")
                                 Assertions.assertThat(message).contains(msg)
                                 true
                             }, "")
