package com.smartcontactmanager.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.smartcontactmanager.dao.ContactRepo;
import com.smartcontactmanager.dao.UserRepo;
import com.smartcontactmanager.entities.Contact;
import com.smartcontactmanager.entities.User;

@RestController
public class SearchController {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ContactRepo contactRepo;
   
    @GetMapping("/search/{query}")
    public ResponseEntity<?> search(@PathVariable("query") String query, Principal principal){
        User user = this.userRepo.getUserByUserName(principal.getName());
        List<Contact> contacts = this.contactRepo.findByNameContainingAndUser(query, user);
        return ResponseEntity.ok(contacts);
    }
}
