package com.campuslands.tickets.repository;

/** Proyección para los conteos de tickets agrupados por un campo enumerado. */
public interface ConteoAgrupado<E extends Enum<E>> {

    E getValor();

    Long getTotal();
}
