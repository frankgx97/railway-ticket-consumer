package cn.guoduhao.TicketSystemConsumer.Repositories;

import cn.guoduhao.TicketSystemConsumer.Models.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {

}


