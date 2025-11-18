package com.sentinelgate.contollers;

import com.sentinelgate.manager.AuthManager;
import com.sentinelgate.request.AuthRequest;
import com.sentinelgate.response.AuthResponse;
import com.sentinelgate.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    AuthManager authManager;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request){
        return authManager.login(request.getUsername(), request.getPassword());
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody AuthRequest request){
        return authManager.signup(request.getUsername(), request.getPassword());
    }

    @PostMapping("/validate")
    public Object validate(@RequestParam("token") String token){
        return new JwtUtils().validateToken(token);
    }

}
