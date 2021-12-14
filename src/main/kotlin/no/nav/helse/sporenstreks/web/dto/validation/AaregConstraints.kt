package no.nav.helse.sporenstreks.web.api.dto.validation

import io.ktor.util.*
import no.nav.helse.arbeidsgiver.integrasjoner.aareg.Arbeidsforhold
import no.nav.helse.sporenstreks.domene.Refusjonskrav
import no.nav.helse.sporenstreks.web.dto.validation.CustomConstraint
import org.valiktor.ConstraintViolationException
import org.valiktor.DefaultConstraintViolation

class ArbeidsforholdConstraint : CustomConstraint

@KtorExperimentalAPI
fun validerArbeidsforhold(aktuelleArbeidsforhold: List<Arbeidsforhold>, krav: Refusjonskrav) {
    val ansattPerioder = aktuelleArbeidsforhold.map { it.ansettelsesperiode.periode }

    krav.perioder.forEach { kravPeriode ->
        val kravPeriodeSubsettAvAnsPeriode = ansattPerioder.any { ansattPeriode ->
            (ansattPeriode.tom == null || kravPeriode.tom.isBefore(ansattPeriode.tom) || kravPeriode.tom == ansattPeriode.tom) &&
                ansattPeriode.fom!!.isBefore(kravPeriode.fom)
        }

        if (aktuelleArbeidsforhold == null || !kravPeriodeSubsettAvAnsPeriode) {
            throw ConstraintViolationException(
                setOf(
                    DefaultConstraintViolation(
                        "identitetsnummer",
                        constraint = ArbeidsforholdConstraint(),
                        value = krav.virksomhetsnummer
                    )
                )
            )
        }
    }
}
