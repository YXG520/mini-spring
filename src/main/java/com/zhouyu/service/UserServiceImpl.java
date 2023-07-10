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

    private String initTestProperty;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // 测试beanName接口
    @Override
    public void setBeanName(String beanName) {
        this.beanName = beanName;
        System.out.println("打印beanName:" + this.beanName);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        System.out.println("afterPropertiesSet初始化开始，准备进行...");
        this.initTestProperty = "successfully afterPropertiesSet";
    }


    @Override
    public void test() {
//        System.out.println(orderService);
//        System.out.println(beanName);
//        System.out.println(name);
//        System.out.println("执行orderService:"+orderService.provideOrderService(););
    }

    @Override
    public void testDI() {
        orderService.provideOrderService();
    }


    @Override
    public void testBeanName() {
        System.out.println("测试beanName:" + this.beanName);
    }

    @Override
    public void testAfterPropertiesSet() {
        System.out.println("测试testAfterPropertiesSet:" + this.initTestProperty);
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
