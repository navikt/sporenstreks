package no.nav.helse.sporenstreks.excel

class ExcelFileParsingException(message: String, val errors: Set<ExcelFileRowError> = emptySet()) : Exception(message)

class CellValueExtractionException(val columnName: String, message: String, cause: Exception? = null) : Exception(message, cause)

data class ExcelFileRowError(
    val rowNumber: Int,
    val column: String,
    val message: String
)
