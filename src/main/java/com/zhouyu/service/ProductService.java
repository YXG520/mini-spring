package com.zhouyu.service;

import com.spring.Autowired;
import com.spring.Component;
import com.sun.org.apache.xpath.internal.operations.Or;

@Component("productService")
public class ProductService {

    @Autowired
    OrderService orderService;

    public void offerProduct() {
        System.out.println("I am providing products....");
    }

    public void callOrderService() {
        orderService.provideOrderService();
    }


}
