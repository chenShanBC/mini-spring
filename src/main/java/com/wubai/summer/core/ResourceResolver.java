package com.wubai.summer.core;


//资源扫描器核心功能： 根据包路径，（通过类加载器读取 ClassPath 资源）扫描指定包及其子包下的所有.class 文件，转换为全限定类名（如com.summer.test.UserService）

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * 资源扫描器：扫描指定包下的所有Class全限定名  （传入一个包路径，这个类会帮他遍历其内部的【所有】类的全限定名，以方便反射（反射是需要全限定名的，而不是靠一个包路径））
 */
public class ResourceResolver {
    //要扫描的基础包（如com.wubai.summer.test）
    private final String basePackage;

    // 包路径 转 文件路径 的分隔符（com.summer.test → com/summer/test）
    private final String basePackagePath;

    //带参构造 传入
    public ResourceResolver(String basePackage) {
        this.basePackage = basePackage;
        this.basePackagePath = basePackage.replace(".", File.separator); //转换的核心原因：类加载器读取 ClassPath 资源时，只认 “文件系统路径”，不认 “Java 包路径”。
    }

    /**
     * 扫描包下所有Class全限定名
     */
    //核心方法 scanClassNames()：入口逻辑
    public List<String> scanClassNames() {
        List<String> classNames = new ArrayList<>();
        try {
            // 通过（当前线程的）类加载器获取包的资源URL
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            URL resource = classLoader.getResource(basePackagePath); //根据转换后的文件路径，获取包对应的目录 URL（如file:/xxx/target/classes/com/summer/test）
            if (resource == null) {
                throw new RuntimeException("扫描的包不存在：" + basePackage);
            }
            // 遍历资源目录下的所有文件/子包
            File packageDir = new File(resource.toURI()); //把 URL 转换为文件系统的File对象，方便后续遍历目录；
            scanDir(packageDir, classNames);
        } catch (Exception e) {
            throw new RuntimeException("包扫描失败",e);
        }
        return classNames;
    }


    /**
     * 递归扫描目录，过滤.class文件并转换为全限定类名
     */
    //递归方法 scanDir()：核心扫描逻辑
    private void scanDir(File dir, List<String> classNames) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) { //是目录则递归它
                // 递归扫描子包
                scanDir(file, classNames);
            } else {
                String fileName = file.getName();
                // 过滤.class文件，排除临时文件（如$Proxy0.class）
                if (fileName.endsWith(".class") && !fileName.contains("$")) {  //以.class结尾且不包含$的文件，执行一下转换全限定名的逻辑
                    // 转换为全限定类名：com/summer/test/UserService.class → com.summer.test.UserService
                    String className = file.getAbsolutePath()
                            // 从绝对路径中截取从基础包路径开始的部分（如xxx/com/summer/test/UserService.class → com/summer/test/UserService.class）
                            .substring(file.getAbsolutePath().indexOf(basePackagePath))
                            //把文件分隔符转回包分隔符（com/summer/test/UserService → com.summer.test.UserService）
                            .replace(File.separator, ".")
                            // // 去掉.class后缀（com.summer.test.UserService.class → com.summer.test.UserService）
                            .replace(".class", "");
                    classNames.add(className);
                    //设计原因：
                    // IOC 容器后续需要通过全限定类名（如com.summer.test.UserService）反射创建实例，所以必须把文件路径转换为 Java 规范的类名。
                }
            }
        }
    }


}
