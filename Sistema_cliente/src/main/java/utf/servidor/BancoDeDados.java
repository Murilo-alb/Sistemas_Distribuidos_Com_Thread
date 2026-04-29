package utf.servidor;

import java.util.HashMap;
import java.util.Map;

public class BancoDeDados {
    
    // Como não há threads concorrendo, o HashMap comum é perfeito e mais rápido!
    // Ele guarda o formato: "login_do_usuario" -> Objeto Perfil
    private Map<String, Perfil> usuarios = new HashMap<>();

    public BancoDeDados() {
        // Conta pré-cadastrada (admin). 
        // Nota: A senha tem 6 dígitos para respeitar a própria regra do sistema!
        usuarios.put("admin", new Perfil("Administrador", "admin", "123456", "ADMIN"));
    }

    // --- MÉTODOS DE VALIDAÇÃO PRIVADOS ---
    private boolean isUsuarioValido(String usuario) {
        // Regra: Letras e números concatenados, sem espaços/especiais, entre 5 a 20 caracteres
        return usuario != null && usuario.matches("^[a-zA-Z0-9]{5,20}$");
    }

    private boolean isSenhaValida(String senha) {
        // Regra: Apenas números, exatamente 6 dígitos
        return senha != null && senha.matches("^[0-9]{6}$");
    }

    // --- MÉTODOS DO CRUD (Sem o 'synchronized' porque não tem concorrência) ---

    public boolean cadastrarUsuario(String nome, String usuario, String senha) {
        // 1. Verifica se os campos não estão vazios
        if (nome == null || nome.trim().isEmpty() || usuario == null || senha == null) {
            return false;
        }
        
        // 2. Valida as regras restritas do Protocolo
        if (!isUsuarioValido(usuario) || !isSenhaValida(senha)) {
            return false; 
        }

        // 3. Verifica se o usuário já existe
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

    // Alterado para retornar boolean para o Servidor saber se as regras foram respeitadas
    public boolean atualizarUsuario(String usuario, String novoNome, String novaSenha) {
        Perfil p = usuarios.get(usuario);
        
        if (p != null) {
            // Verifica regras de nome vazio e senha de 6 dígitos numéricos
            if (novoNome == null || novoNome.trim().isEmpty() || !isSenhaValida(novaSenha)) {
                return false; 
            }
            
            p.setNome(novoNome);
            p.setSenha(novaSenha);
            return true;
        }
        return false;
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