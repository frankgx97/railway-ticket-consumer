package cn.guoduhao.TicketSystemConsumer.Services.ticket;

import cn.guoduhao.TicketSystemConsumer.Models.Ticket;
import cn.guoduhao.TicketSystemConsumer.Models.Train;
import cn.guoduhao.TicketSystemConsumer.Repositories.TicketRepository;
import cn.guoduhao.TicketSystemConsumer.Repositories.TrainRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import cn.guoduhao.TicketSystemConsumer.Services.OrderService;
import java.util.*;

@Service
public class TicketServiceImpl implements TicketService{

    //导入ticketRepository
    private final TicketRepository ticketRepository;

    private final TrainRepository trainRepository;

    //private final OrderService orderService;

    private Logger logger;

    @Autowired //无需实例化，交给Spring管理
    public TicketServiceImpl(TicketRepository ITicketRepository, TrainRepository ITrainRepository ){
        this.ticketRepository = ITicketRepository;
        this.trainRepository = ITrainRepository;
//        this.orderService = IOrderService;
    }

    @Override
    public Optional<Ticket> getTicketByUserId(String userId){
        return this.ticketRepository.findOneByUserId(userId);
    }

//    @JmsListener(destination = "orders")
//    public void receiveAndModifyStations(String message) {
//        //这里的返回值必须为void，否则需要Message存在replyto
//        //从ActiveMQ接收消息
//        this.logger.info("Received message: " + message);
//        ObjectMapper mapper = new ObjectMapper();
//        try{
//            Ticket ticket = mapper.readValue(message, Ticket.class);
//            if (orderService.updateTrainDb(ticket.trainId) == 0){
//                String ticketId = ticket.id;
//
//                //Coding by TianXinyao at July/12th/2018
//
//                String receivedStations = createStations_BJ_SH("北京","上海");
//                String receivedDepartStation = ticket.departStation;
//                String receivedDestinationStation = ticket.destinationStation;
//                String modifiedStation = modifyStations(receivedDepartStation,receivedDestinationStation,receivedStations);
//                ticket.stations = modifiedStation;
//
//                //End Coding...
//
//                //write redis
//                String ticketJson = orderService.updateTicketStatus(ticket);
//                orderService.writeRedis(ticketId, ticket.userId, ticket.trainId, ticketJson);
//            }
//        }catch(JsonProcessingException jsonProcessingException) {
//            this.logger.error("jsonProcessingException");
//            this.logger.error(jsonProcessingException.getMessage());
//        }catch(IOException ioException) {
//            this.logger.error("IOException");
//            this.logger.error(ioException.getMessage());
//        }
//    }

    @Override
    public Integer buyTicket_BJ_SH(Ticket newTicket){
        //在剩余的半程票中是否已经成功购票
        boolean isSuccess = buyRemanentTicket_BJ_SH(newTicket);
        if(isSuccess){
            return 1;//若已成功购票 则返回1
        }
        else{//否则，从全程票中查看是否有空闲票
            //ToDo 需要更改 进入NoSQL中查询含有顾客上车和下车站的trainNo, 再根据TrainNo检索
            List<Train> trains = //注意这里是 trainRepo 不是 ticketRepo
                trainRepository.findByDepartStationAndDestinationStation("北京","上海");
            Integer remanentTickets = trains.get(0).seatsTotal - trains.get(0).seatsSold;
            if(remanentTickets > 0){//若全程票中有空闲票
                String startStation = newTicket.departStation;
                String arriveStation =newTicket.destinationStation;
                //创建新的stations字段
                String defaultStation = createStations_BJ_SH("北京","上海");
                String newStations = modifyStations(startStation,arriveStation,defaultStation);
                trains.get(0).seatsSold += 1;//全程票售出一张(全程半程均可)
                newTicket.stations = newStations;
                ticketRepository.save(newTicket);//更新新买的票至Ticket表
                trainRepository.save(trains.get(0));//更新Train表
                return 1;//返回成功
            }
            else{
                return 2;//表示没有合适的票
            }
        }
    }

    //ToDo JB_SH推广时需要修改
    @Override
    public boolean buyRemanentTicket_BJ_SH(Ticket newTicket){
        //读出乘客的起始站和目的站
        String startStation = newTicket.departStation;
        String arriveStation = newTicket.destinationStation;
        //ToDo JB_SH推广时需要修改
        //在Ticket表中搜索相应段全部为0的票务信息
        List<Ticket> targetTickets = searchRemanentTicket_BJ_SH(startStation,arriveStation);
        //搜索符合条件的剩余票
        if(!targetTickets.isEmpty()){
            String targetStations = targetTickets.get(0).stations;
            //新建String stations
            String newStations = modifyStations(startStation,arriveStation,targetStations);
            if (newStations.equals(targetStations)){ //若stations未发生改动(表示没有修改成功)
                return false; // 则返回false 表示修改失败 购票失败 剩余的票中已经没有满足需求的分段票了
            }
            else{
                //同时更新新票和已经购入票的stations字段
                newTicket.stations = newStations;
                targetTickets.get(0).stations = newStations;
                //ToDo:改
                //当选购票后，票的stations字段全部变为"1" 说明已经凑出了一张全程票
                if (newStations.equals(modifyStations("北京","上海",""))){
                    //找到原来半程票对应的trainNo
                    Optional<Train> train = trainRepository.findOneByTrainNo(targetTickets.get(0).trainNo);
                    if(train.isPresent()){
                        train.get().seatsSold += 1; //trainNo对应的seatsSold + 1
                        trainRepository.save(train.get()); //更新Train表，改变剩余票数
                    }
                    return true; //表示购票成功
                }

                //将这两张票在数据库中更新
                ticketRepository.save(newTicket);
                ticketRepository.save(targetTickets.get(0));
                return true; //返回 true 购票成功 表示此次购票可以在剩余的票中解决
            }
        }
        return false; // 返回 false 购票失败 剩余的票中已经没有满足需求的分段票了
    }

