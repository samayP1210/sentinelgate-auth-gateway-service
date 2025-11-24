package com.sentinelgate.security;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
@Getter
public class AuthClass {

    private Set<String> excludingPaths = new HashSet<>();

    @PostConstruct
    private void init() {
        String[] paths = new String[]{
          "/auth/**",
          "/health"
        };
        this.excludingPaths = new HashSet<>(Arrays.asList(paths));
    }

}
