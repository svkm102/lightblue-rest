package com.redhat.lightblue.rest.auth.ldap;


public class LdapConfiguration {

    private String server;
    private Integer port;
    private String bindDn;
    private String bindDNPwd;
    private Boolean useSSL;
    private String trustStore;
    private String trustStorePassword;
    private Integer poolSize;

    public LdapConfiguration server (String server) {
        this.server = server;
        return this;
    }

    public LdapConfiguration port (Integer port) {
        this.port = port;
        return this;
    }

    public LdapConfiguration bindDn (String bindDn) {
        this.bindDn = bindDn;
        return this;
    }

    public LdapConfiguration bindDNPwd (String bindDNPwd) {
        this.bindDNPwd = bindDNPwd;
        return this;
    }

    public LdapConfiguration useSSL (Boolean useSSL) {
        this.useSSL = useSSL;
        return this;
    }

    public LdapConfiguration trustStore (String trustStore) {
        this.trustStore = trustStore;
        return this;
    }

    public LdapConfiguration trustStorePassword (String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
        return this;
    }

    public LdapConfiguration poolSize (Integer poolSize) {
        this.poolSize = poolSize;
        return this;
    }

    public String getServer() {
        return server;
    }

    public Integer getPort() {
        return port;
    }

    public String getBindDn() {
        return bindDn;
    }

    public String getBindDNPwd() {
        return bindDNPwd;
    }

    public Boolean getUseSSL() {
        return useSSL;
    }

    public String getTrustStore() {
        return trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public Integer getPoolSize() {
        return poolSize;
    }
}