# spring的实现及其疑惑解答
![img_1.png](img_1.png)
# 1 spring的两种容器

## 1.1 使用方式（spring如何获取需要注册bean的类）!!!

```java
   public static void main(String[] args) {
        #A
        ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext("spring.xml");
        User user = (User)classPathXmlApplicationContext.getBean("user");
        
        #B
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(AppConfig.class);
        UserService userService = applicationContext.getBean("userService", UserService.class);
    }
```


### 1.1.1 ClassPathXmlApplicationContext
可以参考代码中的#A，可知道这里的容器创建借助的是spring.xml这个配置文件

### 1.1.2 AnnotationConfigApplicationContext
这种方式是基于注解的实现，AppConfig.class，而AppConfig
中得内容如下图所示：可以看到我们定义了spring得扫描的路径，spring
会自动扫描"com.zhouyu"包下的所有java文件并且生成相应的bean，也可以
通过@Bean注解手动定义某一个bean。
![img_2.png](img_2.png)


## 1.2 spring获取bean的相关细节
大概流程：
解析配置类：new一个ApplicationContext并且传递配置类->解析配置类获取componentScan注解中规定的路径->扫描路径下的java类->注册为容器中的bean

详细说明：
当new一个ApplicationContext对象的时候就相当于创建了一个容器，会
调用这个容器的构造方法，我们需要将指定的配置类传递给它，因为配置类中通过
@componentScan注解规定了扫描的包的全限定名，然后将这个限定名转换成
路径，随后扫描这个路径下的所有java类，并将其注册到IOC容器中称为bean。

```java
package com.zhouyu;

import com.spring.ComponentScan;

@ComponentScan("com.zhouyu.service")
public class AppConfig {
    
}

```
## 1.3 加入IOC容器中，为什么需要两个map？
在扫描一个类时，会首先查看类上是否有@Component字段，如果有则说明
需要加入IOC的容器中，否则不需要，如果需要加入容器，则需要创建两个map。

### 1.3.1 接下来弄清楚这两个map是什么，

首先是定义Map，我们叫BeanDefinitionMap，这个map中的value是BeanDefinition类，
存储的是一个类的元数据，包括但不限于这个类的Class对象，单例还是多例等，
key是其类中@Component注解的指定值，这个map是spring创建bean时的重要参考，
因为它能以O(1)的复杂度取获取创建bean的元数据。

第二个才是真正的对象map，也称作单例池，只存储@Component为"singleton"的
实例对象,如果@Component的值为prototype则不会存储到这个池子中，
其key是@Component注解的指定值，其value为spring启动时就创建的对象。


### 1.3.2 为什么需要这两个map？

这两个`map`在Spring框架中扮演重要角色，它们分别用于存储bean的元数据(`BeanDefinition`)和单例bean的实例。我们分开解释它们的作用：

1. **BeanDefinitionMap**:

   这个`map`存储了bean的元数据，包括bean的类型(`Class`对象)以及其他配置信息（如bean是单例还是多例）。当Spring需要创建一个bean的实例时，它首先查找这个`map`来获取bean的元数据(`BeanDefinition`)。这个元数据包含了Spring需要知道的所有信息来正确地创建bean。

   为什么这个重要？因为这使得Spring可以以一种非常高效的方式管理和创建bean。存储这些元数据允许Spring重用这些信息，而不必每次都重新解析类或注解。

2. **单例池(Singleton Pool)**:

   这个`map`专门用于存储单例bean的实例。当一个bean被定义为单例时，这意味着在整个应用程序的生命周期中，这个bean只会有一个实例。Spring在启动时或在首次请求时创建这个实例，并将其存储在这个`map`中。当你再次请求这个bean时，Spring只需从这个`map`中检索实例，而不是重新创建它。

   这对于资源密集型的对象非常有用，比如数据库连接池，因为你不希望每次请求时都创建一个新的实例。

综上所述，这两个`map`分别用于：

- 让Spring能够快速查找到如何创建bean的信息（通过`BeanDefinitionMap`）。
- 为单例bean提供一个高效的存储机制，这样Spring只需要创建一个实例，然后在后续的请求中重复使用这个实例。

这种设计支持了Spring的核心特性，包括依赖注入、bean的生命周期管理以及性能优化。

