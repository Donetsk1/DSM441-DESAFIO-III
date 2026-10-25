package com.example.desafio_iii.data

data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val genre: String = "",
    val totalPages: Int = 0,
    val pagesRead: Int = 0,
    val status: String = BookStatus.POR_LEER,
    val rating: Int = 0,
    val finishedAt: Long? = null,
    val createdAt: Long = 0L
)

fun Book.progressPercent(): Int {
    if (totalPages <= 0) return 0
    return ((pagesRead.coerceIn(0, totalPages) * 100) / totalPages)
}

object BookStatus {
    const val POR_LEER = "Por leer"
    const val LEYENDO = "Leyendo"
    const val TERMINADO = "Terminado"

    val ALL = listOf(POR_LEER, LEYENDO, TERMINADO)
}

object BookGenre {
    const val NOVELA = "Novela"
    const val CIENCIA_FICCION = "Ciencia ficción"
    const val HISTORIA = "Historia"
    const val DESARROLLO_PERSONAL = "Desarrollo personal"

    val ALL = listOf(NOVELA, CIENCIA_FICCION, HISTORIA, DESARROLLO_PERSONAL)
}
