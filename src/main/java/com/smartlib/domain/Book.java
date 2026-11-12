package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "book", indexes = @Index(columnList = "library_id"))
public class Book extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Library library;
    @Column(nullable = false) private String title;
    private String author;
    private String isbn;
    private String category;
    private String shelf;
    private int totalCopies = 1;
    private int availableCopies = 1;

    public Library getLibrary() { return library; }
    public void setLibrary(Library library) { this.library = library; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getShelf() { return shelf; }
    public void setShelf(String shelf) { this.shelf = shelf; }

    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }

    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }
}