这也是当component中规定一个类为多例时不不用放入map的原因，
主要是因为单例池就是为了避免创建重复的实例。

## 1.4 创建单例bean和多例bean的过程
首先会传入一个字符串调用ApplicationContext的getBean方法，
该方法首先会通过bean的定义map查询对应类的元数据，
元数据包含了该类是支持单例还是多例，如果是单例会
访问单例池取得早就创建的单例对象并返回，如果是多例
会创建一个新的实例并返回。


## 1.5 为什么可以通过字符串就可以从容器中取到对应的bean？
> 这里的容器实际上是一个map对象，这里的字符串在类上的@Component
> 的注解中声明和bean对象唯一绑定在一起(UserService类)，在扫描
> 的时候会解析@Component注解，然后将其放入到beanDefinitionMap中

```java
public class Test {

    public static void main(String[] args) {
        ZhouyuApplicationContext applicationContext = new ZhouyuApplicationContext(AppConfig.class);

        Object userService = applicationContext.getBean("userService");

    }
}
```
```java
@Component("userService")
@Scope("prototype")
public class UserService {

}
```

## 1.6 如何创建单例和多例bean？
扫描阶段会判断这个类中是否有@Scope注解，如果有就会查看其值是否是
"prototype", 如果是则


# 2 spring的依赖注入的实现方式

## 2.1 什么是依赖注入？
### 2.1.1 spring中什么是依赖？
一个类中的某一个属性是一个对象，那么可以称该类依赖这个对象。

![img_3.png](img_3.png)

如下所示Car类以来了Engine类的实例。
```java
@Component
public class Car {
    private Engine engine;
    
}

public class Engine {
    // ... Engine 类的实现
}
```
### 2.1.2 什么是注入？
在Spring中，注入（Injection）是指将一个对象自动地赋值给另一个对象的属性、setter方法或构造方法。这是通过Spring的IoC（控制反转）容器完成的，它负责创建对象，绑定它们的依赖关系，并提供完成初始化的对象。

注入主要有三种形式：

1. **构造器注入**：这是通过使用类的构造方法来注入依赖。Spring IoC容器会自动创建依赖的实例，并通过构造函数传递给需要它们的对象。

    ```java
    @Component
    public class Car {
        private final Engine engine;
    
        @Autowired
        public Car(Engine engine) {
            this.engine = engine;
        }
    }
    ```

2. **Setter注入**：这是通过使用setter方法来注入依赖。Spring IoC容器创建依赖的实例，并使用setter方法将其传递给需要它的对象。

    ```java
    @Component
    public class Car {
        private Engine engine;
    
        @Autowired
        public void setEngine(Engine engine) {
            this.engine = engine;
        }
    }
    ```

3. **字段注入**：这是通过直接将依赖注入到类的字段上。这种方式通常不推荐，因为它可以导致代码难以测试和维护。

    ```java
    @Component
    public class Car {
        @Autowired
        private Engine engine;
    }
    ```

在Spring中，注入的主要目的是实现解耦。通过自动管理对象的创建和它们的依赖关系，可以使代码更加模块化，更易于测试和维护。此外，通过使用配置，你可以轻松地更改注入的实际实现，而无需修改代码。

### 2.1.3 那依赖注入呢？
![img_4.png](img_4.png)

### 2.1.4 在Spring框架中，依赖注入（Dependency Injection, DI）是一种实现控制反转（Inversion of Control, IoC）的技术，为什么？

**控制反转（IoC）**是一种设计原则，它的核心思想是将对象的创建和管理从它们使用的代码中移除，把这些责任交给一个外部的实体
（通常是一个框架或容器），我们都知道创建一个对象的时候
需要初始化，也就是需要给属性赋值，如果一个对象中的某一个属性也是
一个对象，**那么我们也需要创建它，这个创建第三方对象的过程
交给IOC容器完成就可以称为控制反转，而“依赖注入”就是实现
这个第三方对象自动赋值的一种方式。**

### 2.1.5 依赖注入的实现过程

以属性上依赖注入为例：

