package org.nab.new_afm_back.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "cases1")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Case {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "number")
    private String number;

    @Column(name = "uploadDate")
    private LocalDate uploadDate;

    @Column(name = "updateDate")
    private LocalDate updateDate;

    @Column(name = "qualificationDate")
    private LocalDate qualificationDate;

    @Column(name = "accusationDate")
    private LocalDate accusationDate;

    @Column(name = "registrationDate")
    private LocalDate registrationDate;

    @Column(name = "author")
    private String author;

    @Column(name = "investigator")
    private String investigator;

    @Column(name = "policeman")
    private String policeman;

    @ElementCollection
    @CollectionTable(name = "case_articles1", joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "article")
    private List<Integer> articles;

    @Column(name = "object")
    private String object;
    @Column(name = "organ")
    private String organ;
    @Column(name = "qualification")
    private String qualification;
    @Column(name = "damage_amount", precision = 19, scale = 2)
    private BigDecimal damageAmount;

    @Column(name = "criminal_income_amount", precision = 19, scale = 2)
    private BigDecimal criminalIncomeAmount;

    @ElementCollection
    @CollectionTable(name = "case_additional_files1", joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "file_name")
    private List<String> adds;

    @OneToMany(mappedBy = "caseEntity", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CaseFile> caseFiles;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (uploadDate == null) {
            uploadDate = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        updateDate = LocalDate.now();
    }
}
