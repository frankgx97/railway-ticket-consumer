package cn.guoduhao.TicketSystemConsumer.Repositories;

import cn.guoduhao.TicketSystemConsumer.Models.Train;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrainRepository extends JpaRepository<Train, Long> {
    Optional<Train> findOneByTrainId(String trainId);
    Optional<Train> findOneById(Integer id);
    List<Train> findByDepartStationAndDestinationStationAndDepartTime(String departStation, String destinationStation, String departTime);
    List<Train> findByDepartStationAndDestinationStation(String departStation, String destinationStation);
}
