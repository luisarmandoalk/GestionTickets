package com.campuslands.tickets.repository;

import com.campuslands.tickets.entity.AuditoriaTicket;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaTicketRepository extends JpaRepository<AuditoriaTicket, Long> {

    @EntityGraph(attributePaths = "usuario")
    List<AuditoriaTicket> findByTicketIdOrderByFechaAscIdAsc(Long ticketId);
}
