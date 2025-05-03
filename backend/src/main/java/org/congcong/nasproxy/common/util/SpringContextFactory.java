//package org.congcong.nasproxy.common.util;
//
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.ApplicationContextAware;
//import org.springframework.stereotype.Component;
//
//@Component
//public class SpringContextFactory implements ApplicationContextAware {
//
//    private static ApplicationContext context;
//
//    @Override
//    public void setApplicationContext(ApplicationContext applicationContext) {
//        context = applicationContext;
//    }
//
//    public static <T> T getBean( Class<T> requiredType) {
//        return context.getBean(requiredType);
//    }
//}
