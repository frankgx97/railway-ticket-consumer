package cn.guoduhao.TicketSystemConsumer.Models;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.List;


public class TrainStationMap {
    @Id
    @GeneratedValue(generator="system-uuid", strategy= GenerationType.AUTO)
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    public String id ;
    public String trainNo;
    public List<String> stations ;

    public TrainStationMap(){};


}
