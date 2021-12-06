package no.nav.helse.sporenstreks.web.api.validation

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.assertThrows
import org.valiktor.ConstraintViolationException
import kotlin.reflect.KProperty1

fun <B, A> validationShouldFailFor(field: KProperty1<B, A>, block: () -> Unit): Exception {
    val thrown = assertThrows<ConstraintViolationException>(block)
    Assertions.assertThat(thrown.constraintViolations).hasSize(1)
    Assertions.assertThat(thrown.constraintViolations.first().property).isEqualTo(field.name)
    return thrown
}

fun validationShouldFailFor(propertyPath: String, block: () -> Unit): Exception {
    val thrown = assertThrows<ConstraintViolationException>(block)
    Assertions.assertThat(thrown.constraintViolations).hasSize(1)
    Assertions.assertThat(thrown.constraintViolations.first().property).isEqualTo(propertyPath)
    return thrown
}
