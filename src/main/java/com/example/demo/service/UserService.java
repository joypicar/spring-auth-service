package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public User save(User user) {
        Optional<User> existingAccount = userRepository.findByLogin((user.getLogin()));
        if (existingAccount.isPresent()) {
            return existingAccount.get();
        }
        return userRepository.save(user);
    }
}
