package com.payment_bank.controller;

import com.payment_bank.helpers.HTML;
import com.payment_bank.helpers.Token;
import com.payment_bank.mailMessenger.MailMessenger;
import com.payment_bank.models.User;
import com.payment_bank.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.mail.MessagingException;
import javax.validation.Valid;
import java.util.Random;

@Controller
public class RegisterController {

    private final UserRepository userRepository;
    private final MailMessenger mailMessenger;

    @Autowired
    public RegisterController(UserRepository userRepository, MailMessenger mailMessenger) {
        this.userRepository = userRepository;
        this.mailMessenger = mailMessenger;
    }

    @GetMapping("/register")
    public ModelAndView getRegister() {
        ModelAndView getRegisterPage = new ModelAndView("register");
        getRegisterPage.addObject("PageTitle", "Register");
        return getRegisterPage;
    }

    @PostMapping("/register")
    public ModelAndView register(@Valid @ModelAttribute("registerUser") User user,
                                 BindingResult result,
                                 @RequestParam("first_name") String first_name,
                                 @RequestParam("last_name") String last_name,
                                 @RequestParam("email") String email,
                                 @RequestParam("password") String password,
                                 @RequestParam("confirm_password") String confirm_password) {
        ModelAndView registrationPage = new ModelAndView("register");

        if (result.hasErrors() || confirm_password.isEmpty()) {
            registrationPage.addObject("confirm_pass", "The confirm Field is required");
            return registrationPage;
        }

        if (!password.equals(confirm_password)) {
            registrationPage.addObject("passwordMisMatch", "Passwords do not match");
            return registrationPage;
        }

        String token = Token.generateToken();
        int code = generateRandomCode();
        String emailBody = HTML.htmlEmailTemplate(token, code);
        String hashed_password = BCrypt.hashpw(password, BCrypt.gensalt());

        userRepository.registerUser(first_name, last_name, email, hashed_password, token, code);

        try {
            mailMessenger.htmlEmailMessenger("no-reply@paymentbank.com", email, "Verify Account", emailBody);
            ModelAndView loginPage = new ModelAndView("login");
            loginPage.addObject("success", "Account Registered Successfully, Please Check your Email and Verify Account!");
            return loginPage;
        } catch (MessagingException e) {
            registrationPage.addObject("error", "Failed to send verification email. Please try again later.");
        }

        return registrationPage;
    }

    private int generateRandomCode() {
        Random rand = new Random();
        int bound = 1000; // Adjusted bound to ensure a proper range
        return rand.nextInt(bound) + 1000; // Ensure code is at least 1000
    }
}
