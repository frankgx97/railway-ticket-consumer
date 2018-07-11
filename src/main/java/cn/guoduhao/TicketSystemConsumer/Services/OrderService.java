package cn.guoduhao.TicketSystemConsumer.Services;

import cn.guoduhao.TicketSystemConsumer.Repositories.TrainRepository;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.hibernate.service.spi.InjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import cn.guoduhao.TicketSystemConsumer.Models.Ticket;
import cn.guoduhao.TicketSystemConsumer.Models.Train;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@EnableJms
public class OrderService {

    @Autowired
    TrainRepository trainRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private Logger logger;

    public OrderService(){
        this.logger = LoggerFactory.getLogger(OrderService.class);
    }

    public Integer updateTrainDb(Integer id){
        Optional<Train> train = trainRepository.findOneById(id);
        //TODO 加锁
        Integer seatsTotal = train.get().seatsTotal;
        Integer seatsSold = train.get().seatsSold;
        if(seatsSold <= seatsTotal){
            //TODO 判断抢票是否成功
            train.get().seatsSold = seatsSold + 1;
            trainRepository.save(train.get());
            this.logger.info("Train update success.");
            return 0;
        }else{
            this.logger.info("Train update failed.");
            return -1;
        }
    }

    public void writeRedis(String ticketId, String userId, Integer trainId,String json){
        if (json.equals("")){
            this.logger.error("GOT_NULL_MESSAGE.");
        }
        String key = ticketId+"--"+userId+"--"+trainId;
        try{
            this.stringRedisTemplate.opsForValue().set(key, json);
        }catch(Exception e){
            this.logger.error(e.getMessage());
        }
    }

    @JmsListener(destination = "orders")
    public void receive(String message) {
        //这里的返回值必须为void，否则需要Message存在replyto
        //从ActiveMQ接收消息
        this.logger.info("Received message: " + message);
        ObjectMapper mapper = new ObjectMapper();
        try{
            Ticket ticket = mapper.readValue(message, Ticket.class);
            if (updateTrainDb(ticket.trainId) == 0){
                String ticketId = ticket.id;
                //write redis
                String ticketJson = this.updateTicketStatus(ticket);
                this.writeRedis(ticketId, ticket.userId, ticket.trainId, ticketJson);
            }
        }catch(JsonProcessingException jsonProcessingException) {
            this.logger.error("jsonProcessingException");
            this.logger.error(jsonProcessingException.getMessage());
        }catch(IOException ioException) {
            this.logger.error("IOException");
            this.logger.error(ioException.getMessage());
        }
    }

    private String updateTicketStatus(Ticket ticket){
        ObjectMapper mapper = new ObjectMapper();
        try{
            ticket.status = 1;
            return mapper.writeValueAsString(ticket);
        }catch(JsonProcessingException jsonProcessingException) {
            this.logger.error("jsonProcessingException");
            this.logger.error(jsonProcessingException.getMessage());
            return "";
        }
    }

    public List<Ticket> findTicketFromTrainId(String trainId){
        Set<String> keys = this.stringRedisTemplate.keys("*");
        List<String> jsonList = this.stringRedisTemplate.opsForValue().multiGet(keys);

        List<Ticket> ticketList = new ArrayList<>();
        for(int i=0;i<jsonList.size();i++){
            ObjectMapper mapper = new ObjectMapper();
            try{
                Ticket ticket = mapper.readValue(jsonList.get(i), Ticket.class);
                ticketList.add(ticket);
            }catch(Exception e){
                System.out.println(e.getMessage());
            }
        }
        return ticketList;

    }
    /*
    private class RedisKey{
        String userId;
        String ticketId;
        Integer trainId;

        public RedisKey(String userId, String ticketId, Integer trainId){
            this.userId = userId;
            this.ticketId = ticketId;
            this.trainId = trainId;
        }
    }
    */
}

