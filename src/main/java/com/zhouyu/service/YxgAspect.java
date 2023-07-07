package com.zhouyu.service;

import com.spring.Aspect;
import com.spring.Before;
import com.spring.Component;

@Aspect
@Component("yxgAspect")
public class YxgAspect {
    // @Before("public void com.zhouyu.service.UserService.test()")
    @Before("execution(* com.example.myapp.service..*(..))")
    public void before() {
        System.out.println("yxgAspect的前置Aop");
    }



}
