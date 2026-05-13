package pt.sequoia.standByTool.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "turn_types")
@Getter
@Setter
public class TurnType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "google_calendar_id", nullable = false)
    private String googleCalendarId;

    @Column(name = "default_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal defaultValue = BigDecimal.ZERO;

}