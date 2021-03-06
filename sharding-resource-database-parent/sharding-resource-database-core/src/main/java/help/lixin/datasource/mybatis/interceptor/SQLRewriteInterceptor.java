package help.lixin.datasource.mybatis.interceptor;

import help.lixin.resource.sql.ISQLRewriteService;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 通过对JDBC(Connection)进行拦截,就能对SQL语句进行改写了(能适用:mybatis/hibernate/jdbcTemplate...),为什么还要对mybatis进行SQL改写?
 * 原因在于:我们的研发人员在开发环境或者生产环境,可能开启SQL日志,而针对Connection进行重写后,SQL语句的输出要对开发进行培训,对开发不够友好.
 * 但是,为了防止一次请求N次重写SQL语句,做了控制,以防止多次重写SQL.
 */
@Intercepts({
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class SQLRewriteInterceptor implements Interceptor {

    private final Set<String> supportMethods = new HashSet<>();

    {
        supportMethods.add("query");
        supportMethods.add("update");
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        Object target = invocation.getTarget();
        Method method = invocation.getMethod();
        String name = method.getName();
        if (supportMethods.contains(name) && args.length > 1) {
            MappedStatement ms = (MappedStatement) args[0];
            Object oldParameter = args[1];
            BoundSql oldBoundSql = ms.getBoundSql(oldParameter);
            // 重写SQL语句之前
            String overrideBeforeSql = oldBoundSql.getSql();
            // 重写SQL语句之后
            String rewriteAfterSql = overrideBeforeSql;
            boolean present = getSQLRewriteService().isPresent();
            if (present) {
                ISQLRewriteService sqlRewriteService = getSQLRewriteService().get();
                rewriteAfterSql = sqlRewriteService.rewrite(rewriteAfterSql, null);
            }

            BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), rewriteAfterSql, oldBoundSql.getParameterMappings(), oldBoundSql.getParameterObject());
            SqlSource newSqlSource = (parameterObject) -> {
                return newBoundSql;
            };

            MappedStatement newMs = copyFromMappedStatement(ms, newSqlSource);

            // 拷贝参数
            for (ParameterMapping mapping : oldBoundSql.getParameterMappings()) {
                String prop = mapping.getProperty();
                if (oldBoundSql.hasAdditionalParameter(prop)) {
                    newBoundSql.setAdditionalParameter(prop, oldBoundSql.getAdditionalParameter(prop));
                }
            }

            // 构建新的:Invocation
            final Object[] newArgs = args;
            newArgs[0] = newMs;
            Invocation newInvocation = new Invocation(target, method, newArgs);
            return newInvocation.proceed();
        }

        return invocation.proceed();
    }

    protected Optional<ISQLRewriteService> getSQLRewriteService() {
        Iterator<ISQLRewriteService> iterator = ServiceLoader.load(ISQLRewriteService.class).iterator();
        if (iterator.hasNext()) {
            return Optional.of(iterator.next());
        }
        return Optional.empty();
    }

    protected MappedStatement copyFromMappedStatement(MappedStatement ms, SqlSource newSqlSource) {
        MappedStatement.Builder builder = new MappedStatement.Builder(ms.getConfiguration(), ms.getId(), newSqlSource, ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        if (ms.getKeyProperties() != null && ms.getKeyProperties().length > 0) {
            builder.keyProperty(ms.getKeyProperties()[0]);
        }
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(ms.getResultMaps());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());
        return builder.build();
    }
}
