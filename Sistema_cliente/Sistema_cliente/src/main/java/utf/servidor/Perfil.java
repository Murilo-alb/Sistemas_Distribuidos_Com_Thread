package utf.servidor;

public class Perfil {
    private String nome;
    private String usuario;
    private String senha;
    private String token;
    private String nivel; // "ADMIN" ou "USER"
    private String ip;

    public Perfil(String nome, String usuario, String senha, String nivel) {
        this.nome = nome;
        this.usuario = usuario;
        this.senha = senha;
        this.nivel = nivel;
        this.token = null;
        this.ip = null;
    }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    
    public String getUsuario() { return usuario; }
    
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    
    public String getToken() { return token; }
    public void setToken(String novoToken) { this.token = novoToken; }
    
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public void limparSessao() { 
        this.token = null; 
        this.ip = null;
    }
    
    public String getNivel() { return nivel; }
}