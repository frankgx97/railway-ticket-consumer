package cn.guoduhao.TicketSystemConsumer.Schedules;

import cn.guoduhao.TicketSystemConsumer.Models.Ticket;
import cn.guoduhao.TicketSystemConsumer.Repositories.MongoDbRepositories.TicketMongoRepository;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Set;

@Component
public class HistoryOrderTask {

    @Autowired
    public StringRedisTemplate stringRedisTemplate;

    @Autowired
    public TicketMongoRepository ticketMongoRepository;

    private Logger logger;

    public HistoryOrderTask(){
        this.logger = LoggerFactory.getLogger(HistoryOrderTask.class);
    }

    @Scheduled(fixedRate = 3600000)
    public void migrateHistoryOrderToMongo(){
        Set<String> keys = this.stringRedisTemplate.keys("*trainId*");
        List<String> jsonList = this.stringRedisTemplate.opsForValue().multiGet(keys);

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        Long currentTimestamp = timestamp.getTime()/1000;

        this.logger.info(Integer.toString(jsonList.size()));

        for(int i=0;i<jsonList.size();i++){
            try{
                ObjectMapper mapper = new ObjectMapper();
                Ticket ticket = mapper.readValue(jsonList.get(i), Ticket.class);
                if(ticket.timestamp < currentTimestamp - 86400){
                    //rm redis and add mongo
                    this.logger.info("Detected legacy ticket: " + ticket.id);
                    ticketMongoRepository.save(ticket);
                    removeFromRedis(ticket.id);
                }
            }catch(IOException e){
                this.logger.error(e.getMessage());
            }
        }
    }
    private void removeFromRedis(String ticketId){
        Set<String> keys = this.stringRedisTemplate.keys(ticketId+"*");
        this.stringRedisTemplate.delete(keys);
        this.logger.info("Key removed");
    }
}
