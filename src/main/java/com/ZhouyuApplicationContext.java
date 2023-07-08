package com;

import com.pojo.AdviceInfo;
import com.spring.*;
import com.zhouyu.service.OrderService;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

    public static Map<String, List<AdviceInfo>> joinPointMap = new ConcurrentHashMap<>();

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
            Object instance = clazz.getDeclaredConstructor().newInstance();

            // 属性/依赖注入
            for (Field declaredField : clazz.getDeclaredFields()) {
                if (declaredField.isAnnotationPresent(Autowired.class)) {
                    System.out.println("declaredField.getName(): " + declaredField.getName()
                            + ", 探测获取beanDefinition："+  beanDefinitionMap.get(declaredField.getName()));
                    Object bean = getBean(declaredField.getName());
                    System.out.println("bean是否为空：" + (bean == null));

                    // 创建bean
                    if (bean == null) {
                        System.out.println("没有获取到第三方bean, beanName: "+ declaredField.getName() + ", 创建bean");
                        ZhouyuApplicationContext zac = new ZhouyuApplicationContext();
                        bean = zac.createBean(beanName, beanDefinitionMap.get(declaredField.getName()));
                        // throw new RuntimeException("没有获取到第三方bean, beanName: "+ declaredField.getName()+", 此时beanDefinitionMap：" +beanDefinitionMap.toString());
                    }
                    declaredField.setAccessible(true);
                    declaredField.set(instance, bean);
                }
            }

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
            System.out.println("返回instance: "+ beanName);
            return instance;
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
                        if (clazz.isAnnotationPresent(Aspect.class)) {
                            // 遍历切面类中的所有方法
                            for (Method method:clazz.getDeclaredMethods()) {
                                // 查看是否有切面前置通知
                                if (method.isAnnotationPresent(Before.class)) {
                                    // 切点 pointcut
                                    String expression = method.getAnnotation(Before.class).value();
                                    handleAdvices(method,className,beanName,"Before", expression);
                                }
                                // 查看是否有切面后置通知
                                if (method.isAnnotationPresent(After.class)) {
                                    // 切点 pointcut
                                    String expression = method.getAnnotation(After.class).value();
                                    handleAdvices(method,className,beanName,"After", expression);

                                }
                                  // 查看是否有切面相关的环绕通知
                                if (method.isAnnotationPresent(Around.class)) {
                                    // 切点 pointcut
                                    String expression = method.getAnnotation(Around.class).value();
                                    handleAdvices(method,className,beanName,"Around", expression);
                                }
                            }
                        }
                        BeanDefinition beanDefinition = new BeanDefinition();
                        beanDefinition.setClazz(clazz);
                        if (clazz.isAnnotationPresent(Scope.class)) {
                            Scope scopeAnnotation = clazz.getDeclaredAnnotation(Scope.class);
                            beanDefinition.setScope(scopeAnnotation.value());
                        } else {
                            beanDefinition.setScope("singleton");
                        }
                        beanDefinitionMap.put(beanName, beanDefinition);

                    }
                }

            }
        }
    }

    public void handleAdvices(Method method, String className, String beanName, String type, String expression){
        // 查看是否有切面相关的环绕通知

        // 分裂表达式获取该通知适用的所有方法
        String[] joinPoints = expression.split(";");

        for (String joinPointClass : joinPoints) {
            String[] affectedLevels = joinPointClass.split("\\.");
            String affectedBeanName = affectedLevels[affectedLevels.length-2];//获取连接点的类
            String affectedMethodName = affectedLevels[affectedLevels.length-1];
            AdviceInfo ai = new AdviceInfo(className,type, beanName, method.getName(), affectedMethodName);
            if (!joinPointMap.containsKey(affectedBeanName) || joinPointMap.get(affectedBeanName) == null) {
                joinPointMap.put(affectedBeanName, new ArrayList<>());
            }
            joinPointMap.get(affectedBeanName).add(ai);
        }


    }
    public static String lowercaseFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        char firstChar = Character.toLowerCase(input.charAt(0));
        if (input.length() > 1) {
            return firstChar + input.substring(1);
        } else {
            return String.valueOf(firstChar);
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
