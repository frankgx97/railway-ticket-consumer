package cn.guoduhao.TicketSystemConsumer.Services.ticket;

import cn.guoduhao.TicketSystemConsumer.Models.Ticket;

import java.util.List;
import java.util.Optional;

public interface TicketService {

//    Optional<Ticket> getTicketByUserId(String userId);
//
//    String modifiedTicketStation(Ticket ticket);
//
    List<Ticket> searchRemanentTicket(String startStation, String arriveStation , Integer trainId);

    boolean buyRemanentTicket(Ticket newTicket);
//
    Integer buyTicket(Ticket newTicket);
//
    String modifyStations(String departStation,String destinationStation,String trainNo);
//
    String createStations(String trainNo);
//
//    //void receiveAndModifyStations(String message);

    List<String> mapToTrainNo_BJ_SH(String departStation,String destinationStation);

    List<String> mapToTrainNo(String departStation,String destinationStation);

    //票价算法
    float countFee(String departStation,String destinationStation,String trainNo);




}
