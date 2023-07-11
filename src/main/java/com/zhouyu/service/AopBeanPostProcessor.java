package com.zhouyu.service;

import com.ZhouyuApplicationContext;
import com.spring.BeanPostProcessor;
import com.spring.Component;

import static com.ZhouyuApplicationContext.*;

@Component("aopBeanPostProcessor")
public class AopBeanPostProcessor implements BeanPostProcessor {


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        // 比如String.getClass().getName() 为java.lang.String
        System.out.println("执行aop的postProcessBeforeInitialization操作逻辑");
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        Object proxyObj = bean;
        // 看一下是否需要创建切面
        if (adviceInfoMap.containsKey(beanName)) {
            // 判断这个切面是否因为循环依赖而提前被创建
            if (singletonObjects.containsKey(beanName)) {
                proxyObj = singletonObjects.get(beanName);
            } else {
                ZhouyuApplicationContext zac = new ZhouyuApplicationContext();
                proxyObj = zac.createAopProxy(bean, adviceInfoMap.get(beanName));
            }

        }

        System.out.println("是否返回了一个新的AOP代理对象："+(proxyObj != bean));
        return proxyObj;
    }


}
