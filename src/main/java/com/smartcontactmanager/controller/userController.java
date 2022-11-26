package com.smartcontactmanager.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.smartcontactmanager.dao.ContactRepo;
import com.smartcontactmanager.dao.UserRepo;
import com.smartcontactmanager.entities.Contact;
import com.smartcontactmanager.entities.User;
import com.smartcontactmanager.helper.Message;

@Controller
@RequestMapping("/user")
public class userController {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ContactRepo contactRepo;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @ModelAttribute
    public void addCommonData(Model model, Principal principal){
        String userName = principal.getName();
        User user = userRepo.getUserByUserName(userName);
        model.addAttribute("user", user);
    }

    @RequestMapping("/dashboard")
    public String dashboard(Model model){
        model.addAttribute("title", "User | Smart Contact Manager");
        return "user/user_dashboard";
    }

    @RequestMapping("/addContact")
    public String openAddContactForm(Model model){
        model.addAttribute("title", "User | Add Contact");
        model.addAttribute("contact", new Contact());
        return "user/add_contact_form";
    }

    @RequestMapping("/viewContact/{page}")
    public String viewUserContact(@PathVariable("page") Integer page, Model model, Principal principal){
        model.addAttribute("title", "User | ALL Contact");
        String userName = principal.getName();
        User user = this.userRepo.getUserByUserName(userName);
        Pageable pageable=PageRequest.of(page, 5);
        Page<Contact> contacts = this.contactRepo.findContactsOfUser(user.getId(),pageable);
        model.addAttribute("contacts", contacts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", contacts.getTotalPages());
        return "user/view_contact";
    }

    @RequestMapping("/viewProfile")
    public String viewUserProfile(Model model){
        model.addAttribute("title", "User | Profile");
        return "user/user_profile";
    }

    @RequestMapping("/viewSetting")
    public String userSetting(Model model){
        model.addAttribute("title", "User | Setting");
        return "user/user_setting";
    }

    @PostMapping("/processContact")
    public String processContact(@ModelAttribute Contact contact, @RequestParam("contactImage") MultipartFile file, Principal principal, HttpSession session){
        try {
            String name = principal.getName();
            User user = this.userRepo.getUserByUserName(name);
            if(file.isEmpty()){
                contact.setImageURL("contact.png");
            }
            else{
                contact.setImageURL(file.getOriginalFilename());
                File savFile = new ClassPathResource("static/image").getFile();
                Path path = Paths.get(savFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            }
            contact.setUser(user);
            user.getContacts().add(contact);
            this.userRepo.save(user);
            session.setAttribute("message", new Message("Contact is added sucessfully.", "success"));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
            session.setAttribute("message", new Message("Something went wrong! try again...", "danger"));
        }
        return "user/add_contact_form";
    }

    @RequestMapping("/contact/{cid}")
    public String particularContactDetail(@PathVariable("cid") Integer cid, Model model, Principal principal){
        Optional<Contact> contactOptional = this.contactRepo.findById(cid);
        Contact contact = contactOptional.get();
        String userName = principal.getName();
        User user = this.userRepo.getUserByUserName(userName);
        if(user.getId()==contact.getUser().getId()){
            model.addAttribute("contact", contact);
        }
        model.addAttribute("title", "User | Contact");
        return "user/contact_detail";
    }

    @GetMapping("/editContact/{cid}")
    public String editContact(@PathVariable("cid") Integer cid, Model model){
        model.addAttribute("title", "User | Edit Contact");
        Contact contact = this.contactRepo.findById(cid).get();
        model.addAttribute("contact", contact);
        return "user/edit_contact_form";
    }

    @PostMapping("/processEditContact")
    public String processEditContact(@ModelAttribute Contact contact, @RequestParam("contactImage") MultipartFile file, Model model, HttpSession session, Principal principal){
        try {
            Contact oldContactDetail = this.contactRepo.findById(contact.getCid()).get();
            if(file.isEmpty()){
                contact.setImageURL(oldContactDetail.getImageURL());
            }
            else{
                //delete old photo
                File deleteFile = new ClassPathResource("static/image").getFile();
                File file2 = new File(deleteFile, oldContactDetail.getImageURL());
                file2.delete();
                //update new photo
                File savFile = new ClassPathResource("static/image").getFile();
                Path path = Paths.get(savFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                contact.setImageURL(file.getOriginalFilename());
            }
            User user = this.userRepo.getUserByUserName(principal.getName());
            contact.setUser(user);
            this.contactRepo.save(contact);
            session.setAttribute("message", new Message("Your Contact is Updated", "success"));    
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "redirect:/user/viewContact/0";
    }

    @GetMapping("/deleteContact/{cid}")
    public String deleteContact(@PathVariable("cid") Integer cid, Model model, Principal principal, HttpSession session){
        Contact contact = this.contactRepo.findById(cid).get();
        User user = this.userRepo.getUserByUserName(principal.getName());
        user.getContacts().remove(contact);
        this.userRepo.save(user);
        session.setAttribute("message", new Message("Contact Deleted Sucessfully.", "success"));
        return "redirect:/user/viewContact/0";
    }

    @PostMapping("/changePassword")
    public String changePassword(@RequestParam("oldPassword") String oldPassword, @RequestParam("newPassword") String newPassword, Principal principal, HttpSession session){
        String userName = principal.getName();
        User user = this.userRepo.getUserByUserName(userName);
        if(this.bCryptPasswordEncoder.matches(oldPassword, user.getPassword())){
            user.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
            this.userRepo.save(user);
            session.setAttribute("message", new Message("Your Password is Changed Sucessfully.", "success"));
        }
        else{
            session.setAttribute("message", new Message("Your entered Old Password is wrong!", "danger"));
            return "user/user_setting";
        }
        return "user/user_setting";
    }

}
