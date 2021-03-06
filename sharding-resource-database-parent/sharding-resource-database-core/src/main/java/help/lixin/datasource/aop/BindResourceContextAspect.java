package help.lixin.datasource.aop;

import help.lixin.resource.context.ResourceContext;
import help.lixin.resource.context.ResourceContextHolder;
import help.lixin.resource.route.IBuildResourceContextService;
import help.lixin.resource.route.contxt.DefaultInvocation;
import help.lixin.resource.route.contxt.Invocation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;

/**
 * 绑定上下文,要注意:这个注解需要在@Transactional注解的前面,因为@Transactional需要拿Connection了.
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class BindResourceContextAspect {

    @Autowired
    private IBuildResourceContextService buildResourceContextService;

    @Pointcut("@within(org.springframework.transaction.annotation.Transactional) || @annotation(org.springframework.transaction.annotation.Transactional)")
    public void bindResourceContextPointcut() {
    }

    @Around("bindResourceContextPointcut()")
    public Object exec(final ProceedingJoinPoint point) throws Throwable {
        // 1. 构建:Invocation
        Invocation invocation = buildContext(point);
        // 2. 委托给:IBuildResourceContextService构建:ResourceContext
        ResourceContext resourceContext = buildResourceContextService.build(invocation);
        try {
            // 3. 绑定信息到上下文中
            ResourceContextHolder.bind(resourceContext);
            return point.proceed();
        } finally {
            // 4. 解绑线程上下文的信息
            ResourceContextHolder.unBind();
        }
    }

    protected Invocation buildContext(final ProceedingJoinPoint point) {
        MethodSignature methodSignature = (MethodSignature) point.getSignature();
        Method method = methodSignature.getMethod();
        Object target = point.getTarget();
        Class<?> clazz = target.getClass();
        boolean aopProxy = AopUtils.isAopProxy(point.getTarget());
        if (aopProxy) {
            clazz = AopUtils.getTargetClass(point.getTarget());
        }
        // 构建上下文
        Invocation ctx = DefaultInvocation.newBuild()
                .clazz(clazz)
                .method(method)
                .instance(target)
                .properties("_self", point)
                .build();
        return ctx;
    }
}
