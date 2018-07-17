package cn.guoduhao.TicketSystemConsumer;

import cn.guoduhao.TicketSystemConsumer.Models.TrainStationMap;
import cn.guoduhao.TicketSystemConsumer.Repositories.MongoDbRepositories.TicketMongoRepository;
import cn.guoduhao.TicketSystemConsumer.Services.OrderService;
import cn.guoduhao.TicketSystemConsumer.Services.ticket.TicketServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;


@RunWith(SpringRunner.class)
@SpringBootTest
public class MongoDbTests {

    @Autowired
    TicketMongoRepository ticketMongoRepository;

    @Autowired
    TicketServiceImpl ticketServiceImpl;

    @Autowired
    OrderService orderService;

//    @Test
//    public void mongoDbWriteTest(){
//        Ticket ticket = new Ticket();
//        ticket.id = UUID.randomUUID().toString();
//        ticket.trainNo = "G1";
//        ticket.departStation = "北京";
//        ticket.destinationStation = "上海";
//        ticket.departTime = "2018-09-01-11:00:00";
//        ticket.expense = 0;
//        ticket.name = "test";
//        ticket.orderId = "00000001";
//        ticket.seat = "A1";
//        ticket.status = 0;
//        ticket.userId = "4028abda64709466016470952b6b0000";
//        ticketMongoRepository.save(ticket);
//    }

//    @Test
//    @Bean
//    public void mongoDbSearchTest(){
//        Query query = new Query(Criteria.where("stations").is("北京"));
//        TrainStationMap stationInfo = orderService.findOne(query,"Stations");
//        System.out.println(stationInfo.trainNo);
//    }
//
//    @Test
//    @Bean
//    public void mongoDbSearchAllTest(){
//        Query query = new Query(Criteria.where("stations").is("北京"));
//        List<TrainStationMap> stationInfos = orderService.find(query,"Stations");
//        System.out.println(stationInfos.size());
//        System.out.println(stationInfos.get(0).trainNo);
//    }

    @Test
    @Bean
    public void mongoDbSearchAllTest(){
        //预期: 1
        List<TrainStationMap> stationInfos =
                orderService.findAllByDepartStaitonAndDestinationStation("天津西","苏州");
        System.out.println(stationInfos.size());

        //预期: 2
        stationInfos =
                orderService.findAllByDepartStaitonAndDestinationStation("北京","武汉");
        System.out.println(stationInfos.size());

        //预期: 0
        stationInfos =
                orderService.findAllByDepartStaitonAndDestinationStation("天津西","武汉");
        System.out.println(stationInfos.size());

        //预期: 0
        stationInfos =
                orderService.findAllByDepartStaitonAndDestinationStation("天津","苏州");
        System.out.println(stationInfos.size());
    }

    @Test
    @Bean
    public void mongoDbSearchAllByTrainNoTest(){
        //预期: G1,站信息的Array
        TrainStationMap trainStationInfo = orderService.findOneByTrainNo("G1");
        System.out.println(trainStationInfo.trainNo);
        System.out.println(trainStationInfo.stations);
    }

    @Test
    @Bean
    public void mongoDbSearchAllByTrainNoTest2(){
        //预期: G1,站信息的Array
        TrainStationMap trainStationInfo = orderService.findOneByTrainNo("G1");
        System.out.println(trainStationInfo.trainNo);
        System.out.println(trainStationInfo.stations.size());
        System.out.println(trainStationInfo.stations);
        System.out.println(trainStationInfo.stations.get(0));
    }

    @Test
    @Bean
    public void mongoDbSearchAllByTrainNoTest3(){
        //预期: 0
        Integer stationIndex= orderService.stationNameToInteger("北京","G1");
        System.out.println(stationIndex);

        //预期: -1 (线路不存在相应站点则返回-1)
        Integer stationIndex2= orderService.stationNameToInteger("乌鲁木齐","G1");
        System.out.println(stationIndex2);
    }

    @Test
    @Bean
    public void mongoDbcreateStationTest(){
        //预期: 10个0组成的String
        System.out.println(ticketServiceImpl.createStations("G1"));

        //预期: 10个0组成的String
        System.out.println(ticketServiceImpl.createStations("G2"));

        //预期: 9个0组成的String
        System.out.println(ticketServiceImpl.createStations("G507"));

        //预期: ""
        System.out.println(ticketServiceImpl.createStations("G9999"));
    }

    @Test
    @Bean
    public void mongoDbStationsTest1(){
        //预期: 10个0或1组成的String，反映出相应站关系
        System.out.println(ticketServiceImpl.modifyStations("德州","徐州","G1"));

        //预期: 10个1组成的String
        System.out.println(ticketServiceImpl.modifyStations("北京","武汉","G2"));

        //预期: 9个1组成的String
        System.out.println(ticketServiceImpl.modifyStations("北京","武汉","G507"));

        //预期: ""
        System.out.println(ticketServiceImpl.modifyStations("北京","乌鲁木齐","G999"));
    }

    @Test
    @Bean
    public void mongoDbStationsTest2(){
        //预期: G1
        System.out.println(ticketServiceImpl.mapToTrainNo("德州","徐州").get(0));

        //预期: G2
        System.out.println(ticketServiceImpl.mapToTrainNo("石家庄","郑州东").get(0));

        //预期: G2
        System.out.println(ticketServiceImpl.mapToTrainNo("北京","武汉").get(0));
        System.out.println(ticketServiceImpl.mapToTrainNo("北京","武汉").get(1));

        //预期: ""
        System.out.println(ticketServiceImpl.mapToTrainNo("北京","乌鲁木齐").get(0));
    }

    @Test
    @Bean
    public void mongoDbExpenseTest(){
        //预期:相应价格
        System.out.println(ticketServiceImpl.countFee("北京","邯郸东","G2"));

        //预期:相应价格
        System.out.println(ticketServiceImpl.countFee("北京","德州","G1"));

        //预期:-1
        System.out.println(ticketServiceImpl.countFee("北京","德州","G2"));

        //预期:-1
        System.out.println(ticketServiceImpl.countFee("北京","德州","G88"));
    }

}
