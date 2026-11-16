package io.github.ahmedsh10.shiftwise.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** "Can you take my shift?" Employee asks, colleague accepts, supervisor approves. */
@Entity
@Table(name = "swap_requests")
public class SwapRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "requester_id")
    private Employee requester;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "target_employee_id")
    private Employee target;

    @Enumerated(EnumType.STRING)
    private SwapStatus status = SwapStatus.PENDING_COLLEAGUE;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "decided_at")
    private Instant decidedAt;

    protected SwapRequest() {
    }

    public SwapRequest(Assignment assignment, Employee requester, Employee target) {
        this.assignment = assignment;
        this.requester = requester;
        this.target = target;
    }

    public void moveTo(SwapStatus next) {
        this.status = next;
        if (!next.isOpen()) {
            this.decidedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Assignment getAssignment() {
        return assignment;
    }

    public Employee getRequester() {
        return requester;
    }

    public Employee getTarget() {
        return target;
    }

    public SwapStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }
}
