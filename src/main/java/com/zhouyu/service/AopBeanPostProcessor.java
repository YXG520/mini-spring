package com.zhouyu.service;

import com.ZhouyuApplicationContext;
import com.pojo.AdviceInfo;
import com.spring.BeanPostProcessor;
import com.spring.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

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
        String clasSymbolName = bean.getClass().getName();
//        System.out.println("clasSymbolName："+clasSymbolName);
        clasSymbolName = Character.toUpperCase(beanName.charAt(0))+beanName.substring(1);
        List<AdviceInfo> adviceInfos = joinPointMap.get(clasSymbolName);
//        System.out.println("singletonObjects:"+singletonObjects.toString());
        if (adviceInfos != null && adviceInfos.size() != 0) {
            System.out.println("为"+clasSymbolName + "创建代理对象，");
            // jdk创建动态代理
            Object proxyObj = Proxy.newProxyInstance(
                    bean.getClass().getClassLoader(),
                    bean.getClass().getInterfaces(),
                    (proxy, method, args) -> {

                        // 在目标方法调用前执行逻辑
                        System.out.println("Before method execution， add before advice");

                        for (AdviceInfo adviceInfo : adviceInfos) {
                            if (adviceInfo.getType().equals("Before") || adviceInfo.getType().equals("Around")) {
                                invokeAopMethod(adviceInfo);
                            }
                        }
                        // 调用目标方法
                        Object result = method.invoke(bean, args);

                        // 在目标方法调用后执行逻辑
                        System.out.println("After method execution, add after advice");
                        for (AdviceInfo adviceInfo : adviceInfos) {
                            if (adviceInfo.getType().equals("After") || adviceInfo.getType().equals("Around")) {
                                invokeAopMethod(adviceInfo);
                            }
                        }
                        return result;
                    });

            System.out.println("返回了一个AOP代理对象....");
            return proxyObj;
        }
        System.out.println("clasSymbolName:" + clasSymbolName +", 未生成代理对象");

        return bean;
    }

    public void invokeAopMethod(AdviceInfo adviceInfo) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (!singletonObjects.containsKey(adviceInfo.getAspectBeanName())) {
            System.out.println("单例池中找不到该对象, 从定义map中取");

            ZhouyuApplicationContext zhouyuApplicationContext = new ZhouyuApplicationContext();
            System.out.println("aspect beanName是：" + adviceInfo.getAspectBeanName());
            Object aspectBean = zhouyuApplicationContext.getBean(adviceInfo.getAspectBeanName());
            if (aspectBean == null) {
                // 创建bean
                aspectBean = zhouyuApplicationContext.createBean(adviceInfo.getAspectBeanName(), beanDefinitionMap.get(adviceInfo.getAspectBeanName()));
                singletonObjects.put(adviceInfo.getAspectBeanName(), aspectBean);
            }
            Method adviceMethod = aspectBean.getClass().getDeclaredMethod(adviceInfo.getAdviceName());
            adviceMethod.invoke(aspectBean);
        } else {
            Object aspectBean=  singletonObjects.get(adviceInfo.getAspectBeanName());

            Method aspectMethod = aspectBean.getClass().getDeclaredMethod(adviceInfo.getAdviceName());
            // 调用所有的前置方法
            aspectMethod.invoke(aspectBean);
        }
    }
}
