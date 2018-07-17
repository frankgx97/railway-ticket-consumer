package cn.guoduhao.TicketSystemConsumer.Repositories;

import cn.guoduhao.TicketSystemConsumer.Models.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import cn.guoduhao.TicketSystemConsumer.Models.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Integer> {
    Optional<Ticket> findOneById(String id);
    Optional<Ticket> findOneByUserId(String userId);
    Optional<Ticket> findOneByOrderId(String orderId);
    List<Ticket> findByTrainId(Integer trainId);
    List<Ticket> findByTrainNo(String trainNo);
    List<Ticket> findByDepartStationAndDestinationStationAndDepartTime(String departStation, String destinationStation, String departTime);
    List<Ticket> findByDepartStationAndDestinationStation(String departStation,String destinationStation);
    List<Ticket> findByStations(String stations);

    @Modifying(clearAutomatically = true)
    @Transactional
    void deleteByUserId(String userId);

    @Modifying(clearAutomatically = true)
    @Transactional
    void deleteByTrainId(Integer trainId);


}


