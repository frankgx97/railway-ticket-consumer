package cn.guoduhao.TicketSystemConsumer;

import cn.guoduhao.TicketSystemConsumer.Models.Ticket;
import cn.guoduhao.TicketSystemConsumer.Repositories.MongoDbRepositories.TicketMongoRepository;
import cn.guoduhao.TicketSystemConsumer.Services.OrderService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import org.springframework.data.redis.core.StringRedisTemplate;

import cn.guoduhao.TicketSystemConsumer.Schedules.HistoryOrderTask;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class HistoryOrderTaskTests {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    TicketMongoRepository ticketMongoRepository;

    @Autowired
    OrderService orderService;

    @Test
    public void testHistoryOrderTask(){
        HistoryOrderTask historyOrderTask = new HistoryOrderTask();
        historyOrderTask.stringRedisTemplate = this.stringRedisTemplate;
        historyOrderTask.ticketMongoRepository = this.ticketMongoRepository;
        historyOrderTask.migrateHistoryOrderToMongo();
    }

    @Test
    @Bean
    public void testRedisTask(){
        List<Ticket>tickets = orderService.findTicketFromTrainId(110);
        System.out.println(tickets.size());
        //System.out.println(tickets.get(0).stations);
    }
}
