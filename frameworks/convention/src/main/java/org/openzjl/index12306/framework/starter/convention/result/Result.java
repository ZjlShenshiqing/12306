package org.openzjl.index12306.framework.starter.convention.result;

import com.sun.xml.internal.ws.developer.Serialization;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 全局返回对象
 * @author zhangjlk
 * @date 2025/9/15 20:42
 */
@Data
/**
 * Lombok 在编译时会自动重写 setter 方法：
 *     public User setName(String name) { // 注意返回类型是 User 而不是 void
 *         this.name = name;
 *         return this; // 关键：返回 this
 *     }
 *
 * 这样就可以完成链式调用：
 * // 链式调用（优雅）
 * User user = new User()
 *     .setName("Alice")
 *     .setAge(25)
 *     .setEmail("alice@example.com");
 */
@Accessors(chain = true)
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 657901648193301L;

    /**
     * 正确返回码
     */
    public static final String SUCCESS_CODE = "0";

    /**
     * 返回码
     */
    private String code;

    /**
     * 返回信息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 请求ID
     */
    private String requestId;

    public boolean isSuccess() {
        return SUCCESS_CODE.equals(code);
    }
}
