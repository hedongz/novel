package io.github.xxyopen.novel.core.task;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Time;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.github.xxyopen.novel.core.constant.DatabaseConsts;
import io.github.xxyopen.novel.core.constant.EsConsts;
import io.github.xxyopen.novel.dao.entity.BookInfo;
import io.github.xxyopen.novel.dao.mapper.BookInfoMapper;
import io.github.xxyopen.novel.dto.es.EsBookDto;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 小说数据（数据库）同步到 elasticsearch
 *
 * @author xiongxiaoyang
 * @date 2022/5/23
 */
//@ConditionalOnProperty(prefix = "spring.elasticsearch", name = "enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
@Slf4j
public class BookToEsTask {

    private final BookInfoMapper bookInfoMapper;

//    private final ElasticsearchClient elasticsearchClient;

    /**
     * 每月凌晨做一次全量数据同步
     */
    @SneakyThrows
//    @XxlJob("saveToEsJobHandler")
//    @Scheduled(cron = "*/5 * * * * *")
    public ReturnT<String> saveToEs() {
        try {
            QueryWrapper<BookInfo> queryWrapper = new QueryWrapper<>();
            List<BookInfo> bookInfos;
            long maxId = 0;
            for (; ; ) {
                queryWrapper.clear();
                // gt是大于，lt是小于
                // 数据量过大，批量导入！！！
                queryWrapper
                    .orderByAsc(DatabaseConsts.CommonColumnEnum.ID.getName())
                    .gt(DatabaseConsts.CommonColumnEnum.ID.getName(), maxId) // 从记录的最大id的下一步开始（gt是大于）
                    .gt(DatabaseConsts.BookTable.COLUMN_WORD_COUNT, 0) // 字数大于0
                    .last(DatabaseConsts.SqlEnum.LIMIT_30.getSql()); // 限制30个
                bookInfos = bookInfoMapper.selectList(queryWrapper);
                if (bookInfos.isEmpty()) {
                    break;
                }
                BulkRequest.Builder br = new BulkRequest.Builder(); // BulkRequest

                for (BookInfo book : bookInfos) {
                    br.operations(op -> op
                        .index(idx -> idx
                            .index(EsConsts.BookIndex.INDEX_NAME) // 索引库名称：book
                            .id(book.getId().toString()) // id：书籍的id
                            .document(EsBookDto.build(book)) // 将数据库中的小转化为ES文档（一条一条的记录），使用了建造者模式
                        )
                    ).timeout(Time.of(t -> t.time("10s"))); // 超时时间 10s
                    maxId = book.getId();
                }

                RestClient restClient = RestClient.builder(new HttpHost("localhost", 9200)).build();
                RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
                ElasticsearchClient esClient = new ElasticsearchClient(transport);

                BulkResponse result = esClient.bulk(br.build()); // 批量导入！

                // Log errors, if any
                if (result.errors()) {
                    log.error("Bulk had errors"); // 同步失败
                    for (BulkResponseItem item : result.items()) {
                        if (item.error() != null) {
                            log.error(item.error().reason());
                        }
                    }
                }
            }
            return ReturnT.SUCCESS; // 成功！
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return ReturnT.FAIL;
        }
    }

}
