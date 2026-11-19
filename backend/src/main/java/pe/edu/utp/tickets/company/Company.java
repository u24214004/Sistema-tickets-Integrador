package pe.edu.utp.tickets.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 11)
    private String ruc;

    @Column(nullable = false, length = 150)
    private String name;

    protected Company() {
    }

    public Company(String ruc, String name) {
        this.ruc = ruc;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getRuc() {
        return ruc;
    }

    public String getName() {
        return name;
    }
}
