package com.zhouyu;

import com.ZhouyuApplicationContext;
import com.zhouyu.service.UserService;

import java.lang.reflect.InvocationTargetException;

public class Test {

    public static void main(String[] args) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        ZhouyuApplicationContext applicationContext = new ZhouyuApplicationContext(AppConfig.class);

//        System.out.println(applicationContext.getBean("userServiceImpl"));
//        System.out.println(applicationContext.getBean("userService"));
//        System.out.println(applicationContext.getBean("userService"));

        UserService userService = (UserService) applicationContext.getBean("userServiceImpl");
        userService.test();
        userService.before1();
        userService.before2();
        userService.after();
        userService.around();
    }
}
