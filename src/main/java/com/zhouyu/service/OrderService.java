package com.zhouyu.service;

import com.spring.Component;
import com.spring.Scope;

@Component("orderService")
//@Scope("prototype")
public class OrderService {

    public void provideOrderService() {
        System.out.println("I am orderService...");
    }

}
