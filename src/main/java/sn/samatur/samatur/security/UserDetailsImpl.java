package sn.samatur.samatur.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import sn.samatur.samatur.security.entity.Utilisateur; // Ajuste l'import selon l'emplacement de ton entité Utilisateur

import java.util.Collection;
import java.util.List;

public class UserDetailsImpl implements UserDetails {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username; // Correspond à l'identifiant (email/login)
    
    @JsonIgnore
    private String password;

    private Collection<? extends GrantedAuthority> authorities;

    /** Compte actif : un compte désactivé ne peut pas se connecter (RG-15). */
    private boolean actif = true;

    /** Rôle et structure de rattachement, lus en base et jamais dans la requête (RG-16). */
    private String role;
    private Long ecoleId;
    private Long centreEtatCivilId;
    private Long tribunalInstanceId;

    public UserDetailsImpl(Long id, String username, String password,
                           Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
    }

    public static UserDetailsImpl build(Utilisateur utilisateur) {
        List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_" + utilisateur.getRole())
        );

        UserDetailsImpl details = new UserDetailsImpl(
                utilisateur.getId(),
                utilisateur.getIdentifiant(),
                utilisateur.getMotDePasseHash(),
                authorities);
        details.actif = Boolean.TRUE.equals(utilisateur.getActif());
        details.role = utilisateur.getRole();
        details.ecoleId = utilisateur.getEcoleId();
        details.centreEtatCivilId = utilisateur.getCentreEtatCivilId();
        details.tribunalInstanceId = utilisateur.getTribunalInstanceId();
        return details;
    }

    public Long getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public Long getEcoleId() {
        return ecoleId;
    }

    public Long getCentreEtatCivilId() {
        return centreEtatCivilId;
    }

    public Long getTribunalInstanceId() {
        return tribunalInstanceId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return actif; }
}