package com.zhouyu.service;

public interface UserService {
    void test();

    void before1();

    void before2();

    void after();

    void around();

    void testBeanName();

    void testAfterPropertiesSet();

    void testDI();

}