package utf.cliente;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class ciente_class extends JFrame {

    private JTextField txtIP, txtPorta;
    private JButton btnConectar, btnDesconectar;
    private JTextArea areaLog;

    // Chat
    private JTextField txtMensagemChat;
    private JButton btnEnviarChat, btnAtualizarMural; 

    // Autenticação
    private JTextField txtNomeAuth, txtUserAuth, txtSenhaAuth;
    private JButton btnCadastrar, btnLogin;

    // Admin (NOVO)
    private JTextField txtAlvoDeletar;
    private JButton btnDeletar;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson = new Gson();
    private String tokenSessao = "";

    public ciente_class() {
        setTitle("Sistema Cliente SÍNCRONO - Fila Estrita");
        setSize(700, 700); // Aumentei um pouquinho para caber tudo
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // --- PAINEL NORTE: Conexão ---
        JPanel painelNorte = new JPanel(new FlowLayout());
        painelNorte.add(new JLabel("IP:"));
        txtIP = new JTextField("127.0.0.1", 10);
        painelNorte.add(txtIP);
        painelNorte.add(new JLabel("Porta:"));
        txtPorta = new JTextField("12345", 5);
        painelNorte.add(txtPorta);
        
        btnConectar = new JButton("Conectar");
        btnDesconectar = new JButton("Sair da Fila (Logout)");
        btnDesconectar.setEnabled(false);
        
        painelNorte.add(btnConectar);
        painelNorte.add(btnDesconectar);
        add(painelNorte, BorderLayout.NORTH);

        // --- PAINEL CENTRAL: Tela do Mural ---
        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setLineWrap(true);
        areaLog.setWrapStyleWord(true);
        areaLog.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JScrollPane scroll = new JScrollPane(areaLog);
        add(scroll, BorderLayout.CENTER);

        // --- PAINEL SUL: Agrupando as funções ---
        JPanel painelSul = new JPanel();
        painelSul.setLayout(new BoxLayout(painelSul, BoxLayout.Y_AXIS));
        
        // 1. Painel Admin (Opcional: Deletar Contas)
        JPanel painelAdmin = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painelAdmin.setBorder(BorderFactory.createTitledBorder("Painel de Controle (Deletar Conta)"));
        painelAdmin.add(new JLabel("Usuário Alvo (Vazio = Auto-deletar):"));
        txtAlvoDeletar = new JTextField(12);
        btnDeletar = new JButton("Deletar");
        painelAdmin.add(txtAlvoDeletar);
        painelAdmin.add(btnDeletar);

        // 2. Painel de Autenticação
        JPanel painelAuth = new JPanel(new GridLayout(2, 4, 5, 5));
        painelAuth.setBorder(BorderFactory.createTitledBorder("Autenticação"));
        painelAuth.add(new JLabel("Nome (Cadastro):", SwingConstants.RIGHT));
        txtNomeAuth = new JTextField();
        painelAuth.add(txtNomeAuth);
        painelAuth.add(new JLabel("Usuário:", SwingConstants.RIGHT));
        txtUserAuth = new JTextField();
        painelAuth.add(txtUserAuth);
        painelAuth.add(new JLabel("Senha:", SwingConstants.RIGHT));
        txtSenhaAuth = new JTextField(); 
        painelAuth.add(txtSenhaAuth);
        btnCadastrar = new JButton("Cadastrar");
        btnLogin = new JButton("Logar");
        painelAuth.add(btnCadastrar);
        painelAuth.add(btnLogin);

        // 3. Painel de Chat
        JPanel painelChat = new JPanel(new BorderLayout(5, 5));
        painelChat.setBorder(BorderFactory.createTitledBorder("Mural de Mensagens"));
        txtMensagemChat = new JTextField();
        JPanel botoesChat = new JPanel(new GridLayout(1, 2, 5, 0));
        btnAtualizarMural = new JButton("🔄 Atualizar Mural");
        btnEnviarChat = new JButton("Postar Msg");
        botoesChat.add(btnAtualizarMural);
        botoesChat.add(btnEnviarChat);
        painelChat.add(txtMensagemChat, BorderLayout.CENTER);
        painelChat.add(botoesChat, BorderLayout.EAST);

        painelSul.add(painelAdmin);
        painelSul.add(painelAuth);
        painelSul.add(painelChat);
        add(painelSul, BorderLayout.SOUTH);

        setCamposHabilitados(false);

        // --- EVENTOS ---
        btnConectar.addActionListener(e -> conectar());
        btnDesconectar.addActionListener(e -> efetuarLogout());
        btnEnviarChat.addActionListener(e -> postarNoMural());
        txtMensagemChat.addActionListener(e -> postarNoMural());
        btnAtualizarMural.addActionListener(e -> puxarMural());
        btnCadastrar.addActionListener(e -> efetuarCadastro());
        btnLogin.addActionListener(e -> efetuarLogin());
        btnDeletar.addActionListener(e -> efetuarDelecao()); // Adicionamos o evento do botão aqui
    }

    private void conectar() {
        try {
            areaLog.append("⏳ Tentando conectar... (Pode travar se o servidor estiver ocupado)\n");
            socket = new Socket(txtIP.getText(), Integer.parseInt(txtPorta.getText()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            
            areaLog.append("✅ Conectado! O Servidor agora é todo seu.\n");
            btnConectar.setEnabled(false);
            btnDesconectar.setEnabled(true);
            setCamposHabilitados(true);
        } catch (Exception ex) {
            areaLog.append("❌ Erro ao conectar: Servidor desligado ou fila cheia.\n");
        }
    }

    private void enviarSincrono(JsonObject requisicao) {
        try {
            out.println(requisicao.toString());
            String respostaStr = in.readLine(); 
            
            if (respostaStr != null) {
                JsonObject res = gson.fromJson(respostaStr, JsonObject.class);
                
                if (res.has("tipo") && res.get("tipo").getAsString().equals("mural")) {
                    areaLog.setText("");
                    areaLog.append("================ MURAL GERAL ================\n");
                    JsonArray mensagens = res.getAsJsonArray("lista");
                    for (JsonElement elemento : mensagens) {
                        JsonObject msg = elemento.getAsJsonObject();
                        areaLog.append(" 💬 [" + msg.get("remetente").getAsString() + "]: " + msg.get("texto").getAsString() + "\n");
                    }
                    areaLog.append("=============================================\n");
                } 
                else {
                    String status = res.has("resposta") ? res.get("resposta").getAsString() : "---";
                    String msg = res.has("mensagem") ? res.get("mensagem").getAsString() : "OK";
                    
                    if (res.has("token")) tokenSessao = res.get("token").getAsString();

                    if (msg.contains("Login ok") && status.equals("200")) {
                        areaLog.append("SISTEMA: Login efetuado. Baixando mural...\n");
                        puxarMural(); 
                    } else if (msg.equals("Postado")) {
                        puxarMural(); 
                    } else {
                        areaLog.append("SISTEMA (" + status + "): " + msg + "\n");
                    }

                    if (msg.equals("Deslogado") && status.equals("200")) {
                        encerrarConexaoLocal();
                    }
                }
                areaLog.setCaretPosition(areaLog.getDocument().getLength());
            }
        } catch (Exception e) {
            areaLog.append("🔴 Servidor encerrou a conexão.\n");
            encerrarConexaoLocal();
        }
    }

    // --- NOVA FUNÇÃO: DELETAR ---
    private void efetuarDelecao() {
        if (tokenSessao.isEmpty()) {
            areaLog.append("⚠️ Para deletar uma conta, faça login primeiro.\n");
            return;
        }

        String alvo = txtAlvoDeletar.getText().trim();
        JsonObject json = new JsonObject();
        json.addProperty("op", "deletarUsuario");
        json.addProperty("token", tokenSessao);
        
        // Se preencheu o alvo, ele manda o JSON com o usuário alvo para o admin deletar.
        // Se deixou em branco, não manda o alvo (o servidor entende que é o próprio usuário querendo se deletar)
        if (!alvo.isEmpty()) {
            json.addProperty("usuarioAlvo", alvo);
        }

        enviarSincrono(json);
        txtAlvoDeletar.setText("");
    }

    private void efetuarCadastro() {
        String nome = txtNomeAuth.getText().trim();
        String user = txtUserAuth.getText().trim();
        String senha = txtSenhaAuth.getText().trim();

        if (nome.isEmpty() || user.isEmpty() || senha.isEmpty()) {
            areaLog.append("⚠️ Para cadastrar, preencha Nome, Usuário e Senha.\n");
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("op", "cadastrarUsuario");
        json.addProperty("nome", nome);
        json.addProperty("usuario", user);
        json.addProperty("senha", senha);

        enviarSincrono(json);
    }

    private void efetuarLogin() {
        String user = txtUserAuth.getText().trim();
        String senha = txtSenhaAuth.getText().trim();

        if (user.isEmpty() || senha.isEmpty()) {
            areaLog.append("⚠️ Para logar, preencha Usuário e Senha.\n");
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("op", "login");
        json.addProperty("usuario", user);
        json.addProperty("senha", senha);

        enviarSincrono(json);
    }

    private void efetuarLogout() {
        if (tokenSessao.isEmpty()) {
            encerrarConexaoLocal(); 
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("op", "logout");
        json.addProperty("token", tokenSessao);
        enviarSincrono(json);
    }

    private void encerrarConexaoLocal() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {}
        
        tokenSessao = "";
        btnConectar.setEnabled(true);
        btnDesconectar.setEnabled(false);
        setCamposHabilitados(false);
        areaLog.append("Você saiu da fila. Outro cliente pode conectar agora.\n");
    }

    private void postarNoMural() {
        String msg = txtMensagemChat.getText().trim();
        if (msg.isEmpty() || tokenSessao.isEmpty()) return;

        JsonObject json = new JsonObject();
        json.addProperty("op", "enviarMensagem");
        json.addProperty("token", tokenSessao);
        json.addProperty("texto", msg);

        txtMensagemChat.setText("");
        enviarSincrono(json);
    }

    private void puxarMural() {
        if (tokenSessao.isEmpty()) return;
        JsonObject json = new JsonObject();
        json.addProperty("op", "read");
        json.addProperty("token", tokenSessao);
        enviarSincrono(json);
    }

    private void setCamposHabilitados(boolean b) {
        txtMensagemChat.setEnabled(b);
        btnEnviarChat.setEnabled(b);
        btnAtualizarMural.setEnabled(b);
        
        txtNomeAuth.setEnabled(b);
        txtUserAuth.setEnabled(b);
        txtSenhaAuth.setEnabled(b);
        btnCadastrar.setEnabled(b);
        btnLogin.setEnabled(b);
        
        txtAlvoDeletar.setEnabled(b);
        btnDeletar.setEnabled(b);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ciente_class().setVisible(true));
    }
}