package utf.servidor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.JsonObject;

public class BancoDeDados {
    // Utilizando Hash seguro para Multi-Thread
    private Map<String, Perfil> usuarios = new ConcurrentHashMap<>();

    public BancoDeDados() {
        usuarios.put("admin", new Perfil("Administrador", "admin", "123456", "ADMIN"));
    }

    private boolean isUsuarioValido(String usuario){
        return usuario != null && usuario.matches("^[a-zA-Z0-9]{5,20}$");
    }

    private boolean isSenhaValida(String senha){
        return senha != null && senha.matches("^[0-9]{6}$");
    }

    public boolean cadastrarUsuario(String nome, String usuario, String senha){
        if (nome == null || nome.trim().isEmpty() || usuario == null || senha == null) 
        	return false;
        if (!isUsuarioValido(usuario) || !isSenhaValida(senha) || usuarios.containsKey(usuario)) 
        	return false;
        
        Perfil novo = new Perfil(nome, usuario, senha, "USER");
        usuarios.put(usuario, novo);
        return true;
    }

    public Perfil fazerLogin(String usuario, String senha, String ip) {
        Perfil p = usuarios.get(usuario);
        if (p != null && p.getSenha().equals(senha)) {
            if (p.getNivel().equals("ADMIN")) {
                p.setToken("adm");
            } else {
                p.setToken("usr_" + p.getUsuario());
            }
            p.setIp(ip); // Salva o IP como dita o protocolo
            return p;
        }
        return null;
    }

    public boolean fazerLogout(String token){
        Perfil p = buscarPerfilPorToken(token);
        if(p != null) {
            p.limparSessao();
            return true;
        }
        return false;
    }

    public String buscarUsuarioPorToken(String token){
        Perfil p = buscarPerfilPorToken(token);
        return (p != null) ? p.getUsuario() : null;
    }

    public Perfil buscarPerfilPorToken(String token){
        if(token == null || token.isEmpty()) return null;
        for(Perfil p : usuarios.values()){
            if(token.equals(p.getToken())) return p;
        }
        return null;
    }

    public Perfil buscarPerfil(String usuario){
        return usuarios.get(usuario);
    }

    public boolean atualizarUsuario(String usuario, String novoNome, String novaSenha){
        Perfil p = usuarios.get(usuario);
        if(p != null){
            if(novoNome == null || novoNome.trim().isEmpty() || !isSenhaValida(novaSenha)) return false;
            p.setNome(novoNome);
            p.setSenha(novaSenha);
            return true;
        }
        return false;
    }

    public boolean apagarUsuario(String loginAlvo){
        Perfil p = usuarios.get(loginAlvo);
        if(p != null && !p.getNivel().equals("ADMIN")){
            usuarios.remove(loginAlvo);
            return true;
        }
        return false;
    }

    public List<JsonObject> listarUsuariosParaAdmin(){
        List<JsonObject> lista = new ArrayList<>();
        for(Perfil p : usuarios.values()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("usuario", p.getUsuario());
            obj.addProperty("nome", p.getNome());
            lista.add(obj);
        }
        return lista;
    }

    public boolean atualizarUsuarioAdmin(String usuarioAlvo, String novoNome, String novaSenha){
        Perfil p = usuarios.get(usuarioAlvo);
        if(p != null){
            if(novoNome != null && !novoNome.trim().isEmpty()){
                p.setNome(novoNome);
            }
            if(novaSenha != null && !novaSenha.trim().isEmpty()){
                if(isSenhaValida(novaSenha)){
                    p.setSenha(novaSenha);
                }else{
                    return false; 
                }
            }
            return true;
        }
        return false;
    }

    public boolean apagarUsuarioAdmin(String loginAlvo){
        if (loginAlvo == null || loginAlvo.equals("admin")) return false;
        if (usuarios.containsKey(loginAlvo)) {
            usuarios.remove(loginAlvo);
            return true;
        }
        return false;
    }
}