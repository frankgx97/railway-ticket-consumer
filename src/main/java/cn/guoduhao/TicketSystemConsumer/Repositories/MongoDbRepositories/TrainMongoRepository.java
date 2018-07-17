package cn.guoduhao.TicketSystemConsumer.Repositories.MongoDbRepositories;


import cn.guoduhao.TicketSystemConsumer.Models.TrainStationMap;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface TrainMongoRepository extends MongoRepository<TrainStationMap,String> {

    //TrainStationMap findOne(Query query,String collectonName);

}
