package com.SIGMA.USCO.Users.entity;

import com.SIGMA.USCO.Users.entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.entity.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotNull
    @ToString.Include
    private String name;

    @NotNull
    @ToString.Include
    private String lastName;

    @NotNull
    @Column(unique = true)
    @ToString.Include
    private String email;

    @NotNull
    private String password;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;


    @Enumerated(EnumType.STRING)
    @ToString.Include
    private Status status;

    @ToString.Include
    private LocalDateTime creationDate;

    @ToString.Include
    private LocalDateTime lastUpdateDate;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .flatMap(role -> {

                    Stream<GrantedAuthority> roleAuthority = Stream.of(
                            new SimpleGrantedAuthority("ROLE_" + role.getName())
                    );


                    Stream<GrantedAuthority> permissionAuthorities =
                            role.getPermissions()
                                    .stream()
                                    .map(permission -> new SimpleGrantedAuthority("PERM_" + permission.getName()));

                    return Stream.concat(roleAuthority, permissionAuthorities);
                })
                .collect(Collectors.toSet());
    }



    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.status == Status.ACTIVE;
    }
}
