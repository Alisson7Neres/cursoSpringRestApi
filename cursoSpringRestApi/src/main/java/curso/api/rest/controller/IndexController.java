package curso.api.rest.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

import curso.api.rest.model.Usuario;
import curso.api.rest.model.UsuarioDTO;
import curso.api.rest.repository.UsuarioRepository;

@CrossOrigin(origins = "*")
@RestController // Arquitetura REST
@RequestMapping(value = "/usuario")
public class IndexController {
	
	@Autowired
	private UsuarioRepository usuarioRepository;
	
	// Serviço RESTFul
	
	@GetMapping(value = "/{id}", produces = "application/json", headers = "X-API-Version=v1")
	public ResponseEntity<UsuarioDTO> initV1(@PathVariable(value = "id") Long id) {
		
		Optional<Usuario> usuario = usuarioRepository.findById(id);
		
		return new ResponseEntity<UsuarioDTO>(new UsuarioDTO(usuario.get()), HttpStatus.OK);
	}
	
	@GetMapping(value = "/{id}", produces = "application/json", headers = "X-API-Version=v2")
	public ResponseEntity<UsuarioDTO> initV2(@PathVariable(value = "id") Long id) {
		
		Optional<Usuario> usuario = usuarioRepository.findById(id);
		
		return new ResponseEntity<UsuarioDTO> ( new UsuarioDTO(usuario.get()), HttpStatus.OK);
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@GetMapping(value = "/codigo", produces = "application/json")
	public ResponseEntity codigo() {
		return new ResponseEntity("Teste", HttpStatus.OK);
	}
	
	@GetMapping(value = "/", produces = "application/json")
	@CacheEvict(value = "cacheusuarios", allEntries = true)
	@CachePut("cacheusuarios")
	public ResponseEntity<List<Usuario>> usuarioLista() throws InterruptedException{
		
		List<Usuario> list = (List<Usuario>) usuarioRepository.findAll();
		
		return new ResponseEntity<List<Usuario>>(list, HttpStatus.OK);
	}
	
	@PostMapping(value = "/", produces = "application/json")
	public ResponseEntity<Usuario> cadastrar(@RequestBody Usuario usuario) throws IOException{
		
		for(int pos = 0; pos < usuario.getTelefones().size(); pos++) {
			usuario.getTelefones().get(pos).setUsuario(usuario);
		}
		
		// Consumindo API pública externa
			
			URL url = new URL("https://viacep.com.br/ws/"+usuario.getCep()+"/json/");
			URLConnection connection = url.openConnection();
			InputStream is = connection.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
			
			String cep = "";
			StringBuilder jsonCEP = new StringBuilder();
			
			while((cep = br.readLine()) != null) {
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
		// Fim da API externa
		
		String senhaCriptografada = new BCryptPasswordEncoder().encode(usuario.getSenha());
		usuario.setSenha(senhaCriptografada);
		Usuario usuarioSalvo =  usuarioRepository.save(usuario);
		return new ResponseEntity<Usuario>(usuarioSalvo, HttpStatus.OK);
	}
	
	@PutMapping(value = "/", produces = "application/json")
	public ResponseEntity<Usuario> atualizar(@RequestBody Usuario usuario){
		
		for(int pos = 0; pos < usuario.getTelefones().size(); pos++) {
			usuario.getTelefones().get(pos).setUsuario(usuario);
		}
		
		Usuario userTemp = usuarioRepository.findUserByLogin(usuario.getLogin());
		
		//Senhas diferentes
		if(!userTemp.getSenha().equals(usuario.getSenha())) {
			String senhaCriptografada = new BCryptPasswordEncoder().encode(usuario.getSenha());
			usuario.setSenha(senhaCriptografada);
		}
		Usuario usuarioAtualizar = usuarioRepository.save(usuario);
		
		return new ResponseEntity<Usuario>(usuarioAtualizar, HttpStatus.OK);
	}
	
	@DeleteMapping(value = "/{id}", produces = "application/json")
	public ResponseEntity<Usuario> deletar(@RequestBody Usuario usuario,
			@PathVariable(value = "id") Long id){
		
		usuarioRepository.deleteById(id);
		
		return null;
	}
}
