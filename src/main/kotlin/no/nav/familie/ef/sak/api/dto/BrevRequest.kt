package no.nav.familie.ef.sak.api.dto

import java.time.LocalDate

data class BrevRequest(val navn: String,
                       val ident: String,
                       val innvilgelseFra: LocalDate,
                       val innvilgelseTil: LocalDate,
                       val begrunnelseFomDatoInnvilgelse: String,
                       val brevdato: LocalDate,
                       val belopOvergangsstonad: Int
) {
    fun lagBody(): String {
        return """
            {
              "flettefelter": {
                "navn": [
                  "$navn"
                ],
                "fodselsnummer": [
                  "$ident"
                ],
                "Fom-dato innvilgelse": [
                  "$innvilgelseFra"
                ],
                "Tom-dato innvilgelse": [
                  "$innvilgelseTil"
                ],
                "begrunnelseFomDatoInnvilgelse": [
                  "$begrunnelseFomDatoInnvilgelse"
                ],
                "dato": [
                  "$brevdato"
                ],
                "belopOvergangsstonad": [
                  "$belopOvergangsstonad"
                ]
              },
              "valgfelter": {
                  "barnAlder": [
                      {
                          "navn": "barnFodt",
                          "dokumentVariabler":{
                              "flettefelter": { 
                                  "blirEttAarDato": ["12.12.2012"]
                              }
                          }
                      }
                  ]
              },
              "delmalData": {
                "signatur": {
                  "enhet": [
                    "Nav arbeid og ... - OSLO"
                  ],
                  "saksbehandler": [
                    "Saksbehandler Saksbehandlersen"
                  ]
                }
              }
            }
        """.trimIndent()
    }
}