package com.zhouyu.service;

import com.spring.Autowired;
import com.spring.Component;
import com.spring.Scope;

@Component("orderService")
//@Scope("prototype")
public class OrderService {

    @Autowired
    ProductService productService;

    // 尝试自己注入自己
    @Autowired
    OrderService orderService;

    public void provideOrderService() {
        System.out.println("I am orderService...");
    }

    public void callProductService() {
        productService.offerProduct();
    }
    public void callSelfService() {
        System.out.print("ready to test self AOP: ");
        orderService.provideOrderService();
    }


}
