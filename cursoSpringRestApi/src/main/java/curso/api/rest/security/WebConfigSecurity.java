package curso.api.rest.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

import curso.api.rest.services.ImplementacaoUserDetailsService;

// Mapeia URl, enderecos, autoriza ou bloqueia acessos a URL
@Configuration
@EnableWebSecurity
public class WebConfigSecurity extends WebSecurityConfigurerAdapter {

	@Autowired
	private ImplementacaoUserDetailsService implementacaoUserDetailsService;

	// Configura as solicitações de acesso por Http
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		// Ativando a proteção contra usuário que nãp estão validados por token
		http.csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
		// Ativando a permissão para acessoa página inicial do sistema EX: sistemas.com.br/*/
		.disable().authorizeRequests().antMatchers("/").permitAll()
		.antMatchers("/index").permitAll()
		// URl de Logout - Redireciona após o user deslogar do sistema
		.anyRequest().authenticated().and().logout().logoutSuccessUrl("/index")
		// Mapeia URL de Logout e invalida o usuário
		.logoutRequestMatcher(new AntPathRequestMatcher("/logout"));
		// Filtra requisições de login para autentificação
		
		// Filtra demais requisições para verificar a presença do TOKEN JWT no HEADER HTTP
		
		}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		// Service que irá consultar o usuário no banco de dados
		auth.userDetailsService(implementacaoUserDetailsService)
				// Padrão de codificação de senha
				.passwordEncoder(new BCryptPasswordEncoder());
	}
}
