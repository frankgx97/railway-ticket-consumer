package cn.guoduhao.TicketSystemConsumer.Repositories.MongoDbRepositories;

import cn.guoduhao.TicketSystemConsumer.Models.Ticket;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TicketMongoRepository extends MongoRepository<Ticket, String> {
}

