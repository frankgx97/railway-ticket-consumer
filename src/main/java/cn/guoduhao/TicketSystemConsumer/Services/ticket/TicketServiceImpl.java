package cn.guoduhao.TicketSystemConsumer.Services.ticket;

import cn.guoduhao.TicketSystemConsumer.Models.Ticket;
import cn.guoduhao.TicketSystemConsumer.Models.Train;
import cn.guoduhao.TicketSystemConsumer.Models.TrainStationMap;
import cn.guoduhao.TicketSystemConsumer.Repositories.TicketRepository;
import cn.guoduhao.TicketSystemConsumer.Repositories.TrainRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;

import cn.guoduhao.TicketSystemConsumer.Services.OrderService;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import cn.guoduhao.TicketSystemConsumer.Services.OrderService;
import java.util.*;

import static java.lang.Math.abs;

@Service
public class TicketServiceImpl implements TicketService{

    //导入ticketRepository

    private final TrainRepository trainRepository;

    private final OrderService orderService;

    private Logger logger;

    @Autowired //无需实例化，交给Spring管理
    public TicketServiceImpl(TrainRepository ITrainRepository , OrderService IOrderService){
        this.trainRepository = ITrainRepository;
        this.orderService = IOrderService;
    }

    @Override //票价算法
    public float countFee(String departStation,String destinationStation,String trainNo){
        List<Train> targetTrain = trainRepository.findByTrainNo(trainNo);
        if(targetTrain.isEmpty()){
            return -1;
        }
        else{
            Integer totalExpense = targetTrain.get(0).expense - 126;
            System.out.println(totalExpense);
            TrainStationMap stationInfo = orderService.findOneByTrainNo(trainNo);
            Integer totalStaitonAmount = stationInfo.stations.size();
            Integer departNum = orderService.stationNameToInteger(departStation,trainNo);
            Integer destinationNum = orderService.stationNameToInteger(destinationStation,trainNo);
            if(departNum == -1 || destinationNum == -1){
                return -1;
            }
            else {
                DecimalFormat df = new DecimalFormat("#.00");
                float partitionStationAmount = abs(destinationNum - departNum);
                String format = df.format((208 + (partitionStationAmount/totalStaitonAmount)*totalExpense ));
                return Float.valueOf(format);
            }
        }
    }

    @Override
    public Integer buyTicket(Ticket newTicket){
        //在剩余的半程票中是否已经成功购票
        boolean isSuccess = buyRemanentTicket(newTicket);
        if(isSuccess){
            return 1;//若已成功购票 则返回1
        }
        else{//否则，从全程票中查看是否有空闲票
            Optional<Train> train = //注意这里是 trainRepo 不是 ticketRepo
                trainRepository.findOneById(newTicket.trainId);
            Integer remanentTicketsNum = train.get().seatsTotal - train.get().seatsSold;
            if(remanentTicketsNum > 0){//若全程票中有空闲票
                String startStation = newTicket.departStation;
                String arriveStation = newTicket.destinationStation;
                //创建新的stations字段
                String defaultStation = createStations(train.get().trainNo);
                String newStations = modifyStations(startStation,arriveStation,train.get().trainNo);
                if(newStations.equals(defaultStation)){ //当stations等类似于"000000000"时(出现特殊情况)
                    return 2;//返回2
                }
                train.get().seatsSold += 1;//全程票售出一张(全程半程均可)
                newTicket.stations = newStations;
                String ticketJson = orderService.updateTicketStatus(newTicket);
                trainRepository.save(train.get());//更新Train表
                orderService.writeRedis(newTicket.id, newTicket.userId, newTicket.trainId, ticketJson);
                return 1;//返回成功
            }
            else{
                return 2;//表示没有合适的票
            }
        }
    }


//    @Override
    public boolean buyRemanentTicket(Ticket newTicket){
        //读出乘客的起始站和目的站
        String startStation = newTicket.departStation;
        String arriveStation = newTicket.destinationStation;
        String trainNo = newTicket.trainNo;
        Integer trainId = newTicket.trainId;
        //在Ticket表中搜索相应段全部为0的票务信息
        List<Ticket> targetTickets = searchRemanentTicket(startStation,arriveStation,trainId);
        //搜索符合条件的剩余票
        if(!targetTickets.isEmpty()){
            String targetStations = targetTickets.get(0).stations;
            //新建String stations
            String newStations = modifyStations(startStation,arriveStation,trainNo);
            if (newStations.equals(targetStations)){ //若stations未发生改动(表示没有修改成功)
                return false; // 则返回false 表示修改失败 购票失败 剩余的票中已经没有满足需求的分段票了
            }
            else{
                //同时更新相同座位新票和已经购入票的stations字段

                String seat = targetTickets.get(0).seat ;

                List<Ticket> modifiedtargetTickets = orderService.getAllTicketsBySeatAndTrainIdFromredis(seat,trainId);//读出相同seat的Ticket
                //分别更新相同Seat的Ticket
                for( int i = 0 ; i < modifiedtargetTickets.size() ; i++ ){
                    Ticket curTicket = modifiedtargetTickets.get(i);
                    curTicket.stations = newStations;
                    //此处代码粘贴自Services.OrderService中的ticket转redis函数
                    String ticketJsonCur = orderService.updateTicketStatus(curTicket);
                    orderService.writeRedis(curTicket.id, curTicket.userId, curTicket.trainId, ticketJsonCur);
                }

                //更新newTicket
                newTicket.stations = newStations;
                newTicket.seat = seat;
                String ticketJsonNew = orderService.updateTicketStatus(newTicket);
                orderService.writeRedis(newTicket.id, newTicket.userId, newTicket.trainId, ticketJsonNew);

                //当选购票后，票的stations字段全部变为"1" 说明已经凑出了一张全程票
                if (newStations.equals(
                        modifyStations(orderService.findOneByTrainNo(trainNo).stations.get(0),
                                orderService.findOneByTrainNo(trainNo).stations.get(orderService.findOneByTrainNo(trainNo).stations.size()-1),
                                trainNo))){
                    //找到原来半程票对应的trainNo
//                    Optional<Train> train = trainRepository.findOneByTrainNo(targetTickets.get(0).trainNo);
//                    if(train.isPresent()){
//                        train.get().seatsSold += 1; //trainNo对应的seatsSold + 1
//                        trainRepository.save(train.get()); //更新Train表，改变剩余票数
//                    }
                    return true; //表示购票成功
                }
//
//                //将这两张票在数据库中更新
//                ticketRepository.save(newTicket);
//                ticketRepository.save(targetTickets.get(0));
                return true; //返回 true 购票成功 表示此次购票可以在剩余的票中解决
            }
        }
        else{
            return false; // 返回 false 购票失败 剩余的票中已经没有满足需求的分段票了
        }
    }


