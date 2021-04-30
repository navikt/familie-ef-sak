package no.nav.familie.ef.sak.api.beregning

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

sealed class A()
class Aa(val a: String): A()
class Aa2(val a: String): A()

internal class VedtaksperiodeDtoTest {

    @Test
    internal fun name() {
        val a: A = Aa("")
        when(a) {
            is Aa -> println(a.a)
            is Aa2 -> println(a.a)
        }
    }
}