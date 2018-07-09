package cn.guoduhao.TicketSystemConsumer.Services;

import cn.guoduhao.TicketSystemConsumer.Repositories.TrainRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import cn.guoduhao.TicketSystemConsumer.Models.Ticket;
import cn.guoduhao.TicketSystemConsumer.Models.Train;

import java.util.Optional;

@Service
@EnableJms
public class OrderService {

    @Autowired
    TrainRepository trainRepository;

    public void getQueue(){


    }

    public void updateTrainDb(Integer id){
        Optional<Train> train = trainRepository.findOneById(id);

    }

    public void writeRedis(){

    }

    @JmsListener(destination = "orders")
    public Integer receive(String message) {
        System.out.println("Received message: " + message);
        ObjectMapper mapper = new ObjectMapper();
        try{
            Ticket ticket = mapper.readValue(message, Ticket.class);
            updateTrainDb(ticket.id);
            return 0;
        }catch(Exception e){
            System.out.println(e.getMessage());
            return 1;
        }
    }
}
