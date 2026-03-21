package com.wubai.summer.core;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * XML Bean定义读取器：解析XML配置文件，生成BeanDefinition
 * 核心功能：
 * 1. 解析<bean>标签，提取id、class、factory-method属性
 * 2. 解析<constructor-arg>标签，提取构造器依赖
 * 3. 解析<property>标签，提取属性依赖
 */
public class XmlBeanDefinitionReader {

    /**
     * 解析XML文件，返回 BeanName → BeanDefinition 的映射
     *
     * @param xmlPath XML文件路径（相对于ClassPath，如 "beans.xml"）
     */
    public Map<String, BeanDefinition> loadBeanDefinitions(String xmlPath) {
        Map<String, BeanDefinition> beanDefMap = new HashMap<>();

        try {
            // 1. 从ClassPath加载XML文件
                //ClassLoader.getResourceAsStream(xmlPath)：XML 文件放在 ClassPath 下（如 resources 目录），通过类加载器加载，是 Java 读取配置文件的标准方式；
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(xmlPath);
            if (inputStream == null) {
                throw new RuntimeException("XML文件不存在：" + xmlPath);
            }

            // 2. 使用DOM解析器解析XML
                //DOM 解析：把整个 XML 文件加载到内存中形成树形结构（Document），适合小体积的配置文件（IoC 配置文件通常很小）
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);


            // 3. 获取所有<bean>标签
            NodeList beanNodes = doc.getElementsByTagName("bean");
            System.out.println("【XML解析】找到 " + beanNodes.getLength() + " 个Bean定义");

            // 4. 遍历每个<bean>，转换为BeanDefinition
            for (int i = 0; i < beanNodes.getLength(); i++) {
                Element beanElement = (Element) beanNodes.item(i);
                BeanDefinition beanDef = parseBeanElement(beanElement);
                beanDefMap.put(beanDef.getBeanName(), beanDef);
                System.out.println("【Bean注册】" + beanDef.getBeanName() + " -> " + beanDef.getBeanClass().getName());
            }

        } catch (Exception e) {
            throw new RuntimeException("解析XML失败：" + xmlPath, e);
        }

        return beanDefMap;
    }

    /**
     * 解析单个<bean>标签，生成BeanDefinition
     */
    private BeanDefinition parseBeanElement(Element beanElement) {
        try {
            // 1. 读取<bean>的属性
            String id = beanElement.getAttribute("id");  //在bean里面写的就是这个bean的名字
            String className = beanElement.getAttribute("class");
            String factoryMethod = beanElement.getAttribute("factory-method");

            if (id.isEmpty() || className.isEmpty()) {
                throw new RuntimeException("XML中<bean>必须有id和class属性");
            }

            // 2. 加载Bean的Class
            Class<?> beanClass = Class.forName(className);

            // 3. 创建BeanDefinition
            BeanDefinition beanDef = new BeanDefinition();
            beanDef.setBeanName(id);
            beanDef.setBeanClass(beanClass);

            // 4. 判断是普通Bean还是工厂Bean
            if (factoryMethod.isEmpty()) {  //<bean id="userService   class="com.wubai.UserServiceFactory"  factory-method="createInstance"/>
                // 普通Bean：解析<constructor-arg>，选择构造器
                List<String> constructorArgs = parseConstructorArgs(beanElement);
                beanDef.setConstructor(selectConstructor(beanClass, constructorArgs.size()));
                beanDef.setConstructorArgRefs(constructorArgs);
            } else {
                // 工厂Bean：解析factory-method
                Method method = beanClass.getMethod(factoryMethod);
                beanDef.setFactoryMethod(method);
                beanDef.setFactoryBeanName(id); // 工厂Bean就是自己
            }

            // 5. 解析<property>标签（Setter注入）
            beanDef.setPropertyRefs(parseProperties(beanElement));

            return beanDef;

        } catch (Exception e) {
            throw new RuntimeException("解析<bean>失败", e);
        }
    }

    /**
     * 解析<constructor-arg>标签，返回依赖Bean的名称列表
     * 示例：<constructor-arg ref="orderService"/> → ["orderService"]
     */
    private List<String> parseConstructorArgs(Element beanElement) {
        List<String> argRefs = new ArrayList<>();
        NodeList argNodes = beanElement.getElementsByTagName("constructor-arg");

        for (int i = 0; i < argNodes.getLength(); i++) {
            Element argElement = (Element) argNodes.item(i);
            String ref = argElement.getAttribute("ref");
            if (!ref.isEmpty()) {
                argRefs.add(ref);
            }
        }
        return argRefs;
    }

    /**
     * 解析<property>标签，返回 属性名 → Bean引用名 的映射
     * 示例：<property name="dataSource" ref="dataSource"/> → {"dataSource": "dataSource"}
     */
    private Map<String, String> parseProperties(Element beanElement) {
        Map<String, String> propertyRefs = new HashMap<>();
        NodeList propNodes = beanElement.getElementsByTagName("property");

        for (int i = 0; i < propNodes.getLength(); i++) {
            Element propElement = (Element) propNodes.item(i);
            String name = propElement.getAttribute("name");
            String ref = propElement.getAttribute("ref");
            if (!name.isEmpty() && !ref.isEmpty()) {
                propertyRefs.put(name, ref);
            }
        }
        return propertyRefs;
    }

    /**
     * 选择构造器（根据参数数量匹配）
     * 简化版：找第一个参数数量匹配的构造器
     */
    private Constructor<?> selectConstructor(Class<?> clazz, int paramCount) {
        try {
            if (paramCount == 0) {
                // 无参构造器
                Constructor<?> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                return constructor;
            } else {
                // 有参构造器：找第一个参数数量匹配的
                for (Constructor<?> c : clazz.getDeclaredConstructors()) {
                    if (c.getParameterCount() == paramCount) {
                        c.setAccessible(true);
                        return c;
                    }
                }
                throw new RuntimeException("找不到参数数量为" + paramCount + "的构造器");
            }
        } catch (Exception e) {
            throw new RuntimeException("选择构造器失败：" + clazz.getName(), e);
        }
    }
}
/*
*
1. 使用Java自带的DOM解析器（javax.xml.parsers）
2. 遍历`<bean>`标签，提取id、class、factory-method
3. 解析`<constructor-arg ref="xxx">`存入constructorArgRefs
4. 解析`<property name="xxx" ref="yyy">`存入propertyRefs
5. 根据参数数量选择匹配的构造器
* */