在创建一个bean之前会首先从beanDefinitionMap中获取
这个bean的类型，然后会调用newInstance方法生成一个
没有给属性赋值的实例，接着我们会遍历这个实例的所有属性，
并且查找其中带有@Autowired注解的属性，这个属性是一个对象，
此时我们会根据这个属性名调用getBean方法从单例池中获取或者创建一个bean（bean不存在就创建，否则返回），
然后赋值给这个属性，就这样完成了依赖注入。

# 3 BeanNameAware接口实现捕获自身Bean的名称

这是什么意思呢？
以UserService类为例，大概就是说我现在想要知道自身在IOC容器中的名称，并且赋值给自己的beanName属性

我们可以定义一个BeanNameAware接口，然后在UserService中实现这个接口，
当我们在createBean方法中，完成依赖注入后可以判断这个bean是否是BeanNameAware
的实例，如果是则可以调用这个接口的setBeanName方法完成赋值操作，因为创建bean时，
beanName会从@Componnet这样的注解中声明，然后传递给createBean，所以这是合理的。

```java
if (instance instanceof BeanNameAware) {
    ((BeanNameAware)instance).setBeanName(beanName);
}
```
# 4 扩展接口InitializingBean
一般spring创建bean的过程中，在完成依赖注入后，会调用这个接口的afterPropertiesSet
方法，一般这个方法用于属性验证，比如属性是否是空，为空则抛异常 。


# 5 扩展接口BeanPostProcessor实现初始化前、后、属性赋值后的扩展操作。

## 5.1 为什么要这个接口?
因为方便扩展，针对某一些特殊的bean执行一些特殊的处理操作，AOP就是基于
这个扩展机制实现的 

## 5.2 spring框架的实现中，有一个beanPostProcessors接口，为什么有好几个实现类（体现在有一个专门的列表来存储它们）
![img_5.png](img_5.png)

## 5.3 那beanPostProcessor接口的所有实现是不是对所有的bean都会生效

是的，BeanPostProcessor 对 Spring 容器中所有的 bean 都会生效。当 Spring 容器在初始化 bean 的时候，它会遍历所有注册的 BeanPostProcessor，并对每一个 bean 调用 postProcessBeforeInitialization 和 postProcessAfterInitialization 方法。

这就是为什么 BeanPostProcessor 是如此强大的一个扩展点，因为你可以用它来拦截和修改 Spring 容器中所有 bean 的初始化过程。

然而，有时你可能只想针对特定的 bean 执行一些逻辑。为了做到这一点，你可以在你的 BeanPostProcessor 实现中加入一些条件判断。例如，你可以检查 bean 的类型或名称，并只对匹配的 bean 执行特定的逻辑：

```java
@Component
public class CustomBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        // 只对特定类型的 bean 执行逻辑
        if (bean instanceof MySpecificBeanType) {
            // 在这里执行你的自定义逻辑
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 只对特定名称的 bean 执行逻辑
        if ("mySpecificBeanName".equals(beanName)) {
            // 在这里执行你的自定义逻辑
        }
        return bean;
    }
}
```


## 5.4 所有我可以理解为，如果需要对一个bean实现切面，我在postProcessAfterInitialization中判断一下该bean是否有切点，如果有就生成一个代理对象
是的，你的理解是正确的。使用 BeanPostProcessor 的 postProcessAfterInitialization 方法来创建代理对象是 AOP（Aspect-Oriented Programming，面向切面编程）在 Spring 中的常见实现方式。

在 postProcessAfterInitialization 方法中，你可以检查 bean 是否具有与切点相关的注解或其他标识，然后根据需要创建一个代理对象。这个代理对象可以在目标方法调用前后插入额外的逻辑，如记录、事务管理等。

这里是一个简单的例子展示如何使用 BeanPostProcessor 来创建一个代理对象：
```java
@Component
public class AOPBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof MyTargetBean) { // 检查是否是目标 bean
            return Proxy.newProxyInstance(
                    bean.getClass().getClassLoader(),
                    bean.getClass().getInterfaces(),
                    (proxy, method, args) -> {
                        // 在目标方法调用前执行逻辑
                        System.out.println("Before method execution");

                        // 调用目标方法
                        Object result = method.invoke(bean, args);

                        // 在目标方法调用后执行逻辑
                        System.out.println("After method execution");

                        return result;
                    });
        }
        return bean;
    }
}

```
然而，值得注意的是，Spring 本身提供了一个非常强大的 AOP 框架，通常不需要你手动创建 BeanPostProcessor 来处理 AOP。你可以使用 Spring 的 @Aspect 和 @Around 等注解来更简单地实现切面编程。这种方式更为简洁和直观，而且允许你利用 Spring 对 AOP 的深度集成。

