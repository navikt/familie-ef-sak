# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Endring i inntekt

  Scenario: Inntekt endrer seg i ny behandling og det vil bli en rad merket med erstattet i historikk

    Gitt følgende vedtak
      | BehandlingId | Vedtaksresultat |
      | 1            | INNVILGE        |
      | 2            | INNVILGE        |

    Og følgende inntekter
      | BehandlingId | Inntekt |
      | 1            | 0       |
      | 2            | 1       |

    Når lag andelhistorikk kjøres

    Så forvent følgende historikk
      | BehandlingId | Endringstype | Endret i behandlingId | Inntekt |
      | 1            | ERSTATTET    | 2                     | 0       |
      | 2            |              |                       | 1       |

