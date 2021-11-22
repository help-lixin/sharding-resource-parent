package help.lixin.resource.context;

import java.util.*;

public abstract class AbstractResourceContextInfo implements ResourceContextInfo {
    protected String tenantId;
    protected String microServiceName;
    protected String env;
    protected List<String> envs = new ArrayList<>(0);
    protected Map<String, String> properties = new HashMap<>();

    public abstract static class Build {
        protected String tenantId;
        protected String microServiceName;
        protected String env;
        protected Map<String, String> properties = new HashMap<>();

        public Build tenantId(String tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        public Build microServiceName(String microServiceName) {
            this.microServiceName = microServiceName;
            return this;
        }

        public Build env(String env) {
            this.env = env;
            return this;
        }

        public Build properties(Map<String, String> properties) {
            this.properties = properties;
            return this;
        }

        public abstract ResourceContextInfo build();
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setMicroServiceName(String microServiceName) {
        this.microServiceName = microServiceName;
    }

    @Override
    public String getMicroServiceName() {
        return microServiceName;
    }

    @Override
    public void setEnv(String env) {
        this.env = env;
        if (null != env) {
            String[] strings = env.split(",");
            List<String> stringList = Arrays.asList(strings);
            envs.addAll(stringList);
        }
    }

    @Override
    public List<String> getEnv() {
        return envs;
    }

    @Override
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }
}