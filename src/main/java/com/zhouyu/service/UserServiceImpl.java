package com.zhouyu.service;

import com.spring.Autowired;
import com.spring.BeanNameAware;
import com.spring.Component;
import com.spring.InitializingBean;

@Component("userServiceImpl")
//@Scope("prototype")
public class UserServiceImpl implements BeanNameAware, InitializingBean, UserService {

    @Autowired
    private OrderService orderService;

    // 获取当前bean的名字
    private String beanName;

    // 使用 beanPostProcess扩展机制获取当前bean的名字
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }



    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("afterPropertiesSet初始化");
    }

    public void test() {
//        System.out.println(orderService);
//        System.out.println(beanName);
//        System.out.println(name);
        System.out.println("执行orderService:"+orderService);
    }

    @Override
    public void before1() {
        System.out.println("execute before1");
        System.out.println("-------------");
    }

    @Override
    public void before2() {
        System.out.println("execute before2");
        System.out.println("-------------");

    }

    @Override
    public void after() {
        System.out.println("执行与后置通知绑定的方法");
        System.out.println("-------------");

    }

    @Override
    public void around() {
        System.out.println("执行与环绕通知绑定的方法");
        System.out.println("-------------");
    }
}
