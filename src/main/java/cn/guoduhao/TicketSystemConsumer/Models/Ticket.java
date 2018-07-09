package cn.guoduhao.TicketSystemConsumer.Models;

import javax.persistence.*;

@Entity
@Table
public class Ticket {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    public Integer id;
    public String userId;
    public String name;//姓名
    public String orderId;//订单编号
    public String departTime;//时间
    public String departStation;//起点
    public String destinationStation;//终点
    public Integer expense;//车费
    public Integer status;//车票状态
    public String trainId;//车次
    public String seat;//座位

    public Ticket(String name){
        this.name = name;
    }
    public Ticket(){}
}
