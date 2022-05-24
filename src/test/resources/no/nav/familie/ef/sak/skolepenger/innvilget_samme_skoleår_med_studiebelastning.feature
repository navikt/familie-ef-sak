# language: no
# encoding: UTF-8

Egenskap: Skolepenger med studiebelastning

    # TODO hvordan virker studiebelastning? Beregnes stønaden basert på utgifter først, og sen dras det av for studiebelastning?
  Scenario: En utgift på ett skoleår over grensen, med redusert studiebelastning, skal gi redusert beløp (TODO ?)

    Gitt følgende behandlinger for skolepenger
      | BehandlingId | Behandlingstype       |
      | 1            | FØRSTEGANGSBEHANDLING |

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 30               | 100_000  |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 08.2021         | 08.2021         | 100   | 1               |

  Scenario: To utgifter på ett skoleår, over grensen på første utgiften men med studiebelastning - får ikke noe utbetalt for den andre utgiften

    Gitt følgende behandlinger for skolepenger
      | BehandlingId | Behandlingstype       |
      | 1            | FØRSTEGANGSBEHANDLING |

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 30               | 100_000  |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 10.2021         | 06.2022         | 100              | 10_000   |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 08.2021         | 08.2021         | 100   | 1               |

  Scenario: To utgifter på ett skoleår, over grensen på andre utgiften men med studiebelastning, får ikke noe utbetalt for den andre utgiften

    Gitt følgende behandlinger for skolepenger
      | BehandlingId | Behandlingstype       |
      | 1            | FØRSTEGANGSBEHANDLING |

    Gitt følgende vedtak for skolepenger
      | BehandlingId | Vedtaksresultat | Studietype           | Fra og med dato | Til og med dato | Studiebelastning | Utgifter |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 08.2021         | 06.2022         | 100              | 10_000   |
      | 1            | INNVILGE        | HØGSKOLE_UNIVERSITET | 10.2021         | 06.2022         | 30               | 100_000  |

    Når beregner ytelse

    Så forvent følgende andeler lagret for behandling med id: 1
      | Fra og med dato | Til og med dato | Beløp | Kildebehandling |
      | 08.2021         | 08.2021         | 100   | 1               |