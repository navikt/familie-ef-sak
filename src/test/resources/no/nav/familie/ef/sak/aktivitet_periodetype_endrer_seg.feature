# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Endring i aktivitet

  Scenario: Endring i aktivitet fører til erstattet i Andelhistorikk

    Gitt følgende vedtak
      | BehandlingId | Vedtaksresultat | Aktivitet          |
      | 1            | INNVILGE        | BARN_UNDER_ETT_ÅR  |
      | 2            | INNVILGE        | FORSØRGER_I_ARBEID |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Aktivitet          |
      | 1            | ERSTATTET    | 2                     | BARN_UNDER_ETT_ÅR  |
      | 2            |              |                       | FORSØRGER_I_ARBEID |