    //ToDo 添加北京至上海的分段查找功能 如果铁路线路过多需要使用数据库存储。具体实现逻辑在石墨文档
    @Override
    public List<Ticket> searchRemanentTicket_BJ_SH(String startStation, String arriveStation){
        List<Ticket> targetTickets = new ArrayList<>();
        //ToDo 此处推广时需要修改
        List<Ticket> tickets =
                ticketRepository.findByTrainNo("G1");
        //ToDo 此处推广时需要修改
        Integer totalStations = StringToStationNum_BJ_SH("上海"); //总站数
        Integer startNum = StringToStationNum_BJ_SH(startStation); // 乘客上车站
        Integer arriveNum = StringToStationNum_BJ_SH(arriveStation); //乘客下车站
        Integer remanNum = totalStations - arriveNum; //距离终点站的站数(用于组合正则)

        //使用String组合出对应的正则表达式
        String patternStations = "";
        patternStations = "[01]{" + startNum.toString() + "}";
        for(int i = startNum; i < arriveNum ; i++){
            patternStations = patternStations + "0";
        }
        patternStations = patternStations + "[01]{" + remanNum.toString() + "}";
        //System.out.println(patternStations);

        //利用正则遍历车票
        for(int i = 0; i < tickets.size() ;i++){
            boolean isMatch = Pattern.matches(patternStations,tickets.get(i).stations);
            if(isMatch){
                targetTickets.add(tickets.get(i));
            }
        }
        //返回符合情况的List
        return targetTickets;
    }

    @Override
    public String modifiedTicketStation(Ticket ticket){
        return modifyStations(ticket.departStation,ticket.destinationStation,ticket.stations);
    }

    //传入: String depart 站名
    //输出: stations
    //ToDo 分站购票中，如果有多条火车线路，需要在这里修改StringToStationNum_BJ_SH成别的对应关系
    //或者做个switch，多加一个参数，让函数选择哪条线路的车票对应关系
    public String modifyStations(String departStation,String destinationStation,String stations){
        Integer departNum = StringToStationNum_BJ_SH(departStation);
        Integer destinationNum = StringToStationNum_BJ_SH(destinationStation);
        return modifyString(departNum,destinationNum,stations);
    }

    //分段式构建 示例:
    //北京-石家庄-邯郸-郑州
    //    0      0    0
    //以下是用于分段购票的实现函数
    public String modifyString(Integer departNum , Integer destinationNum,String stations) {
        //若选择的起始站比终点站还远 或 起始站与终点站相同
        if (departNum >= destinationNum) {
            //直接返回stations 不予处理
            return stations;
        }
        Integer stationsLength = stations.length();
        //若选择的起始站和终点站位置超过范围
        if(departNum<0 || destinationNum>(stationsLength)){
            //直接返回stations 不予处理
            return stations;
        }
        //新的stations信息
        char[] newStations = stations.toCharArray();
//        newStations[departNum] = '1';
//        newStations[destinationNum] = '1';
        for(Integer temp = departNum  ; temp < destinationNum ; temp++){
            if(stations.charAt(temp) == '1'){
                return stations;
            }
            newStations[temp] = '1';
        }
        //返回新的stations信息
        return Arrays.toString(newStations).replaceAll("[\\[\\]\\s,]", "");
    }

    //生成一个北京-上海的"0000000000"字段
    public String createStations_BJ_SH(String departStation,String destinationaStation){
        Integer departNum = StringToStationNum_BJ_SH(departStation);
        Integer destinationNum = StringToStationNum_BJ_SH(destinationaStation);
        return createStations(departNum,destinationNum);
    }

    //输入站号，自动生成相应位数的stations字段
    private String createStations(Integer departNum,Integer destinationNum){
        String defaultStation = "";
        for(int i = departNum; i < destinationNum ; i++){
            defaultStation = defaultStation + "0";
        }
        return defaultStation;
    }

    //北京-上海的火车站数
    // 0 北京 1 天津西 2 沧州 3 德州 4 徐州 5 南京 6 镇江 7 常州 8 无锡 9 苏州 10 上海
    /**
    * @author 田昕峣
     * @param stationName
     *  车站名
     * @return homoNum
     *  转换好的对应车站序号
    * */
    //ToDo 此函数可放入数据库，以简化代码
    private Integer StringToStationNum_BJ_SH(String stationName){
        Integer homoNum;
        switch (stationName){
            case"北京": homoNum = 0; break;
            case"天津西": homoNum = 1; break;
            case"沧州": homoNum = 2; break;
            case"德州": homoNum = 3; break;
            case"徐州": homoNum = 4; break;
            case"南京": homoNum = 5; break;
            case"镇江": homoNum = 6; break;
            case"常州": homoNum = 7; break;
            case"无锡": homoNum = 8; break;
            case"苏州": homoNum = 9; break;
            case"上海": homoNum = 10; break;
            default:homoNum = -1;break;
        }
        return homoNum;
    }
}
