package no.nav.familie.ef.sak.api.beregning

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

sealed class A()
class Aa(val a: String): A()
class Aa2(val a: String): A()

fun A.yolo() {
    when(this) {
        is Aa -> println(this.a)
        is Aa2 -> println(this.a)
    }
}

internal class VedtaksperiodeDtoTest {

    @Test
    internal fun name() {
        val a: A = Aa("")
        a.yolo()
    }
}