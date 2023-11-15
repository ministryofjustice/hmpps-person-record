package uk.gov.justice.digital.hmpps.personrecord.security

import io.jsonwebtoken.Jwts
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.stereotype.Component
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.util.*

@Component
class JwtAuthHelper {
  private var keyPair: KeyPair? = null

  init {
    val gen = KeyPairGenerator.getInstance("RSA")
    gen.initialize(2048)
    keyPair = gen.generateKeyPair()
  }

  @Bean
  fun jwtDecoder(): JwtDecoder = NimbusJwtDecoder.withPublicKey(keyPair?.public as RSAPublicKey).build()

  fun createJwt(
    subject: String,
    scope: List<String>? = listOf(),
    roles: List<String>? = listOf(),
    expiryTime: Duration = Duration.ofHours(1),
    jwtId: String = UUID.randomUUID().toString(),
  ): String {
    val claims = HashMap<String, Any>()
    claims["user_name"] = subject
    claims["client_id"] = "hmpps-person-record"
    if (!roles.isNullOrEmpty()) claims["authorities"] = roles
    if (!scope.isNullOrEmpty()) claims["scope"] = scope
    return Jwts.builder()
      .id(jwtId)
      .subject(subject)
      .claims(claims)
      .expiration(Date(System.currentTimeMillis() + expiryTime.toMillis()))
      .signWith(keyPair?.private, Jwts.SIG.RS256)
      .compact()
  }
}
