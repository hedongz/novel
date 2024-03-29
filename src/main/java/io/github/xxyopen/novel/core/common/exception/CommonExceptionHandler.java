package io.github.xxyopen.novel.core.common.exception;

import io.github.xxyopen.novel.core.common.constant.ErrorCodeEnum;
import io.github.xxyopen.novel.core.common.resp.RestResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 通用的异常处理器
 *
 * @author xiongxiaoyang
 * @date 2022/5/11
 */
@Slf4j
@RestControllerAdvice
public class CommonExceptionHandler {

    /**
     * 处理数据校验异常
     */
    // TODO:添加ResponseStatus注解，将异常映射为HTTP状态码。
    //  例如：@ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public RestResp<Void> handlerBindException(BindException e) {
        log.error(e.getMessage(), e); // 打印日志
        return RestResp.fail(ErrorCodeEnum.USER_REQUEST_PARAM_ERROR);
    }

    /**
     * 处理业务异常（自己定义的异常类）
     */
    @ExceptionHandler(BusinessException.class)
    public RestResp<Void> handlerBusinessException(BusinessException e) {
        log.error(e.getMessage(), e);
        return RestResp.fail(e.getErrorCodeEnum());
    }

    /**
     * 处理系统异常
     */
    @ExceptionHandler(Exception.class)
    public RestResp<Void> handlerException(Exception e) {
        log.error(e.getMessage(), e);
        return RestResp.error();
    }

}
