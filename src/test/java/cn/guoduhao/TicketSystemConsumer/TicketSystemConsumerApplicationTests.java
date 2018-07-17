package cn.guoduhao.TicketSystemConsumer;

import cn.guoduhao.TicketSystemConsumer.Models.Ticket;
import cn.guoduhao.TicketSystemConsumer.Services.OrderService;
import cn.guoduhao.TicketSystemConsumer.Services.ticket.TicketServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TicketSystemConsumerApplicationTests {

	@Autowired
	OrderService orderService;

	@Autowired
	TicketServiceImpl ticketServiceImpl;

	@Test
	@Bean
	public void RemanentTicketTest(){


//		System.out.println("预期输出: 空");
//		System.out.println(ticketServiceImpl.searchRemanentTicket("北京","武汉",110));

		System.out.println("预期输出: 空");
		System.out.println(ticketServiceImpl.searchRemanentTicket("北京","上海",110));

		System.out.println("预期输出: 非空");
		System.out.println(ticketServiceImpl.searchRemanentTicket("苏州","上海",110));

	}


	@Test
	public void contextLoads() {
	}

}
