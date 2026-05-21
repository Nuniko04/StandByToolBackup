package pt.sequoia.standByTool.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "cards")
@Getter
@Setter
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Identificador único do cartão (Ex: "Cartão 01" ou "4561655")
    @Column(nullable = false, unique = true)
    private String identifier;

    // A data de validade em formato texto "YYYY-MM" (Ex: "2028-12")
    @Column(name = "expiration_date", nullable = false)
    private String expirationDate;
}