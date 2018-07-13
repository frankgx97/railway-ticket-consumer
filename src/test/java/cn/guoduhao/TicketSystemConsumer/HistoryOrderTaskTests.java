package cn.guoduhao.TicketSystemConsumer;

import cn.guoduhao.TicketSystemConsumer.Repositories.MongoDbRepositories.TicketMongoRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import org.springframework.data.redis.core.StringRedisTemplate;

import cn.guoduhao.TicketSystemConsumer.Schedules.HistoryOrderTask;

@RunWith(SpringRunner.class)
@SpringBootTest
public class HistoryOrderTaskTests {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    TicketMongoRepository ticketMongoRepository;

    @Test
    public void testHistoryOrderTask(){
        HistoryOrderTask historyOrderTask = new HistoryOrderTask();
        historyOrderTask.stringRedisTemplate = this.stringRedisTemplate;
        historyOrderTask.ticketMongoRepository = this.ticketMongoRepository;
        historyOrderTask.migrateHistoryOrderToMongo();
    }
}
