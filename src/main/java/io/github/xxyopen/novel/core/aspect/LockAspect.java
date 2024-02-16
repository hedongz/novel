package io.github.xxyopen.novel.core.aspect;

import io.github.xxyopen.novel.core.annotation.Key;
import io.github.xxyopen.novel.core.annotation.Lock;
import io.github.xxyopen.novel.core.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁切面，用于控制带有@Lock注解的方法的并发访问
 *
 * @author xiongxiaoyang
 * @date 2022/6/20
 */
@Aspect // 声明为切面类
@Component // 注册为Spring组件
@RequiredArgsConstructor // 自动生成构造方法，用于注入依赖
public class LockAspect {
    
    private final RedissonClient redissonClient; // 用于获取分布式锁的Redisson客户端

    private static final String KEY_PREFIX = "Lock"; // 锁键的前缀

    private static final String KEY_SEPARATOR = "::"; // 锁键部分分隔符

    /**
     * 环绕通知方法，在被@Lock注解标记的方法调用前后织入切面逻辑
     */
    @Around(value = "@annotation(io.github.xxyopen.novel.core.annotation.Lock)")
    @SneakyThrows // 异常处理注解，简化异常处理代码
    public Object doAround(ProceedingJoinPoint joinPoint) {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = methodSignature.getMethod(); // 目标方法
        Lock lock = targetMethod.getAnnotation(Lock.class); // 获取@Lock注解  从targetMethod中提取Lock注解的信息。
        // 根据prefix、目标方法和其参数构建一个唯一的锁键
        String lockKey = KEY_PREFIX + buildLockKey(lock.prefix(), targetMethod, joinPoint.getArgs());  // 构建锁键
        RLock rLock = redissonClient.getLock(lockKey); // 获取分布式锁实例
        // 尝试获取锁，如果获取成功则执行目标方法，否则抛出异常
        if (lock.isWait() ? rLock.tryLock(lock.waitTime(), TimeUnit.SECONDS) : rLock.tryLock()) {
            try {
                return joinPoint.proceed(); // 执行目标方法
            } finally {
                rLock.unlock(); // 释放锁
            }
        }
        // 获取锁失败，抛出自定义业务异常
        throw new BusinessException(lock.failCode());
    }

    /**
     * 构建分布式锁的键
     */
    private String buildLockKey(String prefix, Method method, Object[] args) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(prefix)) {
            builder.append(KEY_SEPARATOR).append(prefix);  // 添加前缀
        }
        Parameter[] parameters = method.getParameters();
        for (int i = 0; i < parameters.length; i++) {
            builder.append(KEY_SEPARATOR);
            if (parameters[i].isAnnotationPresent(Key.class)) {  // 如果参数标记了@Key注解
                Key key = parameters[i].getAnnotation(Key.class);
                builder.append(parseKeyExpr(key.expr(), args[i])); // 解析表达式，生成锁键的一部分
            }
        }
        return builder.toString();
    }

    private String parseKeyExpr(String expr, Object arg) {
        if (!StringUtils.hasText(expr)) {
            return arg.toString(); // 表达式为空，直接使用参数值
        }
        ExpressionParser parser = new SpelExpressionParser(); // SPEL
        Expression expression = parser.parseExpression(expr, new TemplateParserContext());
        return expression.getValue(arg, String.class); // 解析表达式并代入参数，生成锁键的一部分
    }

}
