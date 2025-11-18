package com.sentinelgate.manager;

import com.sentinelgate.database.mysql.dao.UserDao;
import com.sentinelgate.database.mysql.entity.User;
import com.sentinelgate.response.AuthResponse;
import com.sentinelgate.utils.EncryptionUtils;
import com.sentinelgate.utils.JwtUtils;
import io.micrometer.common.util.StringUtils;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class AuthManager {

    @Autowired
    UserDao userDao;

    @Autowired
    EncryptionUtils encryptionUtils;

    @Autowired
    JwtUtils jwtUtils;

    Logger log = LoggerFactory.getLogger(AuthManager.class);

    public ResponseEntity<AuthResponse> login(String username, String password) {
        try {
            log.info("Login request for {}", username);

            if (StringUtils.isBlank(username)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthResponse.builder().message("username cannot be empty.").build());
            } else if (StringUtils.isBlank(password)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthResponse.builder().message("password cannot be empty.").build());
            }

            Optional<User> user = userDao.findFirstByUsername(username);
            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthResponse.builder().message("Invalid credentials.").build());
            }

            String decryptedPassword = encryptionUtils.decrypt(user.get().getPassword());

            if (!Objects.equals(decryptedPassword, password)){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthResponse.builder().message("Invalid credentials.").build());
            }

            String token = jwtUtils.generateToken(user.get());

            return ResponseEntity.ok(AuthResponse.builder().token(token).message("Success").build());
        } catch (Exception e) {
            log.error("Error while logging in err: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AuthResponse.builder().message("Failed!").build());
        }
    }

    public ResponseEntity<AuthResponse> signup(@NonNull String username, @NonNull String password) {
        try {
            log.info("Signup request for {}", username);

            if (StringUtils.isBlank(username)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AuthResponse.builder().message("username cannot be empty.").build());
            } else if (StringUtils.isBlank(password)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(AuthResponse.builder().message("password cannot be empty.").build());
            }

            Optional<User> optionalUser = userDao.findFirstByUsername(username);
            if (optionalUser.isPresent()) {
                String decryptedPassword = encryptionUtils.decrypt(optionalUser.get().getPassword());
                if (! Objects.equals(decryptedPassword, password)){
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthResponse.builder().message("Invalid credentials.").build());
                }
                String token = jwtUtils.generateToken(optionalUser.get());
                return ResponseEntity.ok(AuthResponse.builder().token(token).message("Success").build());
            }

            User user = User.builder().username(username).password(encryptionUtils.encrypt(password)).build();
            user = userDao.save(user);

            String token = jwtUtils.generateToken(user);

            return ResponseEntity.ok(AuthResponse.builder().token(token).message("Success").build());
        } catch (Exception e) {
            log.error("Error while signing up err: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(AuthResponse.builder().message("Failed!").build());
        }
    }
}