下面是用cglib代理实现的实现AOP的BeanPostProcessor子类：
```java
@Component
public class AOPBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof MyTargetBean) { // 检查是否是目标 bean
            return Proxy.newProxyInstance(
                    bean.getClass().getClassLoader(),
                    bean.getClass().getInterfaces(),
                    (proxy, method, args) -> {
                        // 在目标方法调用前执行逻辑
                        System.out.println("Before method execution");

                        // 调用目标方法
                        Object result = method.invoke(bean, args);

                        // 在目标方法调用后执行逻辑
                        System.out.println("After method execution");

                        return result;
                    });
        }
        return bean;
    }
}
```

## 5.5 如何使用这些BeanPostProcessor，毕竟有多个
当你有多个 BeanPostProcessor 时，Spring 会按照它们的优先级和声明的顺序来调用它们。
你可以通过实现 org.springframework.core.Ordered 接口或使用 @Order 注解来控制 BeanPostProcessor 的执行顺序。

下面是一个简单的demo: 
```java
@Component
public class CustomBeanPostProcessor implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        // 在这里执行你的自定义逻辑，比如修改 bean 属性
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 在这里执行你的自定义逻辑，比如包装 bean 实例
        return bean;
    }
}

@Component
@Order(1) // 设置优先级，数值越小优先级越高
public class FirstBeanPostProcessor implements BeanPostProcessor, Ordered {

   @Override
   public int getOrder() {
      return 1; // 这与@Order(1)效果相同
   }

   // postProcessBeforeInitialization 和 postProcessAfterInitialization 的实现
}
```

## 5.6 如何利用BeanPostProcessor实现预处理和后处理功能？
首先定义一个BeanPostProcessor接口，这个接口中有以下几种方法：
（1）postProcessBeforeInitialization
（2）postProcessAfterInitialization

然后再定义一个实现类：

我们需要为这个实现类注册声明一个@Component注解，spring启动时会将其加入到单例
池子中。

```java
package com.zhouyu.service;

import com.spring.BeanPostProcessor;
import com.spring.Component;

@Component("zhouyuBeanPostProcessor")
public class ZhouyuBeanPostProcessor implements BeanPostProcessor {
   @Override
   public Object postProcessBeforeInitialization(Object bean, String beanName) {
      System.out.println("初始化前");
      // 比如说我想针对某一个单独的bean做处理
      if (beanName.equals("userService")) {
//            ((UserService)bean).setName(beanName);
         ((UserServiceImpl) bean).setName("good");
      }
      return bean;
   }

   @Override
   public Object postProcessAfterInitialization(Object bean, String beanName) {
      System.out.println("初始化后");
      return bean;
   }
}
```

然后在扫描时会加一个识别该实现类的判断，如下所示，发现了一个实现了BeanPostProcessor的
实现类就加入到list中存起来。
```java
// 当解析到BeanPostProcessor的实现类时候需要存起来
if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
    // 这一行实际上应该走spring创建bean的逻辑，因为ZhouyuBeanPostProcessor内部可能有第三方依赖
    // 这样的话，spring内部的逻辑也能进行依赖注入
    BeanPostProcessor instance = (BeanPostProcessor) clazz.getDeclaredConstructor().newInstance();
    System.out.println("beanName是: "+ beanName);
    beanPostProcessorList.add(instance);
}
```

当我们创建一个bean时，会在一个bean对象初始化之前和之后分别遍历一遍这个beanPostProcessorList，
前者会调用它的postProcessBeforeInitialization方法，
后者会调用postProcessAfterInitialization方法。
就这样完成了预处理和后处理。

## 5.7 如果有多个实现类实现了BeanPostProcessor接口，如何对他们排序？

