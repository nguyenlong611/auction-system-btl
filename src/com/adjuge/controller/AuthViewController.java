package com.adjuge.controller;

import com.adjuge.exception.AuthenticationException;
import com.adjuge.model.User;
import com.adjuge.service.AuthService;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Sub-controller responsible for the Authentication view (SRP).
 * Handles login/register tabs and form submissions.
 * Exception Handling: AuthenticationException shown inline per form.
 */
public class AuthViewController {

    private final VBox          formLogin, formRegister;
    private final TextField     loginEmail, regFirstName, regLastName, regEmail;
    private final PasswordField loginPassword, regPassword, regConfirm;
    private final Label         loginError, regError;
    private final AuthService   authService;
    private final Consumer<User> onLoginSuccess;

    public AuthViewController(
            VBox formLogin, VBox formRegister,
            TextField loginEmail, PasswordField loginPassword, Label loginError,
            TextField regFirstName, TextField regLastName,
            TextField regEmail, PasswordField regPassword, PasswordField regConfirm,
            Label regError, AuthService authService, Consumer<User> onLoginSuccess) {
        this.formLogin = formLogin; this.formRegister = formRegister;
        this.loginEmail = loginEmail; this.loginPassword = loginPassword;
        this.loginError = loginError; this.regFirstName = regFirstName;
        this.regLastName = regLastName; this.regEmail = regEmail;
        this.regPassword = regPassword; this.regConfirm = regConfirm;
        this.regError = regError; this.authService = authService;
        this.onLoginSuccess = onLoginSuccess;
    }

    public void showSignInTab() {
        formLogin.setVisible(true); formLogin.setManaged(true);
        formRegister.setVisible(false); formRegister.setManaged(false);
        loginError.setText("");
    }

    public void showCreateAccountTab() {
        formRegister.setVisible(true); formRegister.setManaged(true);
        formLogin.setVisible(false); formLogin.setManaged(false);
        regError.setText("");
    }

    public void onLogin() {
        try {
            User user = authService.login(loginEmail.getText().trim(), loginPassword.getText());
            loginError.setText("");
            onLoginSuccess.accept(user);
        } catch (AuthenticationException e) {
            loginError.setText(e.getMessage());
        }
    }

    public void onRegister() {
        try {
            User user = authService.register(
                    regFirstName.getText().trim(), regLastName.getText().trim(),
                    regEmail.getText().trim(), regPassword.getText(), regConfirm.getText());
            regError.setText("");
            onLoginSuccess.accept(user);
        } catch (AuthenticationException e) {
            regError.setText(e.getMessage());
        }
    }
}
