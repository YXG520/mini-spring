package com.zhouyu.service;

import com.ZhouyuApplicationContext;
import com.pojo.AdviceInfo;
import com.spring.BeanDefinition;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class AopInvocationHandler implements InvocationHandler {

    // 存储原始对象
    private final Object originalObject;
    private final List<AdviceInfo> adviceInfos;

    private Map<String, Object> singletonObjects;

    private Map<String, BeanDefinition> beanDefinitionMap;

    // 构造函数负责传递参数
    public AopInvocationHandler(Object originalObject,
                               List<AdviceInfo> adviceInfos,
                               Map<String, Object> singletonObjects,
                               Map<String, BeanDefinition> beanDefinitionMap) {

        this.originalObject = originalObject;
        this.adviceInfos = adviceInfos;
        this.singletonObjects = singletonObjects;
        this.beanDefinitionMap = beanDefinitionMap;
    }

    public void printAdviceList(List<AdviceInfo> adviceInfos) {
        System.out.println("打印切面：：");
        for (AdviceInfo adviceInfo : adviceInfos) {
            System.out.println(adviceInfo); // 假设AdviceInfo类已经实现了toString()方法
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {


        for (AdviceInfo adviceInfo : adviceInfos) {
            if (adviceInfo.getType().equals("Before") || adviceInfo.getType().equals("Around")) {
//                System.out.println("adviceInfo.getType()： " + adviceInfo.getType() +", adviceInfo.getAffectedMethod():"+adviceInfo.getAffectedMethod() + ", method.getName()："+method.getName());
                if (adviceInfo.getAffectedMethod().equals(method.getName())) {
                    // 如果方法名和保存的方法名匹配，则可以执行通知
                    invokeAopMethod(adviceInfo);
                }
            }
        }
        // 调用目标方法
        Object result = method.invoke(originalObject, args);

        // 在目标方法调用后执行逻辑
//                        System.out.println("After method execution, add after advice");
        for (AdviceInfo adviceInfo : adviceInfos) {
            if (adviceInfo.getType().equals("After") || adviceInfo.getType().equals("Around")) {
                System.out.println("adviceInfo.getAffectedMethod():"+adviceInfo.getAffectedMethod() + ", method.getName()："+method.getName());

                if (adviceInfo.getAffectedMethod().equals(method.getName())) {
                    // 如果方法名和保存的方法名匹配，则可以执行通知
                    invokeAopMethod(adviceInfo);
                }
            }
        }
        return result;
    }

    public void invokeAopMethod(AdviceInfo adviceInfo) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Object aspectBean = null;
        if (!singletonObjects.containsKey(adviceInfo.getAspectBeanName())) {
            System.out.println("单例池中找不到该对象, 从定义map中取得切面");
            ZhouyuApplicationContext zhouyuApplicationContext = new ZhouyuApplicationContext();
            System.out.println("aspect beanName是：" + adviceInfo.getAspectBeanName());
            aspectBean = zhouyuApplicationContext.getBean(adviceInfo.getAspectBeanName());
            if (aspectBean == null) {
                // 创建bean
                aspectBean = zhouyuApplicationContext.createBean(adviceInfo.getAspectBeanName(), beanDefinitionMap.get(adviceInfo.getAspectBeanName()));
                singletonObjects.put(adviceInfo.getAspectBeanName(), aspectBean);
            }
        } else {
            aspectBean=  singletonObjects.get(adviceInfo.getAspectBeanName());
        }

        Method aspectMethod = aspectBean.getClass().getDeclaredMethod(adviceInfo.getAdviceName());
        // 调用所有的前置方法
        aspectMethod.invoke(aspectBean);
    }
}
