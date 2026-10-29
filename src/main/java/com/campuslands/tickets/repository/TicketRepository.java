package com.campuslands.tickets.repository;

import com.campuslands.tickets.entity.Categoria;
import com.campuslands.tickets.entity.EstadoTicket;
import com.campuslands.tickets.entity.Prioridad;
import com.campuslands.tickets.entity.Ticket;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    @Override
    @EntityGraph(attributePaths = {"usuarioSolicitante", "tecnicoAsignado"})
    Page<Ticket> findAll(Specification<Ticket> especificacion, Pageable paginacion);

    @Query("select t.categoria as valor, count(t) as total from Ticket t group by t.categoria")
    List<ConteoAgrupado<Categoria>> contarPorCategoria();

    @Query("select t.prioridad as valor, count(t) as total from Ticket t group by t.prioridad")
    List<ConteoAgrupado<Prioridad>> contarPorPrioridad();

    @Query("select t.estado as valor, count(t) as total from Ticket t group by t.estado")
    List<ConteoAgrupado<EstadoTicket>> contarPorEstado();
}
