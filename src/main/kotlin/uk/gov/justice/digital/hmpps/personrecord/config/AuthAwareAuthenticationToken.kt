package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken

class AuthAwareAuthenticationToken(jwt: Jwt, clientOnly: Boolean, authorities: Collection<GrantedAuthority?>?) :
  JwtAuthenticationToken(jwt, authorities) {

  private val subject: String
  private val clientOnly: Boolean

  init {
    subject = jwt.subject
    this.clientOnly = clientOnly
  }
}
