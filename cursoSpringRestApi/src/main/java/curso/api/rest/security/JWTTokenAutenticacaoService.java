package curso.api.rest.security;

import java.io.IOException;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import curso.api.rest.ApplicationContextLoad;
import curso.api.rest.model.Usuario;
import curso.api.rest.repository.UsuarioRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Service
@Component
public class JWTTokenAutenticacaoService {
	
	// Tempo de validade do Token 2 dias
	private static final long EXPIRATION_TIME = 1728000000;
	
	// Uma senha única para compor a autentificação e ajudar na segurança
	private static final String SECRET = "SenhaExtremamenteSecreta";
	
	// Prefixo padrão de token
	private static final String TOKE_PREFIX = "Bearer";
	
	private static final String HEADER_STRING = "Authorization";
	
	// Gerando token de autentificação e adicionando ao cabeçalho e resposta Http
	public void addAuthentication(HttpServletResponse response, String username) throws IOException {
		
		// Montagem do token
		String JWT = Jwts.builder() //  Chama o gerador de token 
				.setSubject(username) // Adiciona o usuário
				.setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // Tempo de expiração
				.signWith(SignatureAlgorithm.HS512, SECRET).compact(); // Compactação e algoritimos de geração de senha
		// Junta o toke com o prefixo
		String token = TOKE_PREFIX + " " + JWT; // Bearer
		// Adiciona no cabeçalho http
		response.addHeader(HEADER_STRING, token); // Authorization: Bearer
		// Liberando resposta para portas diferentes que usam a API ou clientes WEB
		liberacaoCors(response);
		// Escreve token como resposta no corpo http
		response.getWriter().write("{\"Authorizario\": \""+token+"\"}");
	}
	
	// Retorna o usuário validado com token ou caso não seja válido retorna null
	public Authentication getAuthentication(HttpServletRequest request, HttpServletResponse response) {

		// Pega o token enviado no cabeçalho http
		String token = request.getHeader(HEADER_STRING);
		
		try {
		if(token != null) {
			
			String tokenLimpo = token.replace(TOKE_PREFIX, "").trim();
			// Faz a validação do token do usuário na requisição
			String user = Jwts.parser().setSigningKey(SECRET)
						  .parseClaimsJws(tokenLimpo)
						  .getBody().getSubject(); 
			
			if(user != null) {
				Usuario usuario = ApplicationContextLoad.getApplicationContext()
						.getBean(UsuarioRepository.class).findUserByLogin(user);
				if(usuario != null) {
					if(tokenLimpo.equalsIgnoreCase(usuario.getToken())) {
					return new UsernamePasswordAuthenticationToken(
							usuario.getLogin(), 
							usuario.getSenha(),
							usuario.getAuthorities());
					}
				}
			}
		} // Fim da condição token
	}catch (io.jsonwebtoken.ExpiredJwtException e) {
		try {
			response.getOutputStream().println("Seu TOKEN foi expirado, faça o login ou informe um novo TOKEN para AUTENTICAÇÃO");
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
		liberacaoCors(response);
		return null; // Não autorizado
		
	}

	private void liberacaoCors(HttpServletResponse response) {
		if(response.getHeader("Acess-Control-Allow-Origin") == null) {
			response.addHeader("Acess-Control-Allow-Origin", "*");
		}
		if(response.getHeader("Acess-Control-Allow-Headers") == null) {
			response.addHeader("Acess-Control-Allow-Headers", "*");
		}
		if(response.getHeader("Acess-Control-Allow-Headers") == null) {
			response.addHeader("Acess-Control-Allow-Headers", "*");
		}
		if(response.getHeader("Acess-Control-Allow-Methods") == null) {
			response.addHeader("Acess-Control-Allow-Methods","*");
		}
	}
}
