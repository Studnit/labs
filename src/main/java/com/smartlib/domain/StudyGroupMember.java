package com.smartlib.domain;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name = "study_group_member", uniqueConstraints = @UniqueConstraint(columnNames = {"group_id", "user_id"}))
public class StudyGroupMember extends BaseEntity {
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private StudyGroup group;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private AppUser user;

    public StudyGroup getGroup() { return group; }
    public void setGroup(StudyGroup group) { this.group = group; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }
}
