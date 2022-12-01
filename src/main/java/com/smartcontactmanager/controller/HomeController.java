package com.smartcontactmanager.controller;

import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.smartcontactmanager.dao.UserRepo;
import com.smartcontactmanager.entities.User;
import com.smartcontactmanager.helper.Message;
import com.smartcontactmanager.service.EmailService;

@Controller
public class HomeController {

	@Autowired
	private UserRepo userRepo;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Autowired
	private EmailService emailService;

	Random random=new Random(1000);
	
	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("title","Home | Smart Contact Manager");
		return "index";
	}

	@GetMapping("/about")
	public String about(Model model) {
		model.addAttribute("title","About | Smart Contact Manager");
		return "about";
	}

	@GetMapping("/signin")
	public String signin(Model model) {
		model.addAttribute("title","SignIn | Smart Contact Manager");
		return "signin";
	}

	@RequestMapping("/forgot")
	public String forgotPassword(Model model){
		model.addAttribute("title","Forgot Password | Smart Contact Manager");
		return "forgot";
	}

	@PostMapping("/verifyOTP")
	public String verifyOtp(@RequestParam("email") String email, Model model, HttpSession session){
		model.addAttribute("title","Verify OTP | Smart Contact Manager");
		int otpno=random.nextInt(999999);
		String subject="Otp from Smart Contact Manager";
		String message="<h1> Your one time password is "+otpno+" </h1>";
		String to=email;
		boolean flag = this.emailService.sendEmail(subject, message, to);
		if(flag){
			session.setAttribute("myotp", otpno);
			session.setAttribute("email", email);
			return "otp";
		}
		else{
			session.setAttribute("message", new Message("Your Entered Email isn't Registered!", "danger"));
			return "forgot";
		}
	}

	@PostMapping("/checkOtp")
	public String checkOtp(@RequestParam("otp") int otp, HttpSession session){
		int myotp=(int)session.getAttribute("myotp");
		String email=(String)session.getAttribute("email");
		if(myotp==otp){
			User user = this.userRepo.getUserByUserName(email);
			if(user==null){
				session.setAttribute("message", new Message("User does not exits with this email!", "danger"));
				return "forgot";
			}
			return "new_password";
		}
		else{
			session.setAttribute("message", new Message("Your Entered Otp is wrong!", "danger"));
			return "otp";
		}
	}

	@PostMapping("/newPassword")
	public String newPassword(Model model){
		model.addAttribute("title","New Password | Smart Contact Manager");
		return "new_password";
	}

	@PostMapping("/change_Password")
	public String changePassword(@RequestParam("newpassword") String newpassword, HttpSession session){
		String email=(String)session.getAttribute("email");
		User user = this.userRepo.getUserByUserName(email);
		user.setPassword(this.passwordEncoder.encode(newpassword));
		this.userRepo.save(user);
		return "redirect:/signin?change=Password changed sucessfully..";
	}

	@GetMapping("/signup")
	public String signup(Model model) {
		model.addAttribute("title","SignUp | Smart Contact Manager");
		model.addAttribute("user", new User());
		return "signup";
	}

	@PostMapping("/do_register")
	public String registerUser(@Valid @ModelAttribute("user") User user, BindingResult result, @RequestParam(value = "aggrement", defaultValue = "false") boolean aggrement, Model model, HttpSession httpSession) {
		try {
			if(!aggrement){
				System.out.println("Please accept terms and conditions");
				throw new Exception("Please accept terms and conditions");
			}
			if(result.hasErrors()){
				model.addAttribute("ERROR " +result.toString());
				model.addAttribute("user",user);
				return "signup";
			}
			user.setRole("ROLE_USER");
			user.setEnabled(true);
			user.setImageURL("contact.png");
			user.setPassword(passwordEncoder.encode(user.getPassword()));
			System.out.println(user);
			System.out.println(aggrement);
			this.userRepo.save(user);
			model.addAttribute("user", new User());
			httpSession.setAttribute("message", new Message("Successfully Registered!!","alert-success"));
			return "signup";
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("user", user);
			httpSession.setAttribute("message", new Message("Some Thing Went Wrong !!"+e.getMessage(),"alert-danger"));
			return "signup";
		}
	}

	@GetMapping("/donate")
	public String donateUs(Model model) {
		model.addAttribute("title","Donate Us | Smart Contact Manager");
		return "donate";
	}

	@PostMapping("/create_order")
	@ResponseBody
	public String createOrder(@RequestBody Map<String, Object> data) throws RazorpayException{
		int txnno=random.nextInt(999999);
		int amt = Integer.parseInt(data.get("amount").toString());
		var client= new RazorpayClient("", "");
		JSONObject ob = new JSONObject();
		ob.put("amount", amt*100);
		ob.put("currency", "INR");
		ob.put("receipt", "txn_"+txnno);
		Order order = client.Orders.create(ob);
		return order.toString();
	}

}
