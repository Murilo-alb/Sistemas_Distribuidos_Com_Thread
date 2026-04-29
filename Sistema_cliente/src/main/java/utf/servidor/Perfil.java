package utf.servidor;

public class Perfil {
    
    private String nome;
    private String usuario;
    private String senha;
    private String token;
    private String nivel; // Define se é "ADMIN" ou "USER"

    public Perfil(String nome, String usuario, String senha, String nivel) {
        this.nome = nome;
        this.usuario = usuario;
        this.senha = senha;
        this.nivel = nivel;
        this.token = null; // O token começa nulo até a pessoa fazer login
    }

    // --- GETTERS E SETTERS BÁSICOS ---

    public String getNome() { 
        return nome; 
    }
    
    public void setNome(String nome) { 
        this.nome = nome; 
    }

    public String getUsuario() { 
        return usuario; 
    }
    
    public String getSenha() { 
        return senha; 
    }
    
    public void setSenha(String senha) { 
        this.senha = senha; 
    }

    public String getToken() { 
        return token; 
    }
    
    public String getNivel() { 
        return nivel; 
    }
    
    // O BancoDeDados decide e injeta qual será o token (adm ou usr_nome)
    public void setToken(String novoToken) {
        this.token = novoToken;
    }

    // --- LÓGICA DE SESSÃO ---
    
    // Limpa o token por segurança (usado no logout para invalidar a sessão)
    public void limparToken() {
        this.token = null;
    }
}