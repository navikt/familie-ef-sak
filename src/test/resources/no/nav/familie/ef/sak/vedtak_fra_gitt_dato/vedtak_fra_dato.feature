# language: no
# encoding: UTF-8

Egenskap: hentVedtakForOvergangsstønadFraDato

  Scenario: Enkel behandling med 1 periode

    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Aktivitet         | Vedtaksperiode |
      | 1            | 01.2021         | 03.2021         | INNVILGE        | BARN_UNDER_ETT_ÅR | HOVEDPERIODE   |

    Og følgende inntekter
      | BehandlingId | Fra og med dato | Inntekt | Samordningsfradrag |
      | 1            | 01.2021         | 10      | 20                 |

    Når lag andelhistorikk kjøres

    # vedtaksperioder
    Så forvent følgende vedtaksperioder fra dato: 11.2020
      | Fra og med dato | Til og med dato | Aktivitet         | Vedtaksperiode |
      | 01.2021         | 03.2021         | BARN_UNDER_ETT_ÅR | HOVEDPERIODE   |

    Så forvent følgende vedtaksperioder fra dato: 01.2021
      | Fra og med dato | Til og med dato | Aktivitet         | Vedtaksperiode |
      | 01.2021         | 03.2021         | BARN_UNDER_ETT_ÅR | HOVEDPERIODE   |

    Så forvent følgende vedtaksperioder fra dato: 02.2021
      | Fra og med dato | Til og med dato | Aktivitet         | Vedtaksperiode |
      | 02.2021         | 03.2021         | BARN_UNDER_ETT_ÅR | HOVEDPERIODE   |

    Så forvent følgende vedtaksperioder fra dato: 03.2021
      | Fra og med dato | Til og med dato | Aktivitet         | Vedtaksperiode |
      | 03.2021         | 03.2021         | BARN_UNDER_ETT_ÅR | HOVEDPERIODE   |

    Så forvent følgende vedtaksperioder fra dato: 04.2021
      | Fra og med dato | Til og med dato | Aktivitet | Vedtaksperiode |

    # inntektsperioder
    Så forvent følgende inntektsperioder fra dato: 11.2020
      | Fra og med dato | Inntekt | Samordningsfradrag |
      | 01.2021         | 10      | 20                 |

    Så forvent følgende inntektsperioder fra dato: 01.2021
      | Fra og med dato | Inntekt | Samordningsfradrag |
      | 01.2021         | 10      | 20                 |

    Så forvent følgende inntektsperioder fra dato: 02.2021
      | Fra og med dato | Inntekt | Samordningsfradrag |
      | 02.2021         | 10      | 20                 |

    Så forvent følgende inntektsperioder fra dato: 03.2021
      | Fra og med dato | Inntekt | Samordningsfradrag |
      | 03.2021         | 10      | 20                 |

    Så forvent følgende inntektsperioder fra dato: 04.2021
      | Fra og med dato | Inntekt | Samordningsfradrag |


  Scenario: Behandling med oppsplittet historikk
    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Aktivitet            | Vedtaksperiode |
      | 1            | 01.2021         | 03.2021         | INNVILGE        | BARN_UNDER_ETT_ÅR    | HOVEDPERIODE   |
      | 2            | 02.2021         | 05.2021         | INNVILGE        | FORSØRGER_I_ARBEID   | UTVIDELSE      |
      | 3            | 04.2021         | 04.2021         | SANKSJONERE     | IKKE_AKTIVITETSPLIKT | SANKSJON       |

    Og følgende inntekter
      | BehandlingId | Fra og med dato | Inntekt | Samordningsfradrag |
      | 1            | 01.2021         | 10      | 20                 |
      | 2            | 02.2021         | 20      | 10                 |

    Når lag andelhistorikk kjøres

    Så forvent følgende vedtaksperioder fra dato: 01.2021
      | Fra og med dato | Til og med dato | Aktivitet            | Vedtaksperiode |
      | 01.2021         | 01.2021         | BARN_UNDER_ETT_ÅR    | HOVEDPERIODE   |
      | 02.2021         | 03.2021         | FORSØRGER_I_ARBEID   | UTVIDELSE      |
      | 04.2021         | 04.2021         | IKKE_AKTIVITETSPLIKT | SANKSJON       |
      | 05.2021         | 05.2021         | FORSØRGER_I_ARBEID   | UTVIDELSE      |

    Så forvent følgende vedtaksperioder fra dato: 04.2021
      | Fra og med dato | Til og med dato | Aktivitet            | Vedtaksperiode |
      | 04.2021         | 04.2021         | IKKE_AKTIVITETSPLIKT | SANKSJON       |
      | 05.2021         | 05.2021         | FORSØRGER_I_ARBEID   | UTVIDELSE      |

    Så forvent følgende inntektsperioder fra dato: 01.2021
      | Fra og med dato | Inntekt | Samordningsfradrag |
      | 01.2021         | 10      | 20                 |
      | 02.2021         | 20      | 10                 |

    Så forvent følgende inntektsperioder fra dato: 04.2021
      | Fra og med dato | Inntekt | Samordningsfradrag |
      | 04.2021         | 20      | 10                 |

    # Hvordan skal inntekt håndteres når første perioden er sanksjon?

  Scenario: Behandling med dobbel sanksjon skal ikke slå sammen sanksjoner
    Gitt følgende vedtak
      | BehandlingId | Fra og med dato | Til og med dato | Vedtaksresultat | Aktivitet            | Vedtaksperiode |
      | 1            | 01.2021         | 05.2021         | INNVILGE        | BARN_UNDER_ETT_ÅR    | HOVEDPERIODE   |
      | 2            | 03.2021         | 03.2021         | SANKSJONERE     | IKKE_AKTIVITETSPLIKT | SANKSJON       |
      | 3            | 04.2021         | 04.2021         | SANKSJONERE     | IKKE_AKTIVITETSPLIKT | SANKSJON       |

    Og følgende inntekter
      | BehandlingId | Fra og med dato | Inntekt | Samordningsfradrag |
      | 1            | 01.2021         | 10      | 20                 |

    Når lag andelhistorikk kjøres

    Så forvent følgende vedtaksperioder fra dato: 01.2021
      | Fra og med dato | Til og med dato | Aktivitet            | Vedtaksperiode |
      | 01.2021         | 02.2021         | BARN_UNDER_ETT_ÅR    | HOVEDPERIODE   |
      | 03.2021         | 03.2021         | IKKE_AKTIVITETSPLIKT | SANKSJON       |
      | 04.2021         | 04.2021         | IKKE_AKTIVITETSPLIKT | SANKSJON       |
      | 05.2021         | 05.2021         | BARN_UNDER_ETT_ÅR    | HOVEDPERIODE   |

    # fra sanksjon skal bruke verdi fra tidligere inntekt
    Så forvent følgende inntektsperioder fra dato: 04.2021
      | Fra og med dato | Inntekt | Samordningsfradrag |
      | 04.2021         | 10      | 20                 |


    # TODO hva skjer hvis vi revurderer bak i tiden, der personen ikke har noen periode? Eks 2 måneder før tidligere vedtak