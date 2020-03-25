package no.nav.helse.sporenstreks.web.dto

import no.nav.helse.sporenstreks.web.dto.validation.*
import org.valiktor.functions.isGreaterThanOrEqualTo
import org.valiktor.functions.isPositive
import org.valiktor.functions.validateForEach
import org.valiktor.validate
import java.time.LocalDate

data class Arbeidsgiverperiode(
        val fom: LocalDate,
        val tom: LocalDate,
        val antallDagerMedRefusjon: Int
)

data class RefusjonskravDto(
        val identitetsnummer: String,
        val virksomhetsnummer: String,
        val perioder: Set<Arbeidsgiverperiode>,
        val beløp: Double
) {

    init {
        validate(this) {
            validate(RefusjonskravDto::identitetsnummer).isValidIdentitetsnummer()
            validate(RefusjonskravDto::virksomhetsnummer).isValidOrganisasjonsnummer()
            validate(RefusjonskravDto::beløp).isPositive()

            validate(RefusjonskravDto::perioder).validateForEach {
                validate(Arbeidsgiverperiode::tom).isGreaterThanOrEqualTo(it.fom)
            }

            // antall refusjonsdager kan ikke vøre lenger enn periodens lengde
            validate(RefusjonskravDto::perioder).refujonsDagerIkkeOverstigerPeriodelengder()

            // Summen av antallDagerMedRefusjon kan ikke overstige 13 dager
            validate(RefusjonskravDto::perioder).sumAntallDagerMindreEnnEllerLik(13)

            // opphold mellom periodene kan ikke overstige 16 dager
            validate(RefusjonskravDto::perioder).harMaksimaltOppholdMellomPerioder(16)

            // periodene kan ikke overlappe
            validate(RefusjonskravDto::perioder).harIngenOverlappendePerioder()

            // periodene til sammen kan ikke overstige 16
            validate(RefusjonskravDto::perioder).totalPeriodeLengdeErMaks(16)
        }
    }
}