package io.github.xxyopen.novel.core.listener;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import io.github.xxyopen.novel.core.constant.AmqpConsts;
import io.github.xxyopen.novel.core.constant.EsConsts;
import io.github.xxyopen.novel.dao.entity.BookInfo;
import io.github.xxyopen.novel.dao.mapper.BookInfoMapper;
import io.github.xxyopen.novel.dto.es.EsBookDto;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Rabbit 队列监听器
 *
 * @author xiongxiaoyang
 * @date 2022/5/25
 */
@Component
//@ConditionalOnProperty(prefix = "spring", name = {"elasticsearch.enabled", "amqp.enabled"}, havingValue = "true")
@ConditionalOnProperty(prefix = "spring", name = {"amqp.enabled"}, havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RabbitQueueListener {

    private final BookInfoMapper bookInfoMapper;

//    private final ElasticsearchClient esClient;

    /**
     * 监听小说信息改变的 ES 更新队列，更新最新小说信息到 ES
     */
//    @RabbitListener(queues = AmqpConsts.BookChangeMq.QUEUE_ES_UPDATE)
    @SneakyThrows
    public void updateEsBook(Long bookId) {
        BookInfo bookInfo = bookInfoMapper.selectById(bookId);

        RestClient restClient = RestClient.builder(new HttpHost("localhost", 9200)).build();

        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

        ElasticsearchClient esClient = new ElasticsearchClient(transport);

        IndexResponse response = esClient.index(i -> i
            .index(EsConsts.BookIndex.INDEX_NAME)
            .id(bookInfo.getId().toString())
            .document(EsBookDto.build(bookInfo))
        );
        log.info("Indexed with version " + response.version());
    }

}
