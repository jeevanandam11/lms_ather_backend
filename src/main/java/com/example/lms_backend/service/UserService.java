package com.example.lms_backend.service;

import com.example.lms_backend.model.UserEntity;
import com.example.lms_backend.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
@Autowired
    private UserRepo userRepo;

  public List<UserEntity> getAllUsers(){
      return userRepo.findAll();
  }

  public UserEntity getUserById(Long id){
      return userRepo.findById(id)
              .orElseThrow();
  }
  public UserEntity createUser(UserEntity user){
      userRepo.findByEmail(user.getEmail())
              .ifPresent(u->{
                  throw new RuntimeException("User already present");
              });
      return userRepo.save(user);

  }
  public UserEntity updateUser(UserEntity updatedUser){
      UserEntity existingUser=userRepo.findById(updatedUser.getId())
              .orElseThrow();
      existingUser.setFirstName(updatedUser.getFirstName());
      existingUser.setLastName(updatedUser.getLastName());
      existingUser.setEmail(updatedUser.getEmail());
      existingUser.setPassword(updatedUser.getPassword());
      existingUser.setAge(updatedUser.getAge());
      existingUser.setDateOfBirth(updatedUser.getDateOfBirth());
      existingUser.setUserType(updatedUser.getUserType());
      existingUser.setPurposeOfStudy(updatedUser.getPurposeOfStudy());
      if (updatedUser.getImageUrl() != null) {
          existingUser.setImageUrl(updatedUser.getImageUrl());
      }
      return userRepo.save(existingUser);
  }

  public void deleteUser(Long id){
      userRepo.deleteById(id);
  }
}
