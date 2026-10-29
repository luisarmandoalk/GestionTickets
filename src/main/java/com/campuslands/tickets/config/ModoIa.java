package com.campuslands.tickets.config;

public enum ModoIa {
    /** Usa el modelo de lenguaje si hay API key; si no, el clasificador por reglas. */
    AUTO,
    /** Siempre el modelo de lenguaje (por ejemplo, Ollama local, que no requiere API key). */
    LLM,
    /** Siempre el clasificador por reglas, sin llamadas externas. */
    REGLAS
}
