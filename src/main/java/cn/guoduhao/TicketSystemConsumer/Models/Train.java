package cn.guoduhao.TicketSystemConsumer.Models;

import javax.persistence.*;

@Entity
@Table
public class Train {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    public Integer id;
    public String trainNo;//车次
    public String departStation;//始发站
    public String destinationStation;//到达站
    public String departTime;//出发时间
    public Integer seatsTotal;//总座位
    public Integer seatsSold;//已售出座位
}