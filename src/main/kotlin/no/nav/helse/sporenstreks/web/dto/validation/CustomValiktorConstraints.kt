package no.nav.helse.spion.web.dto.validation

import org.valiktor.Constraint
import org.valiktor.Validator

interface CustomConstraint : Constraint {
    override val messageBundle: String
        get() = "validation/validation-messages"
}


class IdentitetsnummerConstraint : CustomConstraint

fun <E> Validator<E>.Property<String?>.isValidIdentitetsnummer() =
        this.validate(IdentitetsnummerConstraint()) { FoedselsNrValidator.isValid(it) }


class OrganisasjonsnummerConstraint : CustomConstraint

fun <E> Validator<E>.Property<String?>.isValidOrganisasjonsnummer() =
        this.validate(OrganisasjonsnummerConstraint()) { OrganisasjonsnummerValidator.isValid(it) }