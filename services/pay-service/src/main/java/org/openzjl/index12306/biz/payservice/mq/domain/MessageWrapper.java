package org.openzjl.index12306.biz.payservice.mq.domain;

import cn.hutool.core.lang.UUID;
import lombok.*;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.io.Serializable;

/**
 * 消息体包装器
 *
 * @author zhangjlk
 * @date 2026/1/30 16:45
 */
@Data
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
@RequiredArgsConstructor
public class MessageWrapper<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息发送keys
     */
    @NonNull
    private String keys;

    @NonNull
    private T message;

    /**
     * 唯一标识，用户客户端幂等验证
     */
    private String uuid = UUID.randomUUID().toString();

    /**
     * 消息发送时间
     */
    private Long timestamp = System.currentTimeMillis();
}
