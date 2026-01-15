package org.openzjl.index12306.biz.orderservice.remote;

import jakarta.validation.constraints.NotEmpty;
import org.openzjl.index12306.biz.orderservice.remote.dto.UserQueryActualRespDTO;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 远程调用用户服务
 *
 * @author zhangjlk
 * @date 2026/1/15 17:09
 */
@FeignClient(value = "index12306-user${unique-name:}-service", url = "${aggregation.remote-url:}")
public interface UserRemoteService {

    /**
     * 根据乘车人 ID 集合查询乘车人列表
     */
    @GetMapping("/api/user-service/actual/query")
    Result<UserQueryActualRespDTO> queryActualUserByUsername(@RequestParam("username") @NotEmpty String username);
}
