package moe.chikalar.bili;

import moe.chikalar.bili.configuration.BiliRecorderProperties;
import moe.chikalar.bili.utils.HttpsTrustManager;
import org.apache.catalina.servlets.DefaultServlet;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.LinkedList;

@SpringBootApplication
@EnableScheduling
@ServletComponentScan
public class BiliRecorderApplication {

    public static void main(String[] args) {
        HttpsTrustManager.allowAllSSL();
        SpringApplication.run(BiliRecorderApplication.class, args);
    }


    @Bean
    public LinkedList<Long> recordQueue() {
        return new LinkedList<>();
    }
    @Bean
    public ServletRegistrationBean servletRegistrationBean(BiliRecorderProperties properties) {
        final DefaultFileServlet servlet = new DefaultFileServlet();
        final ServletRegistrationBean bean =
                new ServletRegistrationBean(servlet, "/data/*");
        bean.addInitParameter("listings", "true");
        bean.addInitParameter("sortListings", "true");
        bean.addInitParameter("folder", properties.getWorkPath());
        bean.setLoadOnStartup(1);
        return bean;
    }

}
