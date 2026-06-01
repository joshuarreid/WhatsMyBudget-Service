package com.example.wmbservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "criticality")
public class Criticality {

    @Id
    private Long id;

    @NotBlank
    @Size(max = 32)
    @Column(nullable = false, unique = true, length = 32)
    private String name;

    public Criticality(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