方法一：定义一个@SortBeanPostProcessor注解，值是实现类的优先级，
当创建beanDefinition的时候会扫描到该注解并且取出优先级，然后在加入list的时候
会根据优先级进行排序。

# 6 spring AOP

实现是基于BeanPostProcessor实现，首先这个processor的postProcessAfterInitialization
是整个spring创建过程中的最后一环，所以我们可以利用这个方法实现AOP。

如果我们要启用spring的AOP功能，就需要在AppConfig配置类上启用@EnableAspectAutoProxy注解，
这个注解里使用@Import(AspectJAutoProxyRegistrar.class)，作用是引入了这个bean，
但是这个bean最终目的是引入BeanPostProcessor接口，并且生成bean放入IOC容器中去。

在BeanPostProcessor的方法中，会找一个配置类中的切点相关的注解，然后生成一个代理对象并且
返回，这个代理对象包含了需要执行切点方法，包括前后置方法，以及被代理的方法

## 6.1 如何实现spring AOP
IOC容器里有很多bean，但也是分种类的，有切面bean，也有业务bean。

首先会扫描装有@Component所有类，创建并且收集所有带有@Aspect注解的切面Bean，然后找到带有
@Before，@After等字样的注解，这些注解中指明了应用通知/切点的类，然后中将其通过map缓存起来，
map的key是bean的名字，value是一个列表，存储了所有的通知及其绑定的信息，比如通知的路径，表达式等，
，等到后面创建bean时，会根据map判断自己是否被切面定义了，如果是，会通过map查询到相应的切点路径，
然后将原方法和加入了前后置通知的方法绑定在一起，生成一个代理对象，并将这个代理对象返回给IOC容器。

## 6.2 spring AOP会最终生成一个代理对象，这个代理对象是如何生成的呢？

是基于cglib实现的AOP代理对象，cglib是基于继承机制的，比如cglib要为存在切点的UserServiceImpl对象
生成一个代理对象，首先创建一个叫UserServiceImplProxy的子类继承UserServiceImpl类，
```java
@Component("userService")
//@Scope("prototype")
public class UserServiceImpl implements UserService {

    @Autowired
    private OrderService orderService;
    
    public void test() {
        System.out.println("打印userServiceImpl");
    }
}

```

```java
import com.zhouyu.service.UserService;

@Component("userService")
//@Scope("prototype")
public class UserServiceImplProxy extends UserServiceImpl {

   private UserServiceImpl target; // 被代理对象
   
   @Override
   public void test() {
       super.test();// 调用父类的test方法。也可以使用target.test()方法
   }
}

```
问：代理对象UserServiceImplProxy里的orderService属性里是否有值呢？
> 答：没有值，首先UserServiceImplProxy继承了UserServiceImpl，所以UserServiceImplProxy
> 里面也有一个orderService属性，但是代理对象里还存了一个被代理对象的引用，在上面这个例子中就是target字段，
> 也就是说代理对象的orderService属性没有注入

问：但是父类的该orderService属性实现了注入吗？

> 因为代理对象和被代理对象是两个对象，获得代理对象后没有对其属性填充

问：为什么不对代理对象进行属性填充？
> 因为没有必要，代理对象的关注点是切面，属性由被代理对象使用，所以不填充

## 6.3 Spring bean创建的生命周期（也是Spring bean的生命周期）

@ComponentScan获取扫描包->通过@Component获取到待托管类-> 获得类实例化（通过反射调用无参构造方法）
的对象-> 属性填充（依赖注入）-> 初始化(spring提供的InitializingBean接口，有一个afterPropertiesSet方法，属性填充完后会调)
-> 经过BeanPostProcessor可能会生成AOP代理对象（若无AOP则实例化后的对象就是Bean对象） -> Bean对象

## 6.4 如果一个spring @component类有两个带参数的构造方法，构建bean会发生什么？
> 会报错：没有默认的构造方法，spring不知道哪一个构造方法更好，但是如果这两个
> 构造方法中有一个是无参构造，那么久会调用这个无参方法，
> 此外如果只有一个带参数的构造方法，spring也不会报错。

## 6.5 如果一个@Component类只有一个带参数的构造方法，spring也不会报错，而且这个参数的类型是另一个对象，此时会怎么去spring中找到对应的bean并且完成注入？
> 此时会通过spring容器中通过缓存先按照参数类型，再按照参数名字去查找bean。

问：为什么是先byType，再byName呢？
> 答：如果先按照byName去容器中找，有可能别的对象类型也有这个相同的名字，这样就白找了，
> 而如果有多个这种类型的
> bean，可以先找出来，然后再按照byName就可以最终找到想要的bean

问：在单例模式下，会存在多个属于某一个类型的bean吗？
> 是的，存在，
> 比如一个接口Animal，有Cat，Dog两个实现类，那么Cat，Dog都是Animal类型的bean，但是它们有
> 不同的bean名字,参考case1。
> 
> 另一种就是case2提到的，一个配置类中使用@Bean注解引入了多个相同类型的外部或者第三方的bean

case1:
```java
public interface Animal {

}
@Component("cat")
public class Cat implements Animal {

}
@Component("dog")
public class Dog implements Animal {

}
```
case2:
```java
@Component("config")
public class Config {
   @Bean
   public Cat catConfig1() {
       
   }
   @Bean
   public Cat catConfig2() {

   }
}
```

# 7 动态代理实现AOP

强烈推荐看看下面这篇文章，讲解了为什么实现AOP使用cglib动态代理而不是jdk代理

[浅析Spring中AOP的实现原理——动态代理](https://www.cnblogs.com/tuyang1129/p/12878549.html)

# 8 spring事务
## 8.1 spring事务的实现
spring事务也是通过AOP机制实现的，而AOP会导致生成一个代理对象，
所以如果我们想要让事务成功调用，必须使用代理对象调用这个被@Transaction修饰
的方法，而不能使用被代理对象调用事务方法，否则事务会不起作用。

## 8.2 spring事务的失效
### 8.2.1 因为没有正确调用导致的事务失效
下面case1中，当程序调用B完成后会抛异常，因为B是一个事务方法，按照正常的逻辑
抛异常后会回滚，即数据插入失败，但是事实上插入成功了，这就是因为事务失效了。

原因：因为没有使用代理对象调用该事务方法，从而导致事务的失效。

case1：

```java
import com.spring.Autowired;

@Service
public class MyService {

   @Autowired
   private JdbcTemplate jdbcTemplate; 

   public void methodA() {
      // some transactional logic...
      methodB();
   }
   @Transactional
   private void methodB() {
      // some transactional logic...
      jdbcTemplate.execute("insert into t1 values(1,1,1)");
      throw new NullPointerException();
   }
}

```
修正后的代码：
如下代码所示，我们在类中注入了一个关于自身的代理对象，然后调用事务方法时我们使用代理对象的
methodB方法，这样就能成功实现事务的传播。

```java
import com.spring.Autowired;

@Service
public class MyService {

   @Autowired
   private JdbcTemplate jdbcTemplate;

   @Autowired
   private  MyService myService;
   
   public void methodA() {
      // some transactional logic...
      myService.methodB();
   }
   @Transactional
   private void methodB() {
      // some transactional logic...
      jdbcTemplate.execute("insert into t1 values(1,1,1)");
      throw new NullPointerException();
   }
}
```

可能有人会问，为什么MyService myService;能接收代理对象呢？
> 如果使用了CGLIB动态代理（使用子类继承被代理类从而实现代理），前面我们提到了，代理对象会生成一个代理类，
> 继承MyService，那么自然地作为父类就能接收这个子类的对象，在类型上，子类也是父类的类型。

spring为什么能够注入代理自己的代理对象，是怎么实现的呢？
难道不会死锁嘛？毕竟代理对象的bean是在原来的实例化的对象属性赋值后的基础上生成的。，
而对myService属性赋值又需要这个代理对象。
> 答：（猜测），可能是在给myService赋值一个代理对象的时候发现找不到，于是就
> 延迟赋值，所以后面等到代理对象又完成后再回过头来给myService赋值，就这样
> 完成了”注入代理自己的代理对象“的动作

### 8.2.2 因为抛异常导致的事务失效

REQUIRES_NEW:首先介绍一下事务的一个叫做REQUIRES_NEW的传播级别，大概就是在Spring框架中，REQUIRES_NEW是一种事务传播级别，它表示当一个事务性方法被另一个事务性方法调用时，应该如何处理事务。
当一个方法被标记为REQUIRES_NEW传播级别时，无论调用它的方法是否在事务中，都会为此方法启动一个全新的事务。如果已经存在一个事务，那么这个已经存在的事务将会被挂起，直到标记为REQUIRES_NEW的方法完成执行。

以case2为例，按照REQUIRES_NEW的传播级别，事务A中的insert语句应该能正确执行，B会失败，
但是实际上两个事务都会回滚，为什么呢，这是因为methodB的异常相当于事务A也抛掉了异常。

case2:

```java
import com.spring.Autowired;

@Service
public class MyService {

   @Autowired
   private JdbcTemplate jdbcTemplate;

   @Autowired
   private  MyService myService;
   @Transactional
   public void methodA() {
      // some transactional logic...
      jdbcTemplate.execute("insert into t1 values(2,2,2)");

      myService.methodB();
   }
   @Transactional
   private void methodB() {
      // some transactional logic...
      jdbcTemplate.execute("insert into t1 values(1,1,1)");
      throw new NullPointerException();
   }
}
```

# 9 spring循环依赖
## 9.1 出现循环依赖的几种场景

1. 构造器注入：这是最复杂的一种，因为构造器注入的循环依赖无法被 Spring 容器处理，会导致创建 Bean 失败。

```java
@Component
public class A {
    private B b;

    public A(B b) {
        this.b = b;
    }
}

@Component
public class B {
    private A a;

    public B(A a) {
        this.a = a;
    }
}
```

2. Setter注入：Spring 容器能够处理这种情况下的循环依赖。

```java
@Component
public class A {
    private B b;

    @Autowired
    public void setB(B b) {
        this.b = b;
    }
}

@Component
public class B {
    private A a;

    @Autowired
    public void setA(A a) {
        this.a = a;
    }
}
```
3. 属性注入
```java
@Component
public class A {
    @Autowired
    private B b;
}

@Component
public class B {
    @Autowired
    private A a;
}
```

4. Prototype范围的循环依赖：对于非 Singleton Bean（如 Prototype Bean），Spring 容器不会尝试解决循环依赖。尝试创建这样的 Bean 会导致 BeanCurrentlyInCreationException 异常。

```java
@Configuration
public class Config {
    @Bean
    @Scope("prototype")
    public A a() {
        return new A();
    }

    @Bean
    @Scope("prototype")
    public B b() {
        return new B();
    }
}

public class A {
    @Autowired
    private B b;
}

public class B {
    @Autowired
    private A a;
}
```
请注意，这些代码示例的目的是为了解释如何在 Spring 中创建可能的循环依赖，并不推荐在实际应用中创建循环依赖，因为这可能导致代码难以理解和维护。

5 属性中注入自己也是一种循环依赖
```java
@Component
public class A {
    @Autowired
    private A a;
}
```

# 10 疑问点
## 10.1 

```java
// 方法一：
public class BeanDefinition {

    private Object bean;

    public BeanDefinition(Object bean) {
        this.bean = bean;
    }

    public Object getBean() {
        return bean;
    }
}
// 方法二：
public class BeanDefinition {

    private Class beanClass;

    public BeanDefinition(Class beanClass) {
        this.beanClass = beanClass;
    }
    public Object getBean() {
        return beanClass;
    }
}
```
在实现一个IOC容器中，我将方法一换成了二后这样就可以把 Bean 的实例化操作放到容器中处理了，为什么？

![img.png](img.png)

这一点可以从测试点看出差异：

针对方法一，其调用过程如下所示：
```java
    @Test
    public void test_BeanFactory(){
        // 1.初始化 BeanFactory
        MyBeanContainer beanFactory = new MyBeanContainer();

        // 2.注入bean
        BeanDefinition beanDefinition = new BeanDefinition(new UserService());
        beanFactory.register("userService", beanDefinition);

        // 3.获取bean
        UserService userService = (UserService) beanFactory.getBean("userService");
        System.out.print(userService);
        userService.queryUserInfo();
    }
```
针对方法二，调用过程是：


## 9.2为什么需要二级缓存earlyReferenceMap？
当对象A有一个类型为B的属性时，这个时候A先生成

