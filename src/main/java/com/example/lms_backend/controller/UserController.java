package com.example.lms_backend.controller;

import com.example.lms_backend.model.UserEntity;
import com.example.lms_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@CrossOrigin
public class UserController {
    @Autowired
    private UserService userService;
    private final String IMAGE_DIR = "uploads/images";
    private final String VIDEO_DIR = "uploads/videos/";
    @GetMapping
    public List<UserEntity> getAllUsers(){
        return userService.getAllUsers();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserEntity addUser(@RequestParam("firstName") String firstName,@RequestParam("lastName") String lastName,@RequestParam("dateOfBirth") LocalDate dateOfBirth,@RequestParam("age") int age,@RequestParam("email") String email, @RequestParam("password") String password, @RequestParam("userType") String userType, @RequestParam("purposeOfStudy") String purposeOfStudy,@RequestParam(value = "imageUrl", required = false) MultipartFile image)throws IOException {

        Path uploadPath = Paths.get("uploads");
        if(!Files.exists(uploadPath)){
            Files.createDirectories(uploadPath);
        }
        
        String imageFileName = null;
        if(image != null && !image.isEmpty()){
            imageFileName  = image.getOriginalFilename();
            Files.copy(image.getInputStream(),uploadPath.resolve(imageFileName));
        }

        UserEntity userEntity=new UserEntity();
        userEntity.setFirstName(firstName);
        userEntity.setLastName(lastName);
        userEntity.setDateOfBirth(dateOfBirth);
        userEntity.setAge(age);
        userEntity.setEmail(email);
        userEntity.setPassword(password);
        userEntity.setUserType(userType);
        userEntity.setPurposeOfStudy(purposeOfStudy);
        userEntity.setImageUrl(imageFileName);
        return userService.createUser(userEntity);

    }
    @GetMapping("/{id}")
    public UserEntity getUserById(@PathVariable("id") Long id){
        return userService.getUserById(id);

    }

    @PutMapping("/{id}")
    public UserEntity updateUser(@PathVariable("id") Long id,@RequestParam("firstName") String firstName,@RequestParam("lastName") String lastName,@RequestParam("dateOfBirth") LocalDate dateOfBirth,@RequestParam("age") int age,@RequestParam("email") String email, @RequestParam("password") String password, @RequestParam("userType") String userType, @RequestParam("purposeOfStudy") String purposeOfStudy,@RequestParam(value = "imageUrl", required = false) MultipartFile image)throws IOException{
        UserEntity existing = userService.getUserById(id);
        existing.setFirstName(firstName);
        existing.setLastName(lastName);
        existing.setDateOfBirth(dateOfBirth);
        existing.setAge(age);
        existing.setEmail(email);
        existing.setPassword(password);
        existing.setUserType(userType);
        existing.setPurposeOfStudy(purposeOfStudy);
        String uploadDir = "uploads/";
        Files.createDirectories(Paths.get(uploadDir));

        if(image!=null && !image.isEmpty()){


//            Delete Old image
            String oldImage = existing.getImageUrl();
            if(oldImage!=null && !oldImage.isEmpty()){
                Path oldImagePath = Paths.get(uploadDir + oldImage);
                Files.deleteIfExists(oldImagePath);
            }

//            Save new image
            String newFileName = System.currentTimeMillis()+"_"+
                    image.getOriginalFilename();
            Path newImagePath = Paths.get(uploadDir + newFileName);
            Files.copy(image.getInputStream(), newImagePath, StandardCopyOption.REPLACE_EXISTING);
            existing.setImageUrl(newFileName);
        }

        return userService.updateUser(existing);
    }

    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable("id") Long id){
        userService.deleteUser(id);
        return "User deleted successfully!";
    }




}
