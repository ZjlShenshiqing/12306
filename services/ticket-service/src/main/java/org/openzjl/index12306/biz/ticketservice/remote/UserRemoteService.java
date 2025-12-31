package org.openzjl.index12306.biz.ticketservice.remote;

import org.openzjl.index12306.biz.ticketservice.remote.dto.PassengerRespDTO;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 用户远程服务调用
 *
 * @author zhangjlk
 * @date 2025/12/31 上午10:09
 */
@FeignClient(value = "index12306-user${unique-name:}-service", url = "http://127.0.0.1:${server.port}")
public interface UserRemoteService {

    /**
     * 根据乘车人ID列表查询乘车人信息
     * @param username 用户名
     * @param ids 乘车人ID列表
     * @return 乘车人信息列表
     */
    @GetMapping("/api/user-service/inner/passenger/actual/query/ids")
    Result<List<PassengerRespDTO>> listPassengerQueryByIds(@RequestParam("username") String username, @RequestParam("ids") List<String> ids);
}
