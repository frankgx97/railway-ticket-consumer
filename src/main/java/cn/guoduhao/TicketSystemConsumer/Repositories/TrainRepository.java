package cn.guoduhao.TicketSystemConsumer.Repositories;

import cn.guoduhao.TicketSystemConsumer.Models.Train;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

public interface TrainRepository extends JpaRepository<Train, Long> {
    Optional<Train> findOneByTrainNo(String trainNo);
    Optional<Train> findOneById(Integer trainId);
    List<Train> findByTrainNo(String trainNo);
    List<Train> findByDepartStationAndDestinationStationAndDepartTime(String departStation, String destinationStation, String departTime);
    List<Train> findByDepartStationAndDestinationStation(String departStation, String destinationStation);
    @Transactional
    @Modifying
    @Query("update Train t set t.seatsSold = ?2, t.version = ?4 where t.id = ?1 and t.version = ?3")
    void updateSoldSeats(Integer trainId, Integer seatsSold, Long version, Long versionNew);
}
