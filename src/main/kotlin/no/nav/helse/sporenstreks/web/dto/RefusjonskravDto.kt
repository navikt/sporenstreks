package no.nav.helse.sporenstreks.web.dto

import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode.Companion.maksOppholdMellomPerioder
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode.Companion.maksimalAGPLengde
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode.Companion.refusjonFraDatoGammelPeriode
import no.nav.helse.sporenstreks.web.dto.validation.*
import org.valiktor.functions.isGreaterThanOrEqualTo
import org.valiktor.functions.isLessThanOrEqualTo
import org.valiktor.functions.isPositiveOrZero
import org.valiktor.functions.validateForEach
import org.valiktor.validate
import java.time.LocalDate

data class RefusjonskravDto(
    val identitetsnummer: String,
    val virksomhetsnummer: String,
    val perioder: Set<Arbeidsgiverperiode>
) {

    init {
        validate(this) {
            validate(RefusjonskravDto::identitetsnummer).isValidIdentitetsnummer()
            validate(RefusjonskravDto::virksomhetsnummer).isValidOrganisasjonsnummer()

            validate(RefusjonskravDto::perioder).validateForEach {
                validate(Arbeidsgiverperiode::beloep).isPositiveOrZero()
                validate(Arbeidsgiverperiode::beloep).isLessThanOrEqualTo(1_000_000.0)
                validate(Arbeidsgiverperiode::antallDagerMedRefusjon).isPositiveOrZero()
            }

            // Det kan ikke kreves refusjon fra og med 1. oktober 2021 til og med 30. november 2021 eller for lenger enn 6 måneder siden.
            validate(RefusjonskravDto::perioder).refusjonsdatoIkkeiGjenåpning()

            validate(RefusjonskravDto::perioder).validateForEach {
                validate(Arbeidsgiverperiode::tom).isGreaterThanOrEqualTo(it.fom)
                validate(Arbeidsgiverperiode::tom).isLessThanOrEqualTo(LocalDate.now())
            }

            // antall refusjonsdager kan ikke være lenger enn periodens lengde
            validate(RefusjonskravDto::perioder).refujonsDagerIkkeOverstigerPeriodelengder()

            // kan ikke kreve refusjon for dager før 16. mars 2020
            validate(RefusjonskravDto::perioder).refusjonsdagerInnenforGyldigPeriode(refusjonFraDatoGammelPeriode)

            // Summen av antallDagerMedRefusjon kan ikke overstige total periodelengde - 3 dager
            validate(RefusjonskravDto::perioder).arbeidsgiverBetalerForDager(refusjonFraDatoGammelPeriode)

            // opphold mellom periodene kan ikke overstige 16 dager
            validate(RefusjonskravDto::perioder).harMaksimaltOppholdMellomPerioder(maksOppholdMellomPerioder)

            // periodene kan ikke overlappe
            validate(RefusjonskravDto::perioder).harIngenOverlappendePerioder()

            // periodene til sammen kan ikke overstige 16
            validate(RefusjonskravDto::perioder).totalPeriodeLengdeErMaks(maksimalAGPLengde)

            // tom periode kan ikke ha refusjonsbeløp
            validate(RefusjonskravDto::perioder).tomPeriodeKanIkkeHaBeloepConstraint()
        }
    }
}
