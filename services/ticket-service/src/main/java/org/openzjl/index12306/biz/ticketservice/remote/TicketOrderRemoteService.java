package org.openzjl.index12306.biz.ticketservice.remote;

import org.springframework.cloud.openfeign.FeignClient;

/**
 * 车票订单远程服务调用
 *
 * @author zhangjlk
 * @date 2025/12/15 上午10:08
 */
@FeignClient(value = "index12306-order${unique-name:}-service", url = "http://127.0.0.1:9001")
public interface TicketOrderRemoteService {


}
