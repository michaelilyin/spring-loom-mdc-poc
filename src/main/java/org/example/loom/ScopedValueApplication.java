package org.example.loom;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.loom.support.mdc.ScopedMDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@SpringBootApplication
@EnableAsync
public class ScopedValueApplication {
    public static ConfigurableApplicationContext start(String[] args) {
        return SpringApplication.run(ScopedValueApplication.class, args);
    }
}

@Component
class Filter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        MDC.put("requestId", UUID.randomUUID().toString());
//        MDC.put("requestId", "111");
        filterChain.doFilter(request, response);
    }
}

@EnableWebSecurity
@Configuration
class SecurityConfig {
    private static final String NOOP_PASSWORD_PREFIX = "{noop}";
    private static final Pattern PASSWORD_ALGORITHM_PATTERN = Pattern.compile("^\\{.+}.*$");

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            InMemoryUserDetailsManager userDetailsManager
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .userDetailsService(userDetailsManager)
                .authorizeHttpRequests(auth -> {
                    auth.anyRequest().authenticated();
                })
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager(SecurityProperties properties, ObjectProvider<PasswordEncoder> passwordEncoder) {
        SecurityProperties.User user = properties.getUser();
        List<String> roles = user.getRoles();
        return new InMemoryUserDetailsManager(
                User.withUsername("user").password(this.getOrDeducePassword("password", passwordEncoder.getIfAvailable())).roles(StringUtils.toStringArray(roles)).build(),
                User.withUsername("other").password(this.getOrDeducePassword("password", passwordEncoder.getIfAvailable())).roles(StringUtils.toStringArray(roles)).build()
        );
    }

    private String getOrDeducePassword(String password, PasswordEncoder encoder) {
        return encoder == null && !PASSWORD_ALGORITHM_PATTERN.matcher(password).matches() ? NOOP_PASSWORD_PREFIX + password : password;
    }
}

@RestController
@RequestMapping("/api")
class TestController {
    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    private final AsyncService asyncService;

    TestController(AsyncService asyncService) {
        this.asyncService = asyncService;
    }

    @GetMapping("/test")
    public String getTest() throws InterruptedException {
        var name = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("controller:: is virtual:{}, username:{}", Thread.currentThread().isVirtual(), name);

        doScopedWork();
        asyncService.doAsyncWork();

        return "OK";
    }

    @GetMapping("/scope")
    public String getScope() throws InterruptedException {
        return doScopedWork();
    }


    @GetMapping("/async")
    public String getAsync() throws InterruptedException, ExecutionException {
        return doAsyncWork();
    }

    private String doAsyncWork() throws InterruptedException, ExecutionException {
        log.info("start async work");
        try {
            var res1 = asyncService.doAsyncWork();
            var res2 = asyncService.doAsyncWork();
            return res1.get() + res2.get();
        } finally {
            log.info("complete async work");
        }
    }

    private String doScopedWork() throws InterruptedException {
        log.info("start scoped work");
        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
            var res1 = scope.fork(() -> subtask(UUID.randomUUID().toString()));
            var res2 = scope.fork(() -> subtask(UUID.randomUUID().toString()));
            scope.join();
            return res1.get() + res2.get();
        } finally {
            log.info("complete scoped work");
        }
    }

    private static String subtask(String id) throws Exception {
        return ScopedMDC.isolate(() -> {
            log.info("subtask:: override request id");
            MDC.put("requestId", id);
            log.info("subtask:: start");
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            var name = authentication == null ? "unknown" : authentication.getName();
            log.info("subtask:: is virtual:{}, username:{}", Thread.currentThread().isVirtual(), name);
            return id;
        });
    }
}

@Service
class AsyncService {
    private static final Logger log = LoggerFactory.getLogger(AsyncService.class);

    @Async
    public Future<String> doAsyncWork() throws InterruptedException {
        String requestId = UUID.randomUUID().toString();
        log.info("async:: override request id");
        MDC.put("requestId", requestId);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        var name = authentication == null ? "unknown" : authentication.getName();
        log.info("async:: is virtual:{}, username:{}", Thread.currentThread().isVirtual(), name);
        return CompletableFuture.completedFuture(requestId);
    }
}