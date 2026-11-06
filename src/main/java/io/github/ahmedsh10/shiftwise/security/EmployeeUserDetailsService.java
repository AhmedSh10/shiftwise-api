package io.github.ahmedsh10.shiftwise.security;

import io.github.ahmedsh10.shiftwise.repository.EmployeeRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class EmployeeUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employees;

    public EmployeeUserDetailsService(EmployeeRepository employees) {
        this.employees = employees;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return employees.findByEmailIgnoreCase(email)
                .map(e -> User.withUsername(e.getEmail())
                        .password(e.getPasswordHash())
                        .roles(e.getRole().name())
                        .disabled(!e.isActive())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Unknown user"));
    }
}
