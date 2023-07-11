package com.zhouyu.service;

import com.spring.Autowired;
import com.spring.Component;

@Component("boyServiceImpl")
public class BoyServiceImpl implements BoyService{

    @Autowired
    GirlService girlServiceImpl;


    @Override
    public void sayHello2Girls() {
        girlServiceImpl.offerService();
    }

    @Override
    public void offerService() {
        System.out.println("I am a boy, ready to offer service to girls");
    }
}
