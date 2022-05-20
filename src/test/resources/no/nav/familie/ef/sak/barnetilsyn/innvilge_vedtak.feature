# language: no
# encoding: UTF-8

Egenskap: Beregn ytelse steg for innvilget vedtak for barnetilsyn

  Scenario: Innvilget førstegangsbehandling med en periode uten reduksjon

    Og følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Antall barn | Utgifter | Arbeid aktivitet          | Fra og med dato | Til og med dato |
      | 1            | INNVILGE        | 1           | 200      | ETABLERER_EGEN_VIRKSOMHET | 01.2021         | 03.2021         |

    Når vedtak vedtas

    Så forvent følgende andeler lagret for behandling med id: 1
      | Beløp mellom | Fra og med dato | Til og med dato | Kildebehandling |
      | 1-200        | 01.01.2021      | 31.03.2021      | 1               |


  Scenario: Innvilget førstegangsbehandling med to perioder uten reduksjon
    Og følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Antall barn | Utgifter | Arbeid aktivitet          | Fra og med dato | Til og med dato |
      | 1            | INNVILGE        | 1           | 200      | ETABLERER_EGEN_VIRKSOMHET | 01.2021         | 03.2021         |
      | 1            | INNVILGE        | 2           | 350      | ETABLERER_EGEN_VIRKSOMHET | 04.2021         | 07.2021         |

    Når vedtak vedtas

    Så forvent følgende andeler lagret for behandling med id: 1
      | Beløp mellom | Fra og med dato | Til og med dato | Kildebehandling |
      | 1-200        | 01.01.2021      | 31.03.2021      | 1               |
      | 1-350        | 01.04.2021      | 31.07.2021      | 1               |


  Scenario: Innvilget førstegangsbehandling med to perioder med reduksjon
    Og følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Antall barn | Utgifter | Arbeid aktivitet          | Fra og med dato | Til og med dato |
      | 1            | INNVILGE        | 1           | 200      | ETABLERER_EGEN_VIRKSOMHET | 01.2021         | 03.2021         |
      | 1            | INNVILGE        | 2           | 350      | ETABLERER_EGEN_VIRKSOMHET | 04.2021         | 07.2021         |

    Og følgende tilleggsstønad
      | BehandlingId | Beløp | Fra og med dato | Til og med dato |
      | 1            | 100   | 01.2021         | 01.2021         |

    Når vedtak vedtas

    Så forvent følgende andeler lagret for behandling med id: 1
      | Beløp mellom | Fra og med dato | Til og med dato | Kildebehandling |
      | 1-100        | 01.01.2021      | 31.01.2021      | 1               |
      | 1-200        | 01.02.2021      | 31.03.2021      | 1               |
      | 1-350        | 01.04.2021      | 31.07.2021      | 1               |

  Scenario: Innvilget førstegangsbehandling med to perioder med reduksjon og kontantstøtte
    Og følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Antall barn | Utgifter | Arbeid aktivitet          | Fra og med dato | Til og med dato |
      | 1            | INNVILGE        | 1           | 200      | ETABLERER_EGEN_VIRKSOMHET | 01.2021         | 03.2021         |
      | 1            | INNVILGE        | 2           | 350      | ETABLERER_EGEN_VIRKSOMHET | 04.2021         | 07.2021         |

    Og følgende tilleggsstønad
      | BehandlingId | Beløp | Fra og med dato | Til og med dato |
      | 1            | 100   | 01.2021         | 01.2021         |

    Og følgende kontantstøtte
      | BehandlingId | Beløp | Fra og med dato | Til og med dato |
      | 1            | 50    | 01.2021         | 05.2021         |

    Når vedtak vedtas

    Så forvent følgende andeler lagret for behandling med id: 1
      | Beløp mellom | Fra og med dato | Til og med dato | Kildebehandling |
      | 0-0          | 01.01.2021      | 31.01.2021      | 1               |
      | 1-150        | 01.02.2021      | 31.03.2021      | 1               |
      | 1-300        | 01.04.2021      | 31.05.2021      | 1               |
      | 1-350        | 01.06.2021      | 31.07.2021      | 1               |

  Scenario: Innvilget revurdering
    Og følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Antall barn | Utgifter | Arbeid aktivitet          | Fra og med dato | Til og med dato |
      | 1            | INNVILGE        | 1           | 200      | ETABLERER_EGEN_VIRKSOMHET | 01.2021         | 03.2021         |
      | 2            | INNVILGE        | 2           | 350      | ETABLERER_EGEN_VIRKSOMHET | 02.2021         | 07.2021         |

    Når vedtak vedtas

    Så forvent følgende andeler lagret for behandling med id: 1
      | Beløp mellom | Fra og med dato | Til og med dato | Kildebehandling |
      | 1-150        | 01.01.2021      | 31.03.2021      | 1               |

    Så forvent følgende andeler lagret for behandling med id: 2
      | Beløp mellom | Fra og med dato | Til og med dato | Kildebehandling |
      | 1-150        | 01.01.2021      | 31.01.2021      | 1               |
      | 150-250      | 01.02.2021      | 31.07.2021      | 2               |


  Scenario: Innvilget revurdering med 0-periode
    Og følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Antall barn | Utgifter | Arbeid aktivitet          | Fra og med dato | Til og med dato |
      | 1            | INNVILGE        | 1           | 200      | ETABLERER_EGEN_VIRKSOMHET | 01.2021         | 07.2021         |
      | 2            | INNVILGE        | 2           | 0        | ETABLERER_EGEN_VIRKSOMHET | 02.2021         | 02.2021         |
      | 2            | INNVILGE        | 2           | 350      | ETABLERER_EGEN_VIRKSOMHET | 03.2021         | 07.2021         |

    Når vedtak vedtas

    Så forvent følgende andeler lagret for behandling med id: 1
      | Beløp mellom | Fra og med dato | Til og med dato | Kildebehandling |
      | 1-150        | 01.01.2021      | 31.07.2021      | 1               |

    Så forvent følgende andeler lagret for behandling med id: 2
      | Beløp mellom | Fra og med dato | Til og med dato | Kildebehandling |
      | 1-150        | 01.01.2021      | 31.01.2021      | 1               |
      | 150-250      | 01.03.2021      | 31.07.2021      | 2               |


  Scenario: Innvilget opphør og revurdering
    Og følgende vedtak for barnetilsyn
      | BehandlingId | Vedtaksresultat | Antall barn | Utgifter | Arbeid aktivitet          | Fra og med dato | Til og med dato | Opphørsdato |
      | 1            | INNVILGE        | 1           | 200      | ETABLERER_EGEN_VIRKSOMHET | 01.2021         | 04.2021         |             |
      | 2            | OPPHØRT         |             |          |                           |                 |                 | 02.2021     |
      | 3            | INNVILGE        | 2           | 350      | ETABLERER_EGEN_VIRKSOMHET | 05.2021         | 07.2021         |             |

    Når vedtak vedtas

    Så forvent følgende andeler lagret for behandling med id: 1
      | Beløp mellom | Fra og med dato | Til og med dato | Kildebehandling |
      | 1-150        | 01.01.2021      | 30.04.2021      | 1               |

    Så forvent følgende andeler lagret for behandling med id: 2
      | Beløp mellom | Fra og med dato | Til og med dato | Kildebehandling |
      | 1-150        | 01.01.2021      | 31.01.2021      | 1               |

    Så forvent følgende andeler lagret for behandling med id: 3
      | Beløp mellom | Fra og med dato | Til og med dato | Kildebehandling |
      | 1-150        | 01.01.2021      | 31.01.2021      | 1               |
      | 150-250      | 01.05.2021      | 31.07.2021      | 3               |



