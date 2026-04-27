package utf.servidor;

public class Perfil {
    // Adicionado o 'nome' exigido no protocolo
    private String nome;
    private String senha;
    private String token;
    private String nivel;

    // Construtor atualizado
    public Perfil(String nome, String senha, String token, String nivel) {
        this.nome = nome;
        this.senha = senha;
        this.token = token;
        this.nivel = nivel;
    }

    // --- GETTERS E SETTERS ---

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String novaSenha) {
        this.senha = novaSenha;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String novoToken) {
        this.token = novoToken;
    }

    public String getNivel() {
        return nivel;
    }

    public void setNivel(String nivel) {
        this.nivel = nivel;
    }
}