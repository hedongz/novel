package io.github.xxyopen.novel.core.json.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

/**
 * 用户名序列化器（敏感信息，不应该在页面上完全显示）
 *
 * @author xiongxiaoyang
 * @date 2022/5/20
 */
public class UsernameSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String s, JsonGenerator jsonGenerator,
        SerializerProvider serializerProvider) throws IOException {
        // 定义一个 JSON 序列化器，
        // 在 Spring MVC 序列化我们返回的 Java 对象为 JSON 字符串时格式化一下用户名，
        // 隐藏中间的 4 位数字为 ****
        jsonGenerator.writeString(s.substring(0, 4) + "****" + s.substring(8));
    }

}
