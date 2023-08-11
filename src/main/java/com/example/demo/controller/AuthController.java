package com.example.demo.controller;

import com.example.demo.domain.entity.User;
import com.example.demo.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
public class AuthController {

    public final String clientId = System.getenv("GITHUB_CLIENT_ID");
    public final String clientSecret = System.getenv("GITHUB_CLIENT_SECRET");
    public final String redirectUri = System.getenv("REDIRECT_URI");
    public final String callbackUri = System.getenv("CALLBACK_URI");

    @Autowired
    private UserService userService;


    @GetMapping("/authorize")
    public void authorize(HttpServletResponse response) throws IOException {
        // Construct the authorization URL
        String authorizationUrl = "https://github.com/login/oauth/authorize"
                + "?client_id=" + clientId
                + "&redirect_uri=" + redirectUri
                + "&scope=user";

        // Redirect the user to the authorization URL
        response.sendRedirect(authorizationUrl);
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/callback")
    public ResponseEntity<Map<String, String>> handleAuthorizationResponse(@RequestParam("code") String code, HttpServletResponse response) throws IOException {
        // Make a POST request to the GitHub API to exchange the authorization code for an access token
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        HttpEntity<String> request = new HttpEntity<>("", headers);
        ResponseEntity<String> responseEntity = restTemplate.exchange(
                "https://github.com/login/oauth/access_token"
                        + "?client_id=" + clientId
                        + "&client_secret=" + clientSecret
                        + "&redirect_uri=" + redirectUri
                        + "&code=" + code,
                HttpMethod.POST,
                request,
                String.class);

        // Extract the access token from the response body
        String responseBody = responseEntity.getBody();

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});

        String accessToken = (String) responseMap.get("access_token");
        System.out.println("accessToken" + accessToken);

        Map<String, String> responseBodyMap = new HashMap<>();
        responseBodyMap.put("access_token", accessToken);

        // get user information from Github using code
        User githubUser = (User) getGithubUser(accessToken);

        // create User object
        User user = new User();
//        user.setId(githubUser.getId());
        user.setLogin(githubUser.getLogin());
        user.setName(githubUser.getName());
        user.setEmail(githubUser.getEmail());
        System.out.println("user saved" + user);

        // save user to database
        userService.save(user);

        return ResponseEntity.ok().body(responseBodyMap);
    }

    public User getGithubUser(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        ResponseEntity<User> response = restTemplate.exchange(
                "https://api.github.com/user",
                HttpMethod.GET,
                entity,
                User.class
        );
        User githubUser = response.getBody();
        return githubUser;
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/users")
    public Map<String, Object> getUserInfo(@RequestHeader(name = "Authorization") String token) {
        String accessToken = token.replaceAll("Bearer ", "");

        // Set up the headers for the API request
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        // Make a request to the GitHub API to retrieve user information
        RestTemplate restTemplate = new RestTemplate();
        HttpEntity<String> request = new HttpEntity<>("", headers);
        ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.github.com/user",
                HttpMethod.GET,
                request,
                Map.class);

        // Extract the user information from the response body
        Map<String, Object> userData = response.getBody();
        return userData;
    }

}
