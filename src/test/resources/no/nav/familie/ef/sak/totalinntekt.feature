# language: no
# encoding: UTF-8

Egenskap: Andelhistorikk: Ulike typer inntekt

  Scenario: Skal bruke totalinntekten for andelen

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat |
      | 1            | 01.2022         | 02.2022         | INNVILGE        |

    Og følgende inntekter
      | BehandlingId | Fra og med dato | Inntekt | Dagsats | Månedsinntekt |
      | 1            | 01.2022         | 120000  | 100     | 1400          |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Kildebehandling | Inntekt | Beløp |
      | 01.2022         | 02.2022         | 1               | 162000  | 15870 |

    Så forvent følgende historikk
      | BehandlingId | Fra og med dato | Til og med dato | Endringstype | Endret i behandlingId | Inntekt |
      | 1            | 01.2022         | 02.2022         |              |                       | 162000  |