    @Override
    public List<Ticket> searchRemanentTicket(String departStation,String destinationStation, Integer trainId){
        List<Ticket> targetTickets = new ArrayList<>();
        List<Ticket> tickets = orderService.findTicketFromTrainId(trainId); //  查找 Redis 数据库
        //System.out.println(tickets.size());

        String trainNo;
        if(trainRepository.findOneById(trainId).isPresent()){
            trainNo = trainRepository.findOneById(trainId).get().trainNo;
        }
        else{
            trainNo = "";
        }

        System.out.println(trainNo);
        //------------------//

        //ToDo 此处推广时需要修改
        Integer totalStations = orderService.findOneByTrainNo(trainNo).stations.size() - 1; //总站间隔数
        Integer startNum = orderService.stationNameToInteger(departStation,trainNo); // 乘客上车站
        Integer arriveNum = orderService.stationNameToInteger(destinationStation,trainNo); //乘客下车站
        Integer remanNum = totalStations - arriveNum; //距离终点站的站数(用于组合正则)

        //使用String组合出对应的正则表达式
        String patternStations = "";
        patternStations = "[01]{" + startNum.toString() + "}";
        for(int i = startNum; i < arriveNum ; i++){
            patternStations = patternStations + "0";
        }
        patternStations = patternStations + "[01]{" + remanNum.toString() + "}";
        System.out.println(patternStations);

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
    //若能够映射到BJ_SH的列车 则返回"G1";否则返回""
    public List<String> mapToTrainNo_BJ_SH(String departStation,String destinationStation){
        return this.mapToTrainNo(departStation,destinationStation);
    }

    @Override
    public List<String> mapToTrainNo(String departStation,String destinationStation){
        List<TrainStationMap>stationInfos =  orderService.findAllByDepartStaitonAndDestinationStation(departStation , destinationStation);
        List<String> trainNos= new ArrayList<>();
        if(stationInfos.isEmpty()){
            trainNos.add("");
            return trainNos; // 空list
        }
        else{
            for(int i = 0; i<stationInfos.size();i++){
                trainNos.add(stationInfos.get(i).trainNo);
            }
            return trainNos;
        }
    }


    public String modifiedTicketStation(Ticket ticket){
        return modifyStations(ticket.departStation,ticket.destinationStation,ticket.trainNo);
    }

    @Override
    public String modifyStations(String departStation,String destinationStation,String trainNo){
        Integer departNum = orderService.stationNameToInteger(departStation,trainNo);
        Integer destinationNum= orderService.stationNameToInteger(destinationStation,trainNo);
        return modifyString(departNum,destinationNum,createStations(trainNo));
    }

    //分段式构建 示例:
    //北京-石家庄-邯郸-郑州
    //    0      0    0
    //以下是用于分段购票的实现函数
    private String modifyString(Integer departNum , Integer destinationNum,String stations) {
        //若输入站号出现错误
        if(departNum == -1 || destinationNum == -1 || stations.equals("")){
            return stations;//返回""
        }
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


    //生成具有相应位数的stations 此函数用于兼容多线路购票
    @Override
    public String createStations(String trainNo){
        TrainStationMap stationInfo = orderService.findOneByTrainNo(trainNo);
        if (stationInfo == null){
            return "";
        }
        else{
            Integer departNum = 0;
            Integer destinationNum = ( stationInfo.stations.size() - 1 );
            return createStations(departNum,destinationNum);
        }
    }

    //输入站总数(0-总数)，自动生成相应位数的stations字段
    private String createStations(Integer departNum,Integer destinationNum){
        String defaultStation = "";
        for(int i = departNum; i < destinationNum ; i++){
            defaultStation = defaultStation + "0";
        }
        return defaultStation;
    }

}
