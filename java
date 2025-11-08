// =============================
// PART (A): SPRING DEPENDENCY INJECTION (JAVA CONFIG)
// =============================

package com.example;

import org.springframework.context.annotation.*;

class Course {
    private String courseName;
    public Course(String courseName) { this.courseName = courseName; }
    public String getCourseName() { return courseName; }
}

class Student {
    private Course course;
    public Student(Course course) { this.course = course; }
    public void displayInfo() { System.out.println("Enrolled in: " + course.getCourseName()); }
}

@Configuration
class DIConfig {
    @Bean
    public Course course() { return new Course("Spring Framework"); }
    @Bean
    public Student student() { return new Student(course()); }
}

class DIApp {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(DIConfig.class);
        Student s = context.getBean(Student.class);
        s.displayInfo();
        context.close();
    }
}


// =============================
// PART (B): HIBERNATE CRUD APPLICATION
// =============================

package com.example;

import javax.persistence.*;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import java.util.List;

@Entity
@Table(name = "student")
class StudentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String name;
    private int age;
    public StudentEntity() {}
    public StudentEntity(String name, int age) { this.name = name; this.age = age; }
    public int getId() { return id; }
    public String getName() { return name; }
    public int getAge() { return age; }
    public void setName(String name) { this.name = name; }
    public void setAge(int age) { this.age = age; }
}

class HibernateCRUD {
    public static void main(String[] args) {
        SessionFactory factory = new Configuration().configure("hibernate.cfg.xml").addAnnotatedClass(StudentEntity.class).buildSessionFactory();
        Session session = factory.openSession();
        Transaction tx = session.beginTransaction();

        StudentEntity s1 = new StudentEntity("John", 22);
        session.save(s1);

        StudentEntity s2 = session.get(StudentEntity.class, 1);
        if (s2 != null) { s2.setName("John Updated"); session.update(s2); }

        List<StudentEntity> list = session.createQuery("from StudentEntity", StudentEntity.class).list();
        for (StudentEntity s : list) System.out.println(s.getId() + " " + s.getName() + " " + s.getAge());

        StudentEntity s3 = session.get(StudentEntity.class, 2);
        if (s3 != null) session.delete(s3);

        tx.commit();
        session.close();
        factory.close();
    }
}


// =============================
// PART (C): SPRING + HIBERNATE TRANSACTION MANAGEMENT
// =============================

package com.example;

import javax.persistence.*;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import java.util.Properties;
import javax.sql.DataSource;

@Entity
@Table(name = "account")
class Account {
    @Id
    private int accId;
    private String holder;
    private double balance;
    public int getAccId() { return accId; }
    public void setAccId(int accId) { this.accId = accId; }
    public String getHolder() { return holder; }
    public void setHolder(String holder) { this.holder = holder; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}

@Repository
class AccountDAO {
    @Autowired
    private SessionFactory sessionFactory;
    public Account getAccount(int id) { return sessionFactory.getCurrentSession().get(Account.class, id); }
    public void updateAccount(Account acc) { sessionFactory.getCurrentSession().update(acc); }
}

@Service
class BankService {
    @Autowired
    private AccountDAO dao;
    @Transactional
    public void transferMoney(int fromId, int toId, double amount) {
        Account from = dao.getAccount(fromId);
        Account to = dao.getAccount(toId);
        from.setBalance(from.getBalance() - amount);
        to.setBalance(to.getBalance() + amount);
        dao.updateAccount(from);
        dao.updateAccount(to);
    }
}

@Configuration
@ComponentScan("com.example")
@EnableTransactionManagement
class HibernateSpringConfig {
    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
        ds.setUrl("jdbc:mysql://localhost:3306/testdb");
        ds.setUsername("root");
        ds.setPassword("password");
        return ds;
    }
    @Bean
    public SessionFactory sessionFactory() {
        org.hibernate.cfg.Configuration cfg = new org.hibernate.cfg.Configuration();
        cfg.addAnnotatedClass(Account.class);
        cfg.setProperties(hibernateProperties());
        return cfg.buildSessionFactory();
    }
    private Properties hibernateProperties() {
        Properties p = new Properties();
        p.put("hibernate.dialect", "org.hibernate.dialect.MySQL8Dialect");
        p.put("hibernate.hbm2ddl.auto", "update");
        return p;
    }
    @Bean
    public HibernateTransactionManager transactionManager(SessionFactory sf) {
        return new HibernateTransactionManager(sf);
    }
}

class TransactionApp {
    public static void main(String[] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(HibernateSpringConfig.class);
        BankService service = context.getBean(BankService.class);
        service.transferMoney(1, 2, 1000);
        System.out.println("Transfer successful");
        context.close();
    }
}
