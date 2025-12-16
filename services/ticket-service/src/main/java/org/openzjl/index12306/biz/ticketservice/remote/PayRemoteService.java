package org.openzjl.index12306.biz.ticketservice.remote;

import org.openzjl.index12306.biz.ticketservice.remote.dto.PayInfoRespDTO;
import org.openzjl.index12306.biz.ticketservice.remote.dto.RefundReqDTO;
import org.openzjl.index12306.biz.ticketservice.remote.dto.RefundRespDTO;
import org.openzjl.index12306.framework.starter.convention.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 支付单远程调用支付服务Rpc
 *
 * @author zhangjlk
 * @date 2025/12/15 上午10:20
 */
@FeignClient(value = "index12306-order${unique-name:}-service", url = "http://127.0.0.1:9001")
public interface PayRemoteService {

    /**
     * 支付单详情查询
     */
    @GetMapping("/api/pay-service/pay/query")
    Result<PayInfoRespDTO> getPayInfo(@RequestParam(value = "orderSn") String orderSn);

    /**
     * 公共退款接口
     */
    @PostMapping("/api/pay-service/common/refund")
    Result<RefundRespDTO> refund(@RequestBody RefundReqDTO requestParam);
}
