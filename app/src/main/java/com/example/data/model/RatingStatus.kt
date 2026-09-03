package com.example.data.model

/**
 * Representa el estado de calificación de una fotografía.
 */
enum class RatingStatus {
    /** Calificación de 1 a 5 estrellas encontrada. */
    RATED,
    /** Calificación explícita de 0 estrellas o foto sin calificar en cámara. */
    UNRATED,
    /**
     * El formato (por ejemplo, Canon RAW CR3 propietario sin contenedor XMP estándar)
     * o metadatos no permiten leer la calificación de forma fiable.
     */
    UNAVAILABLE,
    /** Error al intentar leer el archivo. */
    ERROR
}
