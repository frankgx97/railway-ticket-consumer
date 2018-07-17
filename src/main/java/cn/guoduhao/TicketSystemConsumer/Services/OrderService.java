package cn.guoduhao.TicketSystemConsumer.Services;

import cn.guoduhao.TicketSystemConsumer.Repositories.TrainRepository;
import cn.guoduhao.TicketSystemConsumer.Services.ticket.TicketService;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.hibernate.service.spi.InjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import cn.guoduhao.TicketSystemConsumer.Models.Ticket;
import cn.guoduhao.TicketSystemConsumer.Models.Train;
import cn.guoduhao.TicketSystemConsumer.Models.TrainStationMap;

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

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private TicketService ticketService;

    private Logger logger;

    public OrderService(){
        this.logger = LoggerFactory.getLogger(OrderService.class);
    }

    public Integer updateTrainDb(Integer id){
        //这里只判断余票，不更新库存
        Optional<Train> train = trainRepository.findOneById(id);
        Integer seatsTotal = train.get().seatsTotal;
        Integer seatsSold = train.get().seatsSold;
        if(seatsSold < seatsTotal){
            this.logger.info("Have tickets left.");
            //train.get().seatsSold = seatsSold + 1;
            //trainRepository.save(train.get());
            //this.logger.info("Train update success.");
            return 0;
        }else{
            this.logger.info("No tickets left");
            return -1;
        }
    }

    public void writeRedis(String ticketId, String userId, Integer trainId,String json){
        //通过trainid查找改为通过trainno
        if (json.equals("")){
            this.logger.error("GOT_NULL_MESSAGE.");
        }
        String key = ticketId+"--"+userId+"--trainId"+trainId;
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
            if (updateTrainDb(ticket.trainId) == 0){//这里只判断余票，不更新库存
                String ticketId = ticket.id;

                //创建标准字段"0000000000"
                String trainNo = ticket.trainNo;
                String receivedDepartStation = ticket.departStation;
                String receivedDestinationStation = ticket.destinationStation;
                //修改成购票状态相应的01串stations
                ticket.stations = ticketService.modifyStations(receivedDepartStation,
                        receivedDestinationStation,trainNo);
                ticketService.buyTicket(ticket);

                //write redis
                //在函数中已经updateRedis了
//                String ticketJson = this.updateTicketStatus(ticket);
//                this.writeRedis(ticketId, ticket.userId, ticket.trainId, ticketJson);
            }
        }catch(JsonProcessingException jsonProcessingException) {
            this.logger.error("jsonProcessingException");
            this.logger.error(jsonProcessingException.getMessage());
        }catch(IOException ioException) {
            this.logger.error("IOException");
            this.logger.error(ioException.getMessage());
        }
    }

    public String updateTicketStatus(Ticket ticket){
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

    public List<Ticket> findTicketFromTrainId(Integer trainId){
        Set<String> keys = this.stringRedisTemplate.keys("*trainId"+Integer.toString(trainId));
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


    //车站信息的相关操作
    //query使用Criteria.where("stations").is("北京")这种格式制定键值对，在上层函数中使用
    //返回一个TrainStationMap对象
    private TrainStationMap findOne(Query query, String collectionName){
        return mongoTemplate.findOne(query,TrainStationMap.class,collectionName);
    }

    // 返回一个List<TrainStationMap>
    private List<TrainStationMap> find(Query query,String collectionName){
        return mongoTemplate.find(query,TrainStationMap.class,collectionName);
    }

    //输入上车站和下车站，返回trainNo
    public List<TrainStationMap> findAllByDepartStaitonAndDestinationStation(String departStation,String destinationStation){
        Query query = new Query();
        query.addCriteria(
                Criteria.where("stations").exists(true).andOperator(
                        Criteria.where("stations").is(departStation),
                        Criteria.where("stations").is(destinationStation)
                )
        );
        //System.out.println("query - " + query.toString());
        return this.find(query,"Stations"); //mongoDB中的Collation名称
    }

    //输入trainNo返回相应的车站信息对象
    public TrainStationMap findOneByTrainNo(String trainNo){
        Query query = new Query(Criteria.where("trainNo").is(trainNo));
        return this.findOne(query,"Stations");
    }

    //输入trainId返回相应的车站信息对象
    public TrainStationMap findOneByTrainId(Integer trainId){
        Query query = new Query(Criteria.where("trainId").is(trainId));
        return this.findOne(query,"Stations");
    }

    //输入站名和trainNo,返回此站名对应的index
    public Integer stationNameToInteger(String stationName , String trainNo){
        TrainStationMap stationInfo = this.findOneByTrainNo(trainNo);
        if(stationInfo != null){
            return stationInfo.stations.indexOf(stationName);
        }
        else{
            return -1;
        }
    }

    //输入站名和trainId,返回此站名对应的index
    public Integer stationNameToInteger(String stationName , Integer trainId){
        TrainStationMap stationInfo = this.findOneByTrainId(trainId);
        if(stationInfo != null){
            return stationInfo.stations.indexOf(stationName);
        }
        else{
            return -1;
        }
    }

    public List<Ticket> getAllTicketsBySeatAndTrainIdFromredis(String seat, Integer trainId){
        List<Ticket> ticketList = new ArrayList<>();

        Set<String> keys = this.stringRedisTemplate.keys("*trainId" + Integer.toString(trainId));
        List<String> jsonList = this.stringRedisTemplate.opsForValue().multiGet(keys);

        for(int i=0;i<jsonList.size();i++){
            ObjectMapper mapper = new ObjectMapper();
            try{
                Ticket ticket = mapper.readValue(jsonList.get(i), Ticket.class);
                if(ticket.seat.equals(seat)){
                    ticketList.add(ticket);
                }
            }catch(Exception e){
                System.out.println(e.getMessage());
            }
        }
        return ticketList;
    }

}

