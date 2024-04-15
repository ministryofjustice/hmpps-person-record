package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt

class AuthAwareTokenConverter : Converter<Jwt, AbstractAuthenticationToken> {

  override fun convert(jwt: Jwt): AbstractAuthenticationToken {
    val clientId: Any? = jwt.claims["client_id"]
    val clientOnly: Boolean = jwt.subject.equals(clientId)
    return AuthAwareAuthenticationToken(jwt, clientOnly, extractAuthorities(jwt))
  }

  private fun extractAuthorities(jwt: Jwt): Collection<GrantedAuthority> {
    @Suppress("UNCHECKED_CAST")
    val authorities = jwt.claims.getOrDefault("authorities", emptyList<String>()) as Collection<String>
    return authorities.map { SimpleGrantedAuthority(it) }
  }
}
