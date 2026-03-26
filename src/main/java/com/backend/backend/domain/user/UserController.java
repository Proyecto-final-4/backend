package com.backend.backend.domain.user;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getCurrentUser(email);
    }

    @PutMapping("/me")
    public UserResponse updateUser(@RequestBody UpdateUserRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.updateUser(email, request);
    }
}
