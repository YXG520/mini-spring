package com.zhouyu.service;

import com.spring.Autowired;
import com.spring.Component;

@Component("girlServiceImpl")
public class GirlServiceImpl implements GirlService {

    @Autowired
    BoyService boyServiceImpl;


    @Override
    public void sayHello2Boys() {
        boyServiceImpl.offerService();
    }

    @Override
    public void offerService() {
        System.out.println("I am a girl, ready to offer service to boys");
    }
}
