package cn.guoduhao.TicketSystemConsumer.Services.ticket;

import cn.guoduhao.TicketSystemConsumer.Models.Ticket;

import java.util.List;
import java.util.Optional;

public interface TicketService {

//    Optional<Ticket> getTicketByUserId(String userId);
//
//    String modifiedTicketStation(Ticket ticket);
//
//    List<Ticket> searchRemanentTicket_BJ_SH(String startStation, String arriveStation);
//
//    boolean buyRemanentTicket_BJ_SH(Ticket newTicket);
//
//    Integer buyTicket_BJ_SH(Ticket newTicket);
//
//    String modifyStations(String departStation,String destinationStation,String stations);
//
//    String createStations_BJ_SH(String departStation,String destinationaStation);
//
//    //void receiveAndModifyStations(String message);

    List<String> mapToTrainNo_BJ_SH(String departStation,String destinationStation);

    List<String> mapToTrainNo(String departStation,String destinationStation);

    //票价算法
    float countFee(String departStation,String destinationStation,String trainNo);




}
