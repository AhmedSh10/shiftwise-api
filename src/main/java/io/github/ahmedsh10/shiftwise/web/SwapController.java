package io.github.ahmedsh10.shiftwise.web;

import io.github.ahmedsh10.shiftwise.security.CurrentUser;
import io.github.ahmedsh10.shiftwise.service.SwapService;
import io.github.ahmedsh10.shiftwise.web.dto.CreateSwapRequest;
import io.github.ahmedsh10.shiftwise.web.dto.SwapResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/swaps")
public class SwapController {

    private final SwapService swapService;

    public SwapController(SwapService swapService) {
        this.swapService = swapService;
    }

    /** An employee offers one of their shifts to a colleague. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SwapResponse request(@Valid @RequestBody CreateSwapRequest request, @AuthenticationPrincipal Jwt jwt) {
        return swapService.request(request, CurrentUser.from(jwt));
    }

    @GetMapping
    public List<SwapResponse> list(@RequestParam(defaultValue = "false") boolean openOnly,
                                   @AuthenticationPrincipal Jwt jwt) {
        return swapService.list(CurrentUser.from(jwt), openOnly);
    }

    @PostMapping("/{id}/accept")
    public SwapResponse accept(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return swapService.accept(id, CurrentUser.from(jwt));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public SwapResponse approve(@PathVariable Long id) {
        return swapService.approve(id);
    }

    @PostMapping("/{id}/reject")
    public SwapResponse reject(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return swapService.reject(id, CurrentUser.from(jwt));
    }

    @PostMapping("/{id}/cancel")
    public SwapResponse cancel(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return swapService.cancel(id, CurrentUser.from(jwt));
    }
}
