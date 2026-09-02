package org.tinycloud.jdbc.verify;

import org.tinycloud.jdbc.annotation.Column;
import org.tinycloud.jdbc.annotation.Id;
import org.tinycloud.jdbc.annotation.IdType;
import org.tinycloud.jdbc.annotation.Table;

@Table("t_batch_verify")
public class BatchVerifyEntity {

    @Id(idType = IdType.AUTO_INCREMENT)
    @Column("id")
    private Long id;

    @Column("name")
    private String name;

    @Column("age")
    private Integer age;

    @Column("email")
    private String email;

    @Column(exist = false)
    private String ignoreMe;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getIgnoreMe() {
        return ignoreMe;
    }

    public void setIgnoreMe(String ignoreMe) {
        this.ignoreMe = ignoreMe;
    }
}
