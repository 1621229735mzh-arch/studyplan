package com.kaoyan.study;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 应用启动类。
 *
 * <p>位于根包 {@code com.kaoyan.study} 下，覆盖各业务模块的组件扫描。
 * MyBatis 使用根包 + {@link Mapper} 注解的显式扫描方式：只有标注 {@code @Mapper}
 * 的接口才会注册为数据访问接口，避免把业务接口误注册为 Mapper。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@MapperScan(basePackages = "com.kaoyan.study", annotationClass = Mapper.class)
public class StudyApplication {

    public static void main(String[] args) {
        SpringApplication.run(StudyApplication.class, args);
    }
}
