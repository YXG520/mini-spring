package com;

import com.pojo.AdviceInfo;
import com.spring.*;
import com.zhouyu.service.AopInvocationHandler;
import com.zhouyu.service.OrderService;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ZhouyuApplicationContext {
    private Class configClass;

    // 单例池
    public static ConcurrentHashMap<String, Object> singletonObjects = new ConcurrentHashMap<>();
    // 定义池
    public static ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    public List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    public static Map<String, List<AdviceInfo>> adviceInfoMap = new ConcurrentHashMap<>();

    // 创建一个map用于确认是否提前完成了beanPostProcessor接口的实现，即是否提前创建了代理对象
    public static Map<String, Boolean> isFinishBPP = new HashMap<>();

    public ZhouyuApplicationContext() {

    }

    public ZhouyuApplicationContext(Class configClass) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        this.configClass = configClass;
        System.out.println("调用构造方法");
        // 扫描
        scan(configClass);

        System.out.println("扫描后，beanDefinitionMap为："+beanDefinitionMap.toString());
        // 先生成所有的切面bean

        //
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            System.out.println("----beanDefinition.getClazz()： "+beanDefinition.getClazz() + ", 是否单例:"+beanDefinition.getScope().equals("singleton")
            + ", 是否已经在单例池子中：" + singletonObjects.containsKey(beanName));

            // 如果发现是单例bean，直接加入单例池
            if (beanDefinition.getScope().equals("singleton") && !singletonObjects.containsKey(beanName)) {
                Object bean = createBean(beanName,beanDefinition); // 因为是初始化检索bean阶段，所以还是需要创建bean的实例
                singletonObjects.put(beanName, bean);//放入单例池中
                System.out.println("beanName:"+beanName+":成功将bean放入单例池中");
            } else {
                // 如果是多例
            }
        }
    }


    // 创建一个bean
    public Object createBean(String beanName, BeanDefinition beanDefinition) {
        Class clazz = beanDefinition.getClazz();
        try {
            // 创建实例
            Object instance = clazz.getDeclaredConstructor().newInstance();
            Object proxyInstance = null;
            // 决定是否需要提前创建代理对象
//            if (beanDefinition.isAspect()) {
            if (adviceInfoMap.containsKey(beanName)) {
                // 如果一个bean需要代理对象，可以提前创建带AOP，带事务的代理对象
                proxyInstance = createAopProxy(instance, adviceInfoMap.get(beanName));
            }
            if (proxyInstance == null) {
                proxyInstance = instance;
            }
            // 立即加入到一级缓存中, 因为有可能不需要创建AOP，则此时只用加入原来的对象到单例池中
            singletonObjects.put(beanName, proxyInstance);

            // 给原来的对象进行属性注入
            populateBean(clazz,instance);

            // 初始化
            initializeBean(instance, beanName);

            System.out.println("返回instance: "+ beanName);
            return proxyInstance;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // 创建代理对象
    public Object createAopProxy(Object bean, List<AdviceInfo> adviceInfos) {
        Object proxyObj = bean;
        if (adviceInfos != null && adviceInfos.size() != 0) {
            // jdk创建动态代理

//            proxyObj = Proxy.newProxyInstance(
//                    bean.getClass().getClassLoader(),
//                    bean.getClass().getInterfaces(),
//                    (proxy, method, args) -> {
//                        // 在目标方法调用前执行逻辑
////                        System.out.println("Before method execution， add before advice");
//                        for (AdviceInfo adviceInfo : adviceInfos) {
//                            if (adviceInfo.getType().equals("Before") || adviceInfo.getType().equals("Around")) {
////                                System.out.println("adviceInfo.getAffectedMethod():"+adviceInfo.getAffectedMethod() + ", method.getName()："+method.getName());
//                                if (adviceInfo.getAffectedMethod().equals(method.getName())) {
//                                    // 如果方法名和保存的方法名匹配，则可以执行通知
//                                    invokeAopMethod(adviceInfo);
//                                }
//                            }
//                        }
//                        // 调用目标方法
//                        Object result = method.invoke(bean, args);
//
//                        // 在目标方法调用后执行逻辑
////                        System.out.println("After method execution, add after advice");
//                        for (AdviceInfo adviceInfo : adviceInfos) {
//                            if (adviceInfo.getType().equals("After") || adviceInfo.getType().equals("Around")) {
//                                if (adviceInfo.getAffectedMethod().equals(method.getName())) {
//                                    // 如果方法名和保存的方法名匹配，则可以执行通知
//                                    invokeAopMethod(adviceInfo);
//                                }
//                            }
//                        }
//                        return result;
//                    });
            proxyObj = Proxy.newProxyInstance(
                    bean.getClass().getClassLoader(),
                    bean.getClass().getInterfaces(), new AopInvocationHandler(bean,adviceInfos,singletonObjects,beanDefinitionMap));

            System.out.println("返回了一个AOP代理对象....");
        }
        return proxyObj;

    }



    // 从代理对象中获取被代理的对象
    public Object getOriginalObjectFromProxy(Object proxy) {
        if(Proxy.isProxyClass(proxy.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(proxy);
            try {
                Field field = handler.getClass().getDeclaredField("originalObject");
                field.setAccessible(true);
                Object originalObject = field.get(handler);
                System.out.println("The original object: " + originalObject);
                return originalObject;
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    // 属性注入
    public void populateBean(Class clazz, Object instance) throws IllegalAccessException {
        // 对原来的对象进行属性/依赖注入

        for (Field declaredField : clazz.getDeclaredFields()) {
            if (declaredField.isAnnotationPresent(Autowired.class)) {
                Object bean = getBean(declaredField.getName());

                // 创建bean
                if (bean == null) {
                    ZhouyuApplicationContext zac = new ZhouyuApplicationContext();
                    bean = zac.createBean(declaredField.getName(), beanDefinitionMap.get(declaredField.getName()));
                }
                declaredField.setAccessible(true);
                declaredField.set(instance, bean);
            }
        }
    }

    // 因为beanPostProcessor，返回的有可能是代理对象
    // 初始化：包括beanNameAware， InitializingBean接口回调，以及beanPostProcessor接口的前后置处理
    public void initializeBean(Object instance, String beanName) throws Exception {
        // Aware回调
        // 完成beanNameAware的赋值
        if (instance instanceof BeanNameAware) {
            ((BeanNameAware)instance).setBeanName(beanName);
        }
        // BeanPostProcessor扩展机制，可以针对初始化前，初始化后，属性赋值后做一些其他操作
        for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
            instance = beanPostProcessor.postProcessBeforeInitialization(instance,beanName);
        }

        // 初始化
        if (instance instanceof InitializingBean) {
            ((InitializingBean)instance).afterPropertiesSet();
        }

        for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
            instance = beanPostProcessor.postProcessAfterInitialization(instance,beanName);
        }
    }

    /*
        扫描所有注册类
     */
    private void scan(Class configClass) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        // 解析配置类：获取componentScan注解的路径->扫描路径下的java类->注册为容器中的bean
        ComponentScan scAnnotation = (ComponentScan) configClass.getDeclaredAnnotation(ComponentScan.class);
        String path = scAnnotation.value();
        System.out.println("path:"+path);
        path = path.replace(".","/");
        // 三种类加载器：Bootstrap ->jre/lib
        // Ext -> jre/ext/lib
        // App -> classpath(指整个应用的路径)
        // classpath: D:\softwares_installation_path\jdk1.8\bin\java.exe "-javaagent:D:\softwares_installation_path\idea\IntelliJ IDEA 2021.3.1\lib\idea_rt.jar=58219:D:\softwares_installation_path\idea\IntelliJ IDEA 2021.3.1\bin" -Dfile.encoding=UTF-8 -classpath D:\softwares_installation_path\jdk1.8\jre\lib\charsets.jar;D:\softwares_installation_path\jdk1.8\jre\lib\deploy.jar;D:\softwares_installation_path\jdk1.8\jre\lib\ext\access-bridge-64.jar;D:\softwares_installation_path\jdk1.8\jre\lib\ext\cldrdata.jar;D:\softwares_installation_path\jdk1.8\jre\lib\ext\dnsns.jar;D:\softwares_installation_path\jdk1.8\jre\lib\ext\jaccess.jar;D:\softwares_installation_path\jdk1.8\jre\lib\ext\jfxrt.jar;D:\softwares_installation_path\jdk1.8\jre\lib\ext\localedata.jar;D:\softwares_installation_path\jdk1.8\jre\lib\ext\nashorn.jar;D:\softwares_installation_path\jdk1.8\jre\lib\ext\sunec.jar;D:\softwares_installation_path\jdk1.8\jre\lib\ext\sunjce_provider.jar;D:\softwares_installation_path\jdk1.8\jre\lib\ext\sunmscapi.jar;D:\softwares_installation_path\jdk1.8\jre\lib\ext\sunpkcs11.jar;D:\softwares_installation_path\jdk1.8\jre\lib\ext\zipfs.jar;D:\softwares_installation_path\jdk1.8\jre\lib\javaws.jar;D:\softwares_installation_path\jdk1.8\jre\lib\jce.jar;D:\softwares_installation_path\jdk1.8\jre\lib\jfr.jar;D:\softwares_installation_path\jdk1.8\jre\lib\jfxswt.jar;D:\softwares_installation_path\jdk1.8\jre\lib\jsse.jar;D:\softwares_installation_path\jdk1.8\jre\lib\management-agent.jar;D:\softwares_installation_path\jdk1.8\jre\lib\plugin.jar;D:\softwares_installation_path\jdk1.8\jre\lib\resources.jar;D:\softwares_installation_path\jdk1.8\jre\lib\rt.jar;C:\Users\yxg\Desktop\后端开发进阶\手写spring\spring-zhouyu\target\classes com.zhouyu.Test
        ClassLoader classLoader = ZhouyuApplicationContext.class.getClassLoader();
//        URL resource = classLoader.getResource(path);

        URL resource = classLoader.getResource(path);
        System.out.println(resource.getPath());
        File file = new File(resource.getFile());
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                System.out.println(f.getAbsolutePath());
                // 将路径转换为全限定名
                String fileName = f.getAbsolutePath();
                if (fileName.endsWith(".class")) {
                    String className = fileName.substring(fileName.indexOf("com"), fileName.indexOf(".class"));
                    className = className.replace("\\",".");
                    System.out.println("全限定名："+className);
                    Class<?> clazz = classLoader.loadClass(className);
                    if (clazz.isAnnotationPresent(Component.class)) {
                        // 判断当前的bean是一个单例bean还是prototype的bean
                        // 将类解析到BeanDefinition里面

                        Component componentAnnotation = clazz.getDeclaredAnnotation(Component.class);
                        String beanName = componentAnnotation.value();

                        // 当解析到BeanPostProcessor的实现类时需要存起来
                        if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                            // 这一行实际上应该走spring创建bean的逻辑，因为ZhouyuBeanPostProcessor内部可能有第三方依赖
                            // 这样的话，spring内部的逻辑也能进行依赖注入
                            BeanPostProcessor instance = (BeanPostProcessor) clazz.getDeclaredConstructor().newInstance();
                            System.out.println("解析到BeanPostProcessor的实现类，beanName是: "+ beanName);

//                            BeanPostProcessor instance = (BeanPostProcessor) getBean(beanName);
                            beanPostProcessorList.add(instance);
                        }

                        // 处理切面类
                        handleAspectClass(className, clazz, beanName);

                        // 处理元数据
                        BeanDefinition beanDefinition = new BeanDefinition();
                        beanDefinition.setClazz(clazz);
                        if (clazz.isAnnotationPresent(Scope.class)) {
                            Scope scopeAnnotation = clazz.getDeclaredAnnotation(Scope.class);
                            beanDefinition.setScope(scopeAnnotation.value());
                        } else {
                            beanDefinition.setScope("singleton");
                        }
                        System.out.println("成功将bean：" + beanName + "加入到definitionMap中, 此时beanDefinition为："+beanDefinition.getClazz());
                        beanDefinitionMap.put(beanName, beanDefinition);

                    }
                }

            }
        }
    }

    private void handleAspectClass(String className, Class<?> clazz, String beanName) {
        // 处理切面类
        if (clazz.isAnnotationPresent(Aspect.class)) {
            // 遍历切面类中的所有方法
            for (Method method: clazz.getDeclaredMethods()) {
                // 查看是否有切面前置通知
                if (method.isAnnotationPresent(Before.class)) {
                    // 切点 pointcut
                    String expression = method.getAnnotation(Before.class).value();
                    saveAdvices(className, beanName, method, expression, "Before");
                }
                // 查看是否有切面后置通知
                if (method.isAnnotationPresent(After.class)) {
                    // 切点 pointcut
                    String expression = method.getAnnotation(After.class).value();
                    saveAdvices(className, beanName, method, expression, "After");
                }
                  // 查看是否有切面相关的环绕通知
                if (method.isAnnotationPresent(Around.class)) {
                    // 切点 pointcut
                    String expression = method.getAnnotation(Around.class).value();
                    saveAdvices(className, beanName, method, expression, "Around");
                }
            }
        }
    }

    // 使用map存储所有“通知”
    private void saveAdvices(String className, String beanName, Method method, String expression, String adviceType) {

        // 分裂表达式获取该通知适用的所有方法
        String[] joinPoints = expression.split(";");

        for (String joinPointClass : joinPoints) {
            System.out.println("joinPointClassPath:"+joinPointClass);
            String[] affectedLevels = joinPointClass.split("\\.");
            System.out.println("length: "+affectedLevels.length);
            String affectedBeanName = affectedLevels[affectedLevels.length-2];
            affectedBeanName = Character.toLowerCase(affectedBeanName.charAt(0)) + affectedBeanName.substring(1); // 首字母小写对应bean的名称
            String affectedMethodName = affectedLevels[affectedLevels.length-1];//获取连接点的方法
            AdviceInfo ai = new AdviceInfo(className,adviceType, beanName, method.getName(), affectedMethodName);
            if (!adviceInfoMap.containsKey(affectedBeanName) || adviceInfoMap.get(affectedBeanName) == null) {
                adviceInfoMap.put(affectedBeanName, new ArrayList<>());
            }
            adviceInfoMap.get(affectedBeanName).add(ai);
        }
    }


    public Object getBean(String beanName) {

        if (beanDefinitionMap.containsKey(beanName)) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            System.out.println("获取到beanDefinition： " + beanDefinition.getClazz() + ", "+beanDefinition.getScope());
            if (beanDefinition.getScope().equals("singleton")) {

                Object o = singletonObjects.get(beanName);
                return o;
            } else {
                // 如果是prototype，就需要创建一个bean
                Object bean = createBean(beanName,beanDefinition);
                return bean;
            }
        } else {
            // 不存在对应的bean
            throw new NullPointerException();
        }
    }
}
