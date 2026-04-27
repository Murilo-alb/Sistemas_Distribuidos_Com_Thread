package utf.servidor;

import java.util.HashMap;
import java.util.Map;

public class BancoDeDados {
    
    // Armazenamento em memória utilizando o login (usuario) como chave
    private Map<String, Perfil> usuarios;

    public BancoDeDados() {
        this.usuarios = new HashMap<>();
        // RNF05: O Admin é inicializado com o token fixo "adm"
        // Formato: nome, senha, token, nivel
        usuarios.put("admin", new Perfil("Administrador", "123456", "adm", "ADMIN"));
    }

    // --- MÉTODOS DO CRUD ---

    /**
     * RF03 / Imagem 1: Cadastro de utilizador
     * @return true se o utilizador foi criado, false se o login já existir
     */
    public boolean cadastrarUsuario(String nome, String usuario, String senha) {
        if (usuarios.containsKey(usuario)) {
            return false;
        }
        // Utilizadores novos começam sem token (gerado apenas no login)
        usuarios.put(usuario, new Perfil(nome, senha, "NENHUM", "USUARIO"));
        return true;
    }

    /**
     * RF01 / RNF07: Autenticação e geração de token
     */
    public Perfil fazerLogin(String usuario, String senha) {
        if (usuarios.containsKey(usuario)) {
            Perfil perfil = usuarios.get(usuario);
            
            if (perfil.getSenha().equals(senha)) {
                // RNF07: Gera o token no formato usr_nomeusuario para utilizadores comuns
                if (perfil.getNivel().equals("USUARIO")) {
                    perfil.setToken("usr_" + usuario);
                }
                return perfil;
            }
        }
        return null;
    }

    /**
     * Imagem 1: Atualização de dados (Nome e Senha)
     */
    public boolean atualizarUsuario(String usuario, String novoNome, String novaSenha) {
        if (usuarios.containsKey(usuario)) {
            Perfil p = usuarios.get(usuario);
            p.setNome(novoNome);
            p.setSenha(novaSenha);
            return true;
        }
        return false;
    }

    /**
     * RF03: Eliminação de utilizador (Impede eliminar o admin)
     */
    public boolean apagarUsuario(String usuario) {
        if (usuario.equalsIgnoreCase("admin")) {
            return false; 
        }
        return usuarios.remove(usuario) != null;
    }

    public Perfil buscarPerfil(String usuario) {
        return usuarios.get(usuario);
    }

    /**
     * Método auxiliar essencial para localizar o utilizador através do token 
     * enviado pelo cliente nas operações de Update, Delete e Read.
     */
    public String buscarUsuarioPorToken(String token) {
        for (Map.Entry<String, Perfil> entry : usuarios.entrySet()) {
            if (entry.getValue().getToken().equals(token)) {
                return entry.getKey(); // Retorna o login do dono do token
            }
        }
        return null;
    }
}