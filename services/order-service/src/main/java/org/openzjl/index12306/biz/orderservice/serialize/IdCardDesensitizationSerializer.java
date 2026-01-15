package org.openzjl.index12306.biz.orderservice.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 *
 * @author zhangjlk
 * @date 2026/1/14 12:04
 */
public class IdCardDesensitizationSerializer extends JsonSerializer<String> {


    @Override
    public void serialize(String s, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

    }
}
