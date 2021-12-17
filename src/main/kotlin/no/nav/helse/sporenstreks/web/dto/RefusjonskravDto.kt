package no.nav.helse.sporenstreks.web.dto

import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsforhold
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode.Companion.antallMånederTilStengt
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode.Companion.arbeidsgiverBetalerForDager
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode.Companion.arbeidsgiverBetalerForDagerGammelPeriode
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode.Companion.maksOppholdMellomPerioder
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode.Companion.maksimalAGPLengde
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode.Companion.refusjonFraDato
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode.Companion.refusjonFraDatoGammelPeriode
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode.Companion.refusjonTilDato
import no.nav.helse.sporenstreks.domene.Arbeidsgiverperiode.Companion.refusjonTilDatoGammelPeriode
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
    val perioder: Set<Arbeidsgiverperiode>,
    val tariffEndring: Boolean = false
) {
    fun validate(arbeidsforhold: List<Arbeidsforhold>) {
        validate(this) {
            validate(RefusjonskravDto::identitetsnummer).isValidIdentitetsnummer()
            validate(RefusjonskravDto::virksomhetsnummer).isValidOrganisasjonsnummer()
            validate(RefusjonskravDto::perioder).måHaAktivtArbeidsforhold(it, arbeidsforhold)

            validate(RefusjonskravDto::perioder).validateForEach {
                validate(Arbeidsgiverperiode::beloep).isPositiveOrZero()
                validate(Arbeidsgiverperiode::beloep).isLessThanOrEqualTo(1_000_000.0)
                validate(Arbeidsgiverperiode::antallDagerMedRefusjon).isPositiveOrZero()
            }
            if (tariffEndring) {
                // kan ikke kreve refusjon for dager etter gjenåpning 1 oktober 2021
                validate(RefusjonskravDto::perioder).refusjonsdatoIkkeEtterGjenåpning(refusjonTilDatoGammelPeriode)
            } else {
                // kan ikke kreve refusjon for dager etter gjenåpning 30.06.2022
                validate(RefusjonskravDto::perioder).refusjonsdatoIkkeEtterGjenåpning(refusjonTilDato)
                // kan ikke kreve refusjon for dager før tre måneder siden
                validate(RefusjonskravDto::perioder).innenforAntallMåneder(antallMånederTilStengt)
            }

            validate(RefusjonskravDto::perioder).validateForEach {
                validate(Arbeidsgiverperiode::tom).isGreaterThanOrEqualTo(it.fom)
                validate(Arbeidsgiverperiode::tom).isLessThanOrEqualTo(LocalDate.now())
            }

            // antall refusjonsdager kan ikke være lenger enn periodens lengde
            validate(RefusjonskravDto::perioder).refujonsDagerIkkeOverstigerPeriodelengder()

            if (tariffEndring) {
                // kan ikke kreve refusjon for dager før 16. mars 2020
                validate(RefusjonskravDto::perioder).refusjonsdagerInnenforGyldigPeriode(refusjonFraDatoGammelPeriode)

                // Summen av antallDagerMedRefusjon kan ikke overstige total periodelengde - 3 dager
                validate(RefusjonskravDto::perioder).arbeidsgiverBetalerForDager(arbeidsgiverBetalerForDagerGammelPeriode, refusjonFraDatoGammelPeriode)
            } else {
                // kan ikke kreve refusjon for dager før 1. desember 2021
                validate(RefusjonskravDto::perioder).refusjonsdagerInnenforGyldigPeriode(refusjonFraDato)

                // Summen av antallDagerMedRefusjon kan ikke overstige total periodelengde - 5 dager
                validate(RefusjonskravDto::perioder).arbeidsgiverBetalerForDager(arbeidsgiverBetalerForDager, refusjonFraDato)
            }

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
