package com.spring;

public class BeanDefinition {
    private Class clazz;
    private String scope;

    private boolean isAspect;

    public boolean isAspect() {
        return isAspect;
    }

    public void setAspect(boolean aspect) {
        isAspect = aspect;
    }

    public Class getClazz() {
        return clazz;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }
    public BeanDefinition() {
    }
    public BeanDefinition(Class clazz) {
        this.clazz = clazz;
    }
}
