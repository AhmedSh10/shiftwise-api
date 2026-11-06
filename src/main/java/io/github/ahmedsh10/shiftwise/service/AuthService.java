package io.github.ahmedsh10.shiftwise.service;

import io.github.ahmedsh10.shiftwise.domain.Employee;
import io.github.ahmedsh10.shiftwise.repository.EmployeeRepository;
import io.github.ahmedsh10.shiftwise.security.TokenService;
import io.github.ahmedsh10.shiftwise.web.dto.LoginRequest;
import io.github.ahmedsh10.shiftwise.web.dto.LoginResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final EmployeeRepository employees;
    private final TokenService tokens;

    public AuthService(AuthenticationManager authenticationManager, EmployeeRepository employees, TokenService tokens) {
        this.authenticationManager = authenticationManager;
        this.employees = employees;
        this.tokens = tokens;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Throws AuthenticationException (-> 401) on bad credentials or disabled account
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        Employee employee = employees.findByEmailIgnoreCase(request.email()).orElseThrow();
        TokenService.IssuedToken token = tokens.issue(employee);
        return new LoginResponse(token.value(), token.expiresAt(), employee.getId(), employee.getRole().name());
    }
}
