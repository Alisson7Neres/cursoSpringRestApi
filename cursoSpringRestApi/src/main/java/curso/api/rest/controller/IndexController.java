package curso.api.rest.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;

import curso.api.rest.model.Telefone;
import curso.api.rest.model.UserChart;
import curso.api.rest.model.UserReport;
import curso.api.rest.model.Usuario;
import curso.api.rest.repository.TelefoneRepository;
import curso.api.rest.repository.UsuarioRepository;
import curso.api.rest.services.ImplementacaoUserDetailsService;
import curso.api.rest.services.ServiceRelatorio;

@CrossOrigin(origins = "*")
@RestController // Arquitetura REST
@RequestMapping(value = "/usuario")
public class IndexController {

	@Autowired
	private UsuarioRepository usuarioRepository;
	
	@Autowired
	private TelefoneRepository telefoneRepository;
	
	@Autowired
	private ImplementacaoUserDetailsService implementacaoUserDetailsService;
	
	@Autowired
	private ServiceRelatorio serviceRelatorio;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;

	// Servi??o RESTFul

	@GetMapping(value = "/{id}", produces = "application/json")
	public ResponseEntity<Usuario> initV2(@PathVariable(value = "id") Long id) {

		Optional<Usuario> usuario = usuarioRepository.findById(id);

		return new ResponseEntity<Usuario>(usuario.get(), HttpStatus.OK);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@GetMapping(value = "/codigo", produces = "application/json")
	public ResponseEntity codigo() {
		return new ResponseEntity("Teste", HttpStatus.OK);
	}

	@GetMapping(value = "/", produces = "application/json")
	@CacheEvict(value = "cacheusuarios", allEntries = true)
	@CachePut("cacheusuarios")
	public ResponseEntity<Page<Usuario>> usuarioLista() throws InterruptedException {

		// Criando p??gina????o. 
		PageRequest page = PageRequest.of(0, 5, Sort.by("nome"));
		Page<Usuario> list = usuarioRepository.findAll(page);

		return new ResponseEntity<Page<Usuario>>(list, HttpStatus.OK);
	}
	
	@GetMapping(value = "/page/{pagina}", produces = "application/json")
	@CacheEvict(value = "cacheusuarios", allEntries = true)
	@CachePut("cacheusuarios")
	public ResponseEntity<Page<Usuario>> usuarioPagina(@PathVariable("pagina") int pagina) throws InterruptedException {

		// Criando p??gina????o. 
		PageRequest page = PageRequest.of(pagina, 5, Sort.by("nome"));
		Page<Usuario> list = usuarioRepository.findAll(page);

		return new ResponseEntity<Page<Usuario>>(list, HttpStatus.OK);
	}

	@PostMapping(value = "/", produces = "application/json")
	public ResponseEntity<Usuario> cadastrar(@RequestBody Usuario usuario) throws IOException {

		for (int pos = 0; pos < usuario.getTelefones().size(); pos++) {
			usuario.getTelefones().get(pos).setUsuario(usuario);
		}

		// Consumindo API p??blica externa

		if(usuario.getCep() != null && usuario.getCep().trim() != null) {
			
		URL url = new URL("https://viacep.com.br/ws/" + usuario.getCep() + "/json/");
		URLConnection connection = url.openConnection();
		InputStream is = connection.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

		String cep = "";
		StringBuilder jsonCEP = new StringBuilder();

		while ((cep = br.readLine()) != null) {
			jsonCEP.append(cep);
		}

		System.out.println(jsonCEP.toString());

		Usuario userAux = new Gson().fromJson(jsonCEP.toString(), Usuario.class);

		usuario.setCep(userAux.getCep());
		usuario.setLogradouro(userAux.getLogradouro());
		usuario.setComplemento(userAux.getComplemento());
		usuario.setBairro(userAux.getBairro());
		usuario.setLocalidade(userAux.getLocalidade());
		usuario.setUf(userAux.getUf());
		}
		// Fim da API externa

		String senhaCriptografada = new BCryptPasswordEncoder().encode(usuario.getSenha());
		usuario.setSenha(senhaCriptografada);
		Usuario usuarioSalvo = usuarioRepository.save(usuario);
		
		implementacaoUserDetailsService.inserirAcessoPadrao(usuario.getId());
		
		return new ResponseEntity<Usuario>(usuarioSalvo, HttpStatus.OK);
	}

	@PutMapping(value = "/", produces = "application/json")
	@CachePut("cacheuser")
	public ResponseEntity<Usuario> atualizar(@RequestBody Usuario usuario) {

		for (int pos = 0; pos < usuario.getTelefones().size(); pos++) {
			usuario.getTelefones().get(pos).setUsuario(usuario);
		}

		Usuario userTemp = usuarioRepository.findById(usuario.getId()).get();

		// Senhas diferentes
		if (!userTemp.getSenha().equals(usuario.getSenha())) {
			String senhaCriptografada = new BCryptPasswordEncoder().encode(usuario.getSenha());
			usuario.setSenha(senhaCriptografada);
		}
		Usuario usuarioAtualizar = usuarioRepository.save(usuario);

		return new ResponseEntity<Usuario>(usuarioAtualizar, HttpStatus.OK);
	}
	
	@PostMapping(value = "/telefone")
	public ResponseEntity<Telefone> salvarTelefone(@RequestBody Telefone telefone) {
		if(telefone.getNumero() != null && telefone.getNumero().trim() != null) {
		telefoneRepository.save(telefone);
		
		return new ResponseEntity<Telefone>(HttpStatus.OK);
		}else {
			return new ResponseEntity<Telefone>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@DeleteMapping(value = "/{id}", produces = "application/json")
	public String deletar(@PathVariable("id") Long id) {

		usuarioRepository.deleteById(id);

		return "ok";
	}
	
	@DeleteMapping(value = "/telefone/{id}", produces = "application/json")
	public String deletarTelefone(@PathVariable("id") Long id) {

		telefoneRepository.deleteById(id);

		return "ok";
	}

	@GetMapping(value = "/usuarioPorNome/{nome}", produces="application/json")
	@CachePut("cacheusuarios")
	public ResponseEntity<Page<Usuario>> usuarioPorNome(@PathVariable("nome") String nome) throws InterruptedException {
		
		PageRequest pageRequest = null;
		Page<Usuario> list = null;
		
		// N??o informou o nome
		if(nome == null || (nome != null && nome.trim().isEmpty())
				|| nome.equalsIgnoreCase("undefined")) {
			pageRequest = PageRequest.of(0, 5, Sort.by("nome"));
			list = usuarioRepository.findAll(pageRequest);
		}else {
			pageRequest = PageRequest.of(0, 5, Sort.by("nome"));
			list = usuarioRepository.findUserByNamePage(nome, pageRequest);
		}
		
		return new ResponseEntity<Page<Usuario>>(list, HttpStatus.OK);
	}
	
	@GetMapping(value = "/usuarioPorNome/{nome}/page/{page}", produces="application/json")
	@CachePut("cacheusuarios")
	public ResponseEntity<Page<Usuario>> usuarioPorNomePage(@PathVariable("nome") String nome ,
			@PathVariable(name = "page") int page) throws InterruptedException {
		
		PageRequest pageRequest = null;
		Page<Usuario> list = null;
		
		// N??o informou o nome
		if(nome == null || (nome != null && nome.trim().isEmpty())
				|| nome.equalsIgnoreCase("undefined")) {
			pageRequest = PageRequest.of(page, 5, Sort.by("nome"));
			list = usuarioRepository.findAll(pageRequest);
		}else {
			pageRequest = PageRequest.of(page, 5, Sort.by("nome"));
			list = usuarioRepository.findUserByNamePage(nome, pageRequest);
		}
		
		return new ResponseEntity<Page<Usuario>>(list, HttpStatus.OK);
	}
	
	@SuppressWarnings("all")
	@GetMapping(value = "/relatorio", produces="application/text")
	public ResponseEntity<String> downloadRelatorio(HttpServletRequest request) throws Exception {
		byte[] pdf = serviceRelatorio.gerarRelatorio("relatorio-usuario",
				new HashMap(),  request.getServletContext());
		
		String base64Pdf = "data:application/pdf;base64," + Base64.encodeBase64String(pdf);
		return new ResponseEntity<String>(base64Pdf, HttpStatus.OK);
		
	}
	
	@PostMapping(value = "/relatorio/", produces="application/text")
	public ResponseEntity<String> downloadRelatorioParam(HttpServletRequest request, 
			@RequestBody UserReport userReport) throws Exception {
		

		String dataInicio = userReport.getDataInicio();
		
		String dataFim =userReport.getDataFim();
		
		Map<String, Object> params = new HashMap<>();
		
		params.put("DATA_INICIO", dataInicio);
		params.put("DATA_FIM", dataFim);
		
		byte[] pdf = serviceRelatorio.gerarRelatorio("relatorio-usuario-param", 
				params, request.getServletContext());
		
		String base64Pdf = "data:application/pdf;base64," + Base64.encodeBase64String(pdf);
		return new ResponseEntity<>(base64Pdf, HttpStatus.OK);
		
	}
	
	@GetMapping(value = "/grafico", produces = "application/json")
	public ResponseEntity<UserChart> grafico(){
		
		UserChart userChart = new UserChart();
		
		List<String> resultado = jdbcTemplate.queryForList(
				"select array_agg(nome) from usuario where salario > 0 union all select cast(array_agg(salario)as character varying[]) from usuario where salario > 0;",
				String.class);
		
		if(!resultado.isEmpty()) {
			String nomes = resultado.get(0).replaceAll("\\{", "").replaceAll("\\}", "");
			String salario = resultado.get(1).replaceAll("\\{", "").replaceAll("\\}", "");
			
			userChart.setNome(nomes);
			userChart.setSalario(salario);
		}
		
		return new ResponseEntity<UserChart>(userChart, HttpStatus.OK);
	}
}
