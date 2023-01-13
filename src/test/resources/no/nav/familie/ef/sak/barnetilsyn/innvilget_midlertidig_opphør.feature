# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Enkel innvilget behandling av typen barnetilsyn

  Scenario: Innvilgede perioder for barnetilsyn skal opphøres dersom

    Gitt følgende vedtak for barnetilsyn
      | Periodetype BT | Aktivitetstype BT | BehandlingId | Vedtaksresultat | Antall barn | Utgifter | Fra og med dato | Til og med dato | Arbeid aktivitet          | Er midlertidig opphør |
      | ordinær        | i_arbeid          | 1            | INNVILGE        | 1           | 200      | 01.2021         | 03.2021         | ETABLERER_EGEN_VIRKSOMHET |                       |
      | ordinær        | i_arbeid          | 2            | INNVILGE        |             | 0        | 02.2021         | 02.2021         | ETABLERER_EGEN_VIRKSOMHET | Ja                    |
      | ordinær        | i_arbeid          | 2            | INNVILGE        | 1           | 100      | 03.2021         | 03.2021         | ETABLERER_EGEN_VIRKSOMHET |                       |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Kontantstøtte | Tilleggsstønad | Antall barn | Utgifter | Beløp | Arbeid aktivitet          |
      | 1            | 01.2021         | 01.2021         | SPLITTET     | 2                     |               |                | 1           | 200      | 128   | ETABLERER_EGEN_VIRKSOMHET |
      | 1            | 02.2021         | 03.2021         | FJERNET      | 2                     |               |                | 1           | 200      | 128   | ETABLERER_EGEN_VIRKSOMHET |
      | 2            | 03.2021         | 03.2021         |              |                       |               |                | 1           | 100      | 64    | ETABLERER_EGEN_VIRKSOMHET |

    Så forvent følgende andeler lagret for behandling med id: 2
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 01.2021         | 01.2021         | 128   | 1               |
      | 03.2021         | 03.2021         | 64    | 2               |

  Scenario: Førstegangsbehandling med midlertidlig opphør

    Gitt følgende vedtak for barnetilsyn
      | Periodetype BT | Aktivitetstype BT | BehandlingId | Vedtaksresultat | Antall barn | Utgifter | Fra og med dato | Til og med dato | Arbeid aktivitet          | Er midlertidig opphør |
      | ordinær        | i_arbeid          | 1            | INNVILGE        | 1           | 200      | 01.2021         | 01.2021         | ETABLERER_EGEN_VIRKSOMHET |                       |
      | ordinær        | i_arbeid          | 1            | INNVILGE        |             | 0        | 02.2021         | 02.2021         | ETABLERER_EGEN_VIRKSOMHET | Ja                    |
      | ordinær        | i_arbeid          | 1            | INNVILGE        | 1           | 200      | 03.2021         | 03.2021         | ETABLERER_EGEN_VIRKSOMHET |                       |

    Når beregner ytelse

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Kontantstøtte | Tilleggsstønad | Antall barn | Utgifter | Beløp | Arbeid aktivitet          |
      | 1            | 01.2021         | 01.2021         |              |                       |               |                | 1           | 200      | 128   | ETABLERER_EGEN_VIRKSOMHET |
      | 1            | 03.2021         | 03.2021         |              |                       |               |                | 1           | 200      | 128   | ETABLERER_EGEN_VIRKSOMHET |

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 01.2021         | 01.2021         | 128   | 1               |
      | 03.2021         | 03.2021         | 128   | 1               |