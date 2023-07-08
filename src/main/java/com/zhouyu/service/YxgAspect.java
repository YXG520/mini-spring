package com.zhouyu.service;

import com.spring.*;

@Aspect
@Component("yxgAspect")
public class YxgAspect {
    // @Before("public void com.zhouyu.service.UserService.test()")
    @Before("com.zhouyu.service.UserServiceImpl.before1;com.zhouyu.service.UserServiceImpl.before2")
    public void before() {
        System.out.println("前置通知执行成功");
    }

    @After("com.zhouyu.service.UserServiceImpl.after")
    public void after() {
        System.out.println("后置通知执行成功");
    }

    @Around("com.zhouyu.service.UserServiceImpl.around")
    public void around() {
        System.out.println("环绕通知执行成功");
    }



}
