package com.campuslands.tickets.repository;

import com.campuslands.tickets.entity.HistorialEstadoTicket;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialEstadoTicketRepository extends JpaRepository<HistorialEstadoTicket, Long> {

    List<HistorialEstadoTicket> findByTicketIdOrderByFechaCambioAscIdAsc(Long ticketId);
}
