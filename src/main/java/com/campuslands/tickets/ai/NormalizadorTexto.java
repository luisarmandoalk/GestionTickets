package com.campuslands.tickets.ai;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

final class NormalizadorTexto {

    private static final Pattern MARCAS_DIACRITICAS = Pattern.compile("\\p{M}+");
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");

    private NormalizadorTexto() {
    }

    /** Minúsculas y sin tildes ni diéresis, para comparar palabras clave sin importar cómo se escribieron. */
    static String paraComparar(String texto) {
        return sinTildes(colapsarEspacios(texto)).toLowerCase(Locale.ROOT);
    }

    static String sinTildes(String texto) {
        return MARCAS_DIACRITICAS.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
    }

    static String colapsarEspacios(String texto) {
        return ESPACIOS.matcher(texto.strip()).replaceAll(" ");
    }

    static int contarPalabras(String texto) {
        String limpio = colapsarEspacios(texto);
        return limpio.isEmpty() ? 0 : limpio.split(" ").length;
    }

    static String limitarPalabras(String texto, int maximo) {
        String[] palabras = colapsarEspacios(texto).split(" ");
        if (palabras.length <= maximo) {
            return String.join(" ", palabras);
        }
        return String.join(" ", Arrays.copyOf(palabras, maximo)).replaceAll("[,;:]+$", "") + "...";
    }
}
