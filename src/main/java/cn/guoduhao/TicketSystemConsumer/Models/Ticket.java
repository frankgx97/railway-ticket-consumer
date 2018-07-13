package cn.guoduhao.TicketSystemConsumer.Models;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@Table
public class Ticket {
    @Id
    @GeneratedValue(generator="system-uuid", strategy= GenerationType.AUTO)
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    public String id;
    public String userId;
    public String name;//姓名
    public String orderId;//订单编号
    public String departTime;//时间
    public String departStation;//起点
    public String destinationStation;//终点
    public Integer expense;//车费
    public Integer status;//车票状态
    public Integer trainId;//火车id
    public String trainNo;//车次
    public String seat;//座位
    public String stations;//途经车站
    public String version;//乐观锁
    public Long timestamp;//时间戳

    public Ticket(String name){
        this.name = name;
    }
    public Ticket(){}
}
