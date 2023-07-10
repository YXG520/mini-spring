package com.zhouyu;

import com.ZhouyuApplicationContext;
import com.zhouyu.service.UserService;

import java.lang.reflect.InvocationTargetException;

public class Test {

    public static void main(String[] args) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        System.out.println("--------------------------容器初始化---------------------------------");

        ZhouyuApplicationContext applicationContext = new ZhouyuApplicationContext(AppConfig.class);
        System.out.println("----------------------------------------测试----------------------------------------------------------------");
        System.out.println("----------------------------------------测试----------------------------------------------------------------");
        System.out.println("----------------------------------------测试----------------------------------------------------------------");


        System.out.println("--------------------------测试单例-----------------------------------");
//        System.out.println(applicationContext.getBean("userServiceImpl"));
//        System.out.println(applicationContext.getBean("userServiceImpl"));
        System.out.println("两次返回的bean地址是否一致：" + (applicationContext.getBean("userServiceImpl") == applicationContext.getBean("userServiceImpl")));
//        System.out.println(applicationContext.getBean("userService"));
        UserService userService = (UserService) applicationContext.getBean("userServiceImpl");
        System.out.println("----------------------------测试非循环依赖的注入-------------------------------");
        userService.testDI();
        System.out.println("----------------------------测试BeanName接口的实现-------------------------------");
        userService.testBeanName();

        System.out.println("----------------------------测试InitializingBean接口的实现-------------------------------");
        userService.testAfterPropertiesSet();

        userService.before1();
        userService.before2();
        userService.after();
        userService.around();
    }
}
