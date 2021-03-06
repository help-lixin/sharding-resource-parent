package help.lixin.resource.route.contxt;

import java.lang.reflect.Method;
import java.util.Map;

public interface Invocation {
    void setMethod(Method method);

    Method getMethod();

    void setClazz(Class<?> clazz);

    Class<?> getClazz();

    void setInstance(Object instance);

    Object getInstance();

    void setProperties(Map<String, Object> properties);

    Map<String, Object> getProperties();
}
