package utf.servidor;

import java.util.HashMap;
import java.util.Map;

public class BancoDeDados {
    
    // Como não há threads concorrendo, o HashMap comum é perfeito e mais rápido!
    // Ele guarda o formato: "login_do_usuario" -> Objeto Perfil
    private Map<String, Perfil> usuarios = new HashMap<>();

    public BancoDeDados() {
        // Contas pré-cadastradas para você não perder tempo digitando na apresentação
        usuarios.put("admin", new Perfil("Administrador", "admin", "123456", "ADMIN"));
    }

    // --- MÉTODOS DO CRUD (Sem o 'synchronized' porque não tem concorrência) ---

    public boolean cadastrarUsuario(String nome, String usuario, String senha) {
        if (usuarios.containsKey(usuario)) {
            return false; 
        }
        Perfil novo = new Perfil(nome, usuario, senha, "USER");
        
        // REGRA DO TOKEN NO CADASTRO:
        novo.setToken("usr_" + usuario); 
        
        usuarios.put(usuario, novo);
        return true;
    }
    public Perfil fazerLogin(String usuario, String senha) {
        Perfil p = usuarios.get(usuario);
        
        if (p != null && p.getSenha().equals(senha)) {
            // REGRA DE TOKENS QUE VOCÊ PEDIU:
            if (p.getNivel().equals("ADMIN")) {
                p.setToken("adm"); // Se for admin, o token é fixo "adm"
            } else {
                p.setToken("usr_" + p.getUsuario()); // Se for user, é "usr_usuario"
            }
            return p;
        }
        return null; 
    }
    public String buscarUsuarioPorToken(String token) {
        if (token == null || token.isEmpty()) return null;
        
        for (Perfil p : usuarios.values()) {
            if (token.equals(p.getToken())) {
                return p.getUsuario();
            }
        }
        return null; // Token inválido ou expirado
    }

    public Perfil buscarPerfil(String usuario) {
        return usuarios.get(usuario);
    }

    public void atualizarUsuario(String usuario, String novoNome, String novaSenha) {
        Perfil p = usuarios.get(usuario);
        if (p != null) {
            p.setNome(novoNome);
            p.setSenha(novaSenha);
        }
    }

    public boolean apagarUsuario(String loginAlvo) {
        // O loginAlvo deve ser exatamente o "usuario" (ex: murilo), não o nome completo.
        Perfil p = usuarios.get(loginAlvo);
        
        if (p != null) {
            // Regra de segurança: Não permite apagar utilizadores de nível ADMIN
            if (p.getNivel().equals("ADMIN")) {
                return false; 
            }
            usuarios.remove(loginAlvo);
            return true;
        }
        return false; // Utilizador não encontrado
    }
}