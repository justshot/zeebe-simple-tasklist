/*
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.zeebe.tasklist;

import io.camunda.zeebe.spring.client.EnableZeebeClient;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@SpringBootApplication
@EnableZeebeClient
@EnableScheduling
@EnableAsync
@EnableSpringDataWebSupport
@ComponentScan(basePackages = {"io.zeebe.tasklist","com.jiasl"})
public class ZeebeSimpleTasklistApp {

  private static final Logger LOG = LoggerFactory.getLogger(ZeebeSimpleTasklistApp.class);

  @Value("${zeebe.client.worker.tasklist.adminUsername}")
  private String adminUsername;

  @Value("${zeebe.client.worker.tasklist.adminPassword}")
  private String adminPassword;

  @Value("${server.allowedOriginsUrls}")
  private String allowedOriginsUrls;

  @Autowired private UserService userService;

  public static void main(String... args) {
    SpringApplication.run(ZeebeSimpleTasklistApp.class, args);
  }

  @PostConstruct
  public void init() {
    if (!adminUsername.isEmpty() && !userService.hasUserWithName(adminUsername)) {
      LOG.info(
          "Creating admin user with name '{}' and password '{}'", adminUsername, adminPassword);
      userService.newAdminUser(adminUsername, adminPassword);
    }
  }

  @Bean
  public WebMvcConfigurer corsConfigurer() {
    final String urls = this.allowedOriginsUrls;
    return new WebMvcConfigurerAdapter() {
      @Override
      public void addCorsMappings(CorsRegistry registry) {
        if (StringUtils.hasText(urls)) {
          String[] allowedOriginsUrlArr = urls.split(";");
          registry.addMapping("/**").allowedOrigins(allowedOriginsUrlArr);
        }
      }
    };
  }
}
