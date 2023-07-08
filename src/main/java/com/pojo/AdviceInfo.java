package com.pojo;

public class AdviceInfo {
    // 全限定名
    private String fullPathName;

    // 通知类型，before，after，around
    private String type;

    // 切面的beanName, 方便加入代理时使用
    private String aspectBeanName;

    // 待切入的通知对应的方法名
    private String adviceName;

    // 作用点,jointPoint
    private String affectedMethod;


    public AdviceInfo(String fullPathName, String type, String aspectBeanName, String adviceName, String affectedMethod) {
        this.fullPathName = fullPathName;
        this.type = type;
        this.aspectBeanName = aspectBeanName;
        this.adviceName = adviceName;
        this.affectedMethod = affectedMethod;
    }

    public String getFullPathName() {
        return fullPathName;
    }

    public void setFullPathName(String fullPathName) {
        this.fullPathName = fullPathName;
    }
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAspectBeanName() {
        return aspectBeanName;
    }

    public void setAspectBeanName(String aspectBeanName) {
        this.aspectBeanName = aspectBeanName;
    }

    public String getAdviceName() {
        return adviceName;
    }

    public void setAdviceName(String adviceName) {
        this.adviceName = adviceName;
    }

    public String getAffectedMethod() {
        return affectedMethod;
    }

    public void setAffectedMethod(String affectedMethod) {
        this.affectedMethod = affectedMethod;
    }


}
