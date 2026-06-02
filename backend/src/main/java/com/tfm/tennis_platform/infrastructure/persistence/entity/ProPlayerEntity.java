package com.tfm.tennis_platform.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "pro_players")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProPlayerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "puntos")
    private Integer points;

    @Column(name = "licencia")
    private String license;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "posicion")
    private Integer rankingPosition;

    @Column(name = "posicion_territorial")
    private Integer territorialPosition;

    @Column(name = "posicion_provincial")
    private Integer provincialPosition;

    @Column(name = "posicion_club")
    private Integer clubPosition;

    @Column(name = "posicion_edad")
    private Integer agePosition;

    @Column(name = "edad")
    private String ageCategory;

    @Column(name = "nombre_club")
    private String clubName;

    @Column(name = "nombre_provincial")
    private String provincialName;

    @Column(name = "nombre_territorial")
    private String territorialName;

    @Column(name = "nombre_categoria")
    private String categoryName;

    @Column(name = "puntos_otorga")
    private Integer awardedPoints;

    @Column(name = "fecha_nacimiento")
    private LocalDate birthDate;

    @Column(name = "gender")
    private String gender;
}
