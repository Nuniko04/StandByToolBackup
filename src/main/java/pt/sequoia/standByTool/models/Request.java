package pt.sequoia.standByTool.models;

import jakarta.persistence.*;
import pt.sequoia.standByTool.models.enums.RequestStatus;
import pt.sequoia.standByTool.models.enums.RequestType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "requests")
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 50)
    private RequestType requestType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turn_id")
    private Turn turn;

    @Column(name = "time_off_start")
    private OffsetDateTime timeOffStart;

    @Column(name = "time_off_end")
    private OffsetDateTime timeOffEnd;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "requester_note", columnDefinition = "TEXT")
    private String requesterNote;

    @Column(name = "assigner_note", columnDefinition = "TEXT")
    private String assignerNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // --- Lifecycle Methods ---
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // --- Getters & Setters ---
    public UUID getId() {
        return id;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    public Turn getTurn() {
        return turn;
    }

    public void setTurn(Turn turn) {
        this.turn = turn;
    }

    public OffsetDateTime getTimeOffStart() {
        return timeOffStart;
    }

    public void setTimeOffStart(OffsetDateTime timeOffStart) {
        this.timeOffStart = timeOffStart;
    }

    public OffsetDateTime getTimeOffEnd() {
        return timeOffEnd;
    }

    public void setTimeOffEnd(OffsetDateTime timeOffEnd) {
        this.timeOffEnd = timeOffEnd;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getRequesterNote() {
        return requesterNote;
    }

    public void setRequesterNote(String requesterNote) {
        this.requesterNote = requesterNote;
    }

    public String getAssignerNote() {
        return assignerNote;
    }

    public void setAssignerNote(String assignerNote) {
        this.assignerNote = assignerNote;
    }

    public User getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(User processedBy) {
        this.processedBy = processedBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}