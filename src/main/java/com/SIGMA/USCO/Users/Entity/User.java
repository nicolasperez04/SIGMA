package com.SIGMA.USCO.Users.Entity;

import com.SIGMA.USCO.Users.Entity.enums.ProgramRole;
import com.SIGMA.USCO.Users.Entity.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String name;

    @NotNull
    private String lastName;

    @NotNull
    @Column(unique = true)
    private String email;

    @NotNull
    private String password;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;


    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDateTime creationDate;

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
