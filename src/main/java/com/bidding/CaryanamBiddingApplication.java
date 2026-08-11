package com.bidding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class CaryanamBiddingApplication {

    @jakarta.annotation.PostConstruct
    public void init() {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Kolkata"));
    }

    public static void main(String[] args) {
        SpringApplication.run(CaryanamBiddingApplication.class, args);

        System.out.println("""
              ====================================================
               CARYANAM BIDDING  BACKEND STARTED
              ====================================================

             Application : CARYANAM BIDDING
             Server      : http://localhost:8088
             Swagger UI  : http://localhost:8088/swagger-ui/index.html
             Swagger UI  : http://localhost:8088/swagger-ui/index.html



             ====================================================
             """);

    }

}


