package com.example.campusresources.config;

import com.example.campusresources.entity.Category;
import com.example.campusresources.entity.Resource;
import com.example.campusresources.repository.CategoryRepository;
import com.example.campusresources.repository.ResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Inserts deterministic sample data (10 categories, 800 resources) ONLY when the database is empty,
 * so restarting the application never duplicates data. Not used by the "test" profile.
 */
@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final int RESOURCE_COUNT = 800;

    private static final String[][] CATEGORIES = {
            {"Programming", "Programming languages and software development"},
            {"Databases", "Relational and NoSQL database concepts"},
            {"Algorithms", "Algorithms and data structures"},
            {"Web Development", "Frontend, backend and web frameworks"},
            {"Operating Systems", "Processes, memory, scheduling and file systems"},
            {"Computer Networks", "Protocols, routing and network security basics"},
            {"Software Engineering", "Design, testing and project practices"},
            {"Artificial Intelligence", "Machine learning and intelligent systems"},
            {"Cyber Security", "Defensive security and safe coding"},
            {"Mathematics", "Discrete maths, statistics and linear algebra"}
    };

    /** Five topics for each category, in the same order as CATEGORIES. */
    private static final String[][] TOPICS = {
            {"Java Programming", "Python Basics", "C++ Essentials", "Spring Boot", "Clean Code"},
            {"MySQL", "SQL Joins", "Database Design", "Indexing and Query Tuning", "NoSQL Concepts"},
            {"Sorting Algorithms", "Graph Algorithms", "Dynamic Programming", "Trees and Heaps", "Hashing"},
            {"HTML and CSS", "JavaScript", "REST API Design", "React Fundamentals", "Web Accessibility"},
            {"Process Scheduling", "Memory Management", "File Systems", "Deadlocks", "Linux Command Line"},
            {"TCP/IP", "HTTP and HTTPS", "Routing", "DNS", "Network Security"},
            {"Agile Methods", "Unit Testing", "UML Modelling", "Version Control with Git", "Software Architecture"},
            {"Machine Learning", "Neural Networks", "Natural Language Processing", "Data Preprocessing", "AI Ethics"},
            {"Cryptography", "Secure Coding", "Web Application Security", "Authentication Basics", "Incident Response"},
            {"Discrete Mathematics", "Probability", "Linear Algebra", "Statistics", "Number Theory"}
    };

    private static final String[] FORMS = {
            "Handbook", "Study Guide", "Quick Reference", "Practical Tutorial",
            "Complete Course", "Exam Notes", "Lab Manual", "Cheat Sheet"
    };
    private static final String[] TYPES = {
            "BOOK", "STUDY_GUIDE", "REFERENCE", "TUTORIAL",
            "TUTORIAL", "STUDY_GUIDE", "BOOK", "REFERENCE"
    };
    private static final String[] AUTHORS = {
            "A. Sharma", "R. Singh", "K. Kaur", "M. Gupta", "S. Verma", "P. Mehta",
            "N. Joshi", "L. Brown", "T. Nguyen", "C. Garcia", "H. Ali", "D. Kumar"
    };

    private final CategoryRepository categoryRepository;
    private final ResourceRepository resourceRepository;

    public DataInitializer(CategoryRepository categoryRepository, ResourceRepository resourceRepository) {
        this.categoryRepository = categoryRepository;
        this.resourceRepository = resourceRepository;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() > 0 || resourceRepository.count() > 0) {
            log.info("Database already contains data - sample data NOT inserted");
            return;
        }

        List<Category> categories = new ArrayList<>();
        for (String[] c : CATEGORIES) {
            categories.add(new Category(c[0], c[1]));
        }
        categories = categoryRepository.saveAll(categories);

        LocalDateTime base = LocalDateTime.of(2025, 1, 1, 8, 0);
        List<Resource> batch = new ArrayList<>();
        for (int i = 0; i < RESOURCE_COUNT; i++) {
            int categoryIndex = i % CATEGORIES.length;
            String topic = TOPICS[categoryIndex][(i / CATEGORIES.length) % 5];
            int formIndex = (i / 50) % FORMS.length;

            Resource r = new Resource();
            r.setTitle(topic + " " + FORMS[formIndex] + " #" + (i + 1));
            r.setDescription("A " + FORMS[formIndex].toLowerCase() + " covering " + topic.toLowerCase()
                    + " for undergraduate students.");
            r.setAuthor(AUTHORS[i % AUTHORS.length]);
            r.setResourceType(TYPES[formIndex]);
            r.setRating((10 + (i * 37) % 41) / 10.0);      // exact tenths, always between 1.0 and 5.0
            r.setCategory(categories.get(categoryIndex));
            r.setCreatedAt(base.plusHours(i * 5L));
            r.setUpdatedAt(r.getCreatedAt());
            batch.add(r);

            if (batch.size() == 200) {
                resourceRepository.saveAll(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            resourceRepository.saveAll(batch);
        }
        log.info("Inserted {} categories and {} sample resources", categories.size(), RESOURCE_COUNT);
    }
}
