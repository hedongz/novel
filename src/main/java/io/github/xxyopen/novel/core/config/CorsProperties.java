package io.github.xxyopen.novel.core.config;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 创建 CorsProperties 类来绑定 CORS 配置属性
 * 跨域配置属性
 *
 * @author hedong
 * @date 2022/5/17
 */
@ConfigurationProperties(prefix = "novel.cors")
@Data
public class CorsProperties {

    // 允许跨域的域名
    private List<String> allowOrigins;
}
