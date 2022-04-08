# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Enkel innvilget behandling av typen barnetilsyn

  Scenario: Innvilget barnetilsyn skal gi informasjon om antall barn, utgifter, kontantstøtte

    Gitt følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Antall barn | Utgifter |
      | 1            | INNVILGE        | 1           | 200      |

    Og følgende kontantstøtte
      | BehandlingId | Beløp |
      | 1            | 10    |

    Og følgende tillegsstønad
      | BehandlingId | Beløp |
      | 1            | 15    |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Inntekt | Aktivitet          |
      | 1            | ERSTATTET    | 2                     | 0       | BARN_UNDER_ETT_ÅR  |
      | 2            |              |                       | 1       | FORSØRGER_I_ARBEID |

