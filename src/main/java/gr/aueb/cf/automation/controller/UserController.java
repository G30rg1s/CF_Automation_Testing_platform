package gr.aueb.cf.automation.controller;

import gr.aueb.cf.automation.dto.UserLoginDto;
import gr.aueb.cf.automation.dto.UserLoginResponseDto;
import gr.aueb.cf.automation.dto.UserRegisterDto;
import gr.aueb.cf.automation.dto.UserResponseDto;
import gr.aueb.cf.automation.dto.UserUpdateDto;
import gr.aueb.cf.automation.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cf_automation")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }


    @PostMapping("/users/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponseDto register(
            @Valid @RequestBody UserRegisterDto request) {

        return userService.register(request);
    }


    @PostMapping("/users/login")
    public ResponseEntity<Void> login(
            @Valid @RequestBody UserLoginDto request) {

        UserLoginResponseDto loginResponse = userService.login(request);

        ResponseCookie accessTokenCookie =
                ResponseCookie
                        .from("access_token", loginResponse.getAccessToken())
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .build();

        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .build();
    }


    @GetMapping("/users/me")
    @SecurityRequirement(name = "bearerAuth")
    public UserResponseDto getCurrentUser(@AuthenticationPrincipal Jwt jwt) {

        Long userId = Long.valueOf(jwt.getSubject());

        return userService.getById(userId);
    }


    @PutMapping("/users/update")
    @SecurityRequirement(name = "bearerAuth")
    public UserResponseDto update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UserUpdateDto request) {

        Long userId = Long.valueOf(jwt.getSubject());

        return userService.update(userId, request);
    }

    @PostMapping("/users/logout")
    public ResponseEntity<Void> logout() {

        ResponseCookie deleteCookie =
                ResponseCookie
                        .from("access_token", "")
                        .httpOnly(true)
                        .secure(false)
                        .sameSite("Lax")
                        .path("/")
                        .maxAge(0)
                        .build();

        return ResponseEntity
                .noContent()
                .header(
                        HttpHeaders.SET_COOKIE,
                        deleteCookie.toString()
                )
                .build();
    }
}