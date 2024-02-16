package io.github.xxyopen.novel.manager.cache;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.github.xxyopen.novel.core.constant.CacheConsts;
import io.github.xxyopen.novel.core.constant.DatabaseConsts;
import io.github.xxyopen.novel.dao.entity.BookInfo;
import io.github.xxyopen.novel.dao.entity.HomeBook;
import io.github.xxyopen.novel.dao.mapper.BookInfoMapper;
import io.github.xxyopen.novel.dao.mapper.HomeBookMapper;
import io.github.xxyopen.novel.dto.resp.HomeBookRespDto;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 首页推荐小说 缓存管理类
 *
 * @author hedong
 * @date 2022/5/12
 */
@Component
@RequiredArgsConstructor
public class HomeBookCacheManager {

    private final HomeBookMapper homeBookMapper;

    private final BookInfoMapper bookInfoMapper;

    /**
     * 查询首页小说推荐，并放入缓存中
     *
     * 总之：返回小说推荐的响应对象列表，并放入缓存
     *
     * @Cacheable注解：
     * cacheNames/value ：用来指定缓存组件的名字
     *
     * key ：缓存数据时使用的 key，可以用它来指定。默认是使用方法参数的值。（这个 key 你可以使用 spEL 表达式来编写）
     *
     * keyGenerator ：key 的生成器。 key 和 keyGenerator 二选一使用
     *
     * cacheManager ：可以用来指定缓存管理器。从哪个缓存管理器里面获取缓存。
     *
     * condition ：可以用来指定符合条件的情况下才缓存
     *
     * unless ：否定缓存。当 unless 指定的条件为 true ，方法的返回值就不会被缓存。当然你也可以获取到结果进行判断。（通过 #result 获取方法结果）
     *
     * sync ：是否使用异步模式。
     */
    // 标注缓存注解：（cacheManager：缓存管理器，value：缓存组件的名字）
    // 使用 @Cacheable 注解就可以将运行结果缓存，以后查询相同的数据，直接从缓存中取，不需要调用方法。
    @Cacheable(cacheManager = CacheConsts.CAFFEINE_CACHE_MANAGER, value = CacheConsts.HOME_BOOK_CACHE_NAME)
    public List<HomeBookRespDto> listHomeBooks() {
        // 从首页小说推荐表中查询出需要推荐的小说
        QueryWrapper<HomeBook> queryWrapper = new QueryWrapper<>();
        // 按照sort字段的大小升序
        queryWrapper.orderByAsc(DatabaseConsts.CommonColumnEnum.SORT.getName());
        // 将推荐小说封装到homeBooks
        List<HomeBook> homeBooks = homeBookMapper.selectList(queryWrapper);

        // 获取<推荐小说>ID列表
        if (!CollectionUtils.isEmpty(homeBooks)) { // 如果查出的homeBook不为空
            // 获取homeBooks的Id，加到列表
            List<Long> bookIds = homeBooks.stream()
                .map(HomeBook::getBookId)
                .toList();

            // 根据小说ID列表查询相关的<小说信息>列表
            QueryWrapper<BookInfo> bookInfoQueryWrapper = new QueryWrapper<>();
            bookInfoQueryWrapper.in(DatabaseConsts.CommonColumnEnum.ID.getName(), bookIds);
            List<BookInfo> bookInfos = bookInfoMapper.selectList(bookInfoQueryWrapper);

            // 组装 HomeBookRespDto 列表数据并返回
            if (!CollectionUtils.isEmpty(bookInfos)) {
                Map<Long, BookInfo> bookInfoMap = bookInfos.stream()
                    .collect(Collectors.toMap(BookInfo::getId, Function.identity()));

                return homeBooks.stream().map(v -> {
                    BookInfo bookInfo = bookInfoMap.get(v.getBookId());
                    HomeBookRespDto bookRespDto = new HomeBookRespDto();
                    bookRespDto.setType(v.getType());
                    bookRespDto.setBookId(v.getBookId());
                    bookRespDto.setBookName(bookInfo.getBookName());
                    bookRespDto.setPicUrl(bookInfo.getPicUrl());
                    bookRespDto.setAuthorName(bookInfo.getAuthorName());
                    bookRespDto.setBookDesc(bookInfo.getBookDesc());
                    return bookRespDto;
                }).toList();

            }

        }

        return Collections.emptyList();
    }

}
