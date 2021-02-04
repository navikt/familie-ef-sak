package no.nav.familie.ef.sak.api.dto

data class BrevRequest(val navn: String, val ident: String) {

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
            "dato": [
                "01.01.1986"
            ]
        }
            }
        """.trimIndent()


    }
}