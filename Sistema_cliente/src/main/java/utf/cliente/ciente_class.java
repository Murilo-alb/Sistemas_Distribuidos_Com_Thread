package utf.cliente;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
    private JTextArea areaLog; // Mural e avisos amigáveis
    private JTextArea areaLogServidor; // Log bruto do JSON

    // Chat (Agora apenas o envio padrão!)
    private JTextField txtMensagemChat;
    private JButton btnEnviarChat, btnAtualizarMural; 

    // Autenticação e Usuário
    private JTextField txtNomeAuth, txtUserAuth, txtSenhaAuth;
    private JButton btnCadastrar, btnLogin, btnAtualizarUser, btnConsultarUser;

    // Admin
    private JTextField txtAlvoDeletar;
    private JButton btnDeletarUser;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson = new Gson();
    private String tokenSessao = "";

    public ciente_class() {
        setTitle("Sistema Cliente SÍNCRONO - Fila Estrita");
        setSize(850, 800); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // --- PAINEL NORTE: Conexão ---
        JPanel painelNorte = new JPanel(new FlowLayout());
        painelNorte.add(new JLabel("IP do Servidor:"));
        txtIP = new JTextField("127.0.0.1", 10);
        painelNorte.add(txtIP);
        painelNorte.add(new JLabel("Porta:"));
        txtPorta = new JTextField("12345", 5);
        painelNorte.add(txtPorta);
        
        btnConectar = new JButton("🔌 Conectar");
        btnDesconectar = new JButton("🚪 Sair (Logout)");
        btnDesconectar.setEnabled(false);
        painelNorte.add(btnConectar);
        painelNorte.add(btnDesconectar);
        add(painelNorte, BorderLayout.NORTH);

        // --- PAINEL CENTRAL: Tela do Mural + Log do Servidor ---
        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setLineWrap(true);
        areaLog.setWrapStyleWord(true);
        areaLog.setFont(new Font("Monospaced", Font.PLAIN, 14));
        areaLog.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scrollMural = new JScrollPane(areaLog);
        scrollMural.setBorder(BorderFactory.createTitledBorder("Mural e Sistema"));

        areaLogServidor = new JTextArea();
        areaLogServidor.setEditable(false);
        areaLogServidor.setLineWrap(true);
        areaLogServidor.setWrapStyleWord(true);
        areaLogServidor.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaLogServidor.setForeground(new Color(0, 102, 51)); 
        areaLogServidor.setMargin(new Insets(10, 10, 10, 10));
        JScrollPane scrollLog = new JScrollPane(areaLogServidor);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Log Bruto do Protocolo (JSONs)"));

        JSplitPane splitCenter = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollMural, scrollLog);
        splitCenter.setResizeWeight(0.65); 
        add(splitCenter, BorderLayout.CENTER);

        // --- PAINEL SUL: SISTEMA DE ABAS ---
        JTabbedPane painelAbas = new JTabbedPane();
        painelAbas.setBorder(new EmptyBorder(5, 5, 5, 5));

        // ABA 1: AUTENTICAÇÃO (Login / Cadastro)
        JPanel abaAuth = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 20));
        abaAuth.add(new JLabel("Nome:")); 
        txtNomeAuth = new JTextField(12);
        abaAuth.add(txtNomeAuth);
        abaAuth.add(new JLabel("Usuário:")); 
        txtUserAuth = new JTextField(10);
        abaAuth.add(txtUserAuth);
        abaAuth.add(new JLabel("Senha:")); 
        txtSenhaAuth = new JTextField(8);
        abaAuth.add(txtSenhaAuth);
        
        btnLogin = new JButton("✅ Logar");
        btnCadastrar = new JButton("📝 Cadastrar");
        abaAuth.add(btnLogin);
        abaAuth.add(btnCadastrar);
        painelAbas.addTab("🔑 Acesso Inicial", abaAuth);

        // ABA 2: MURAL DE CHAT (LIMPO E DIRETO)
        JPanel abaChat = new JPanel(new BorderLayout(10, 10));
        abaChat.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JPanel pnlEnvio = new JPanel(new BorderLayout(5, 5));
        pnlEnvio.add(new JLabel("Nova Msg:"), BorderLayout.WEST);
        txtMensagemChat = new JTextField();
        btnEnviarChat = new JButton("Enviar Msg");
        pnlEnvio.add(txtMensagemChat, BorderLayout.CENTER);
        pnlEnvio.add(btnEnviarChat, BorderLayout.EAST);

        JPanel pnlAcoesMsg = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnAtualizarMural = new JButton("🔄 Forçar Atualização do Mural");
        pnlAcoesMsg.add(btnAtualizarMural);

        abaChat.add(pnlEnvio, BorderLayout.NORTH);
        abaChat.add(pnlAcoesMsg, BorderLayout.CENTER);
        painelAbas.addTab("💬 Chat de Mensagens", abaChat);

        // ABA 3: GERENCIAR CONTA
        JPanel abaConta = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        
        JPanel pnlUsuario = new JPanel(new FlowLayout());
        pnlUsuario.setBorder(BorderFactory.createTitledBorder("Meus Dados"));
        btnConsultarUser = new JButton("🔍 Consultar Meus Dados");
        btnAtualizarUser = new JButton("⚙️ Atualizar Nome/Senha");
        pnlUsuario.add(btnConsultarUser);
        pnlUsuario.add(btnAtualizarUser);

        JPanel pnlAdmin = new JPanel(new FlowLayout());
        pnlAdmin.setBorder(BorderFactory.createTitledBorder("Deletar Conta (Comum ou Admin)"));
        pnlAdmin.add(new JLabel("Usuário Alvo (Vazio = Eu mesmo):"));
        txtAlvoDeletar = new JTextField(10);
        btnDeletarUser = new JButton("🚨 Deletar Conta");
        pnlAdmin.add(txtAlvoDeletar);
        pnlAdmin.add(btnDeletarUser);

        abaConta.add(pnlUsuario);
        abaConta.add(pnlAdmin);
        painelAbas.addTab("⚙️ Gerenciar Conta", abaConta);

        add(painelAbas, BorderLayout.SOUTH);

        setCamposHabilitados(false);

        // --- EVENTOS ---
        btnConectar.addActionListener(e -> conectar());
        btnDesconectar.addActionListener(e -> efetuarLogout());
        
        btnCadastrar.addActionListener(e -> efetuarCadastro());
        btnLogin.addActionListener(e -> efetuarLogin());
        
        btnConsultarUser.addActionListener(e -> consultarUsuario()); 
        btnAtualizarUser.addActionListener(e -> abrirJanelaAtualizarDados()); 
        btnDeletarUser.addActionListener(e -> efetuarDelecaoUsuario());
        
        btnEnviarChat.addActionListener(e -> postarNoMural());
        btnAtualizarMural.addActionListener(e -> puxarMural());
        txtMensagemChat.addActionListener(e -> postarNoMural());
    }

    private void conectar() {
        try {
            areaLog.append("⏳ Tentando conectar...\n");
            socket = new Socket(txtIP.getText(), Integer.parseInt(txtPorta.getText()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            
            areaLog.append("✅ Conectado!\n");
            areaLogServidor.append("=== INÍCIO DA SESSÃO ===\n");
            btnConectar.setEnabled(false);
            btnDesconectar.setEnabled(true);
            setCamposHabilitados(true);
        } catch (Exception ex) {
            areaLog.append("❌ Erro ao conectar.\n");
        }
    }

    private void enviarSincrono(JsonObject requisicao) {
        try {
            String reqStr = requisicao.toString();
            out.println(reqStr);
            
            areaLogServidor.append("-> ENVIADO: " + reqStr + "\n");
            areaLogServidor.setCaretPosition(areaLogServidor.getDocument().getLength());

            String respostaStr = in.readLine(); 
            
            if (respostaStr != null) {
                areaLogServidor.append("<- RECEBIDO: " + respostaStr + "\n");
                areaLogServidor.setCaretPosition(areaLogServidor.getDocument().getLength());

                JsonObject res = gson.fromJson(respostaStr, JsonObject.class);
                
                if (res.has("resposta") && res.get("resposta").getAsString().equals("200") && res.has("nome") && res.has("usuario")) {
                    String n = res.get("nome").getAsString();
                    String u = res.get("usuario").getAsString();
                    txtNomeAuth.setText(n);
                    txtUserAuth.setText(u);
                    areaLog.append("SISTEMA: Dados carregados com sucesso (Nome: " + n + ", Usuário: " + u + ").\n");
                }
                else if (res.has("mensagens")) {
                    areaLog.setText("");
                    areaLog.append("================ MURAL GERAL ================\n");
                    JsonArray mensagens = res.getAsJsonArray("mensagens");
                    for (JsonElement elemento : mensagens) {
                        JsonObject msgObj = elemento.getAsJsonObject();
                        String remetente = msgObj.has("usuario") ? msgObj.get("usuario").getAsString() : "Desconhecido";
                        String textoMsg = msgObj.has("mensagem") ? msgObj.get("mensagem").getAsString() : "";
                        // Removido o ID da exibição, fica bem mais limpo!
                        areaLog.append(" 💬 " + remetente + ": " + textoMsg + "\n");
                    }
                    areaLog.append("=============================================\n");
                } 
                else {
                    String status = res.has("resposta") ? res.get("resposta").getAsString() : "---";
                    String msg = res.has("mensagem") ? res.get("mensagem").getAsString() : "";
                    
                    if (res.has("token")) tokenSessao = res.get("token").getAsString();

                    if (!msg.isEmpty() || !status.equals("200")) {
                        areaLog.append("SISTEMA (" + status + "): " + msg + "\n");
                    }

                    if (status.equals("200") && (msg.contains("Postado") || msg.contains("Atualizado") || msg.contains("Deletado") || msg.contains("Login ok") || msg.contains("Enviado"))) {
                        if (!msg.equals("Deletado com sucesso")) {
                             puxarMural(); 
                        }
                    }

                    if (status.equals("200") && (msg.contains("logout") || msg.equals("Deletado com sucesso")) && (requisicao.get("op").getAsString().equals("logout") || requisicao.get("op").getAsString().equals("deletarUsuario"))) {
                        encerrarConexaoLocal();
                    }
                }
                areaLog.setCaretPosition(areaLog.getDocument().getLength());
            }
        } catch (Exception e) {
            areaLog.append("🔴 Servidor encerrou a conexão.\n");
            areaLogServidor.append("=== CONEXÃO ROMPIDA ===\n");
            encerrarConexaoLocal();
        }
    }

    private void efetuarCadastro() {
        JsonObject json = new JsonObject();
        json.addProperty("op", "cadastrarUsuario");
        json.addProperty("nome", txtNomeAuth.getText().trim());
        json.addProperty("usuario", txtUserAuth.getText().trim());
        json.addProperty("senha", txtSenhaAuth.getText().trim());
        enviarSincrono(json);
    }

    private void efetuarLogin() {
        JsonObject json = new JsonObject();
        json.addProperty("op", "login");
        json.addProperty("usuario", txtUserAuth.getText().trim());
        json.addProperty("senha", txtSenhaAuth.getText().trim());
        enviarSincrono(json);
    }

    private void consultarUsuario() {
        if (tokenSessao.isEmpty()) {
            areaLog.append("⚠️ Você precisa estar logado para consultar seus dados.\n");
            return;
        }
        JsonObject json = new JsonObject();
        json.addProperty("op", "consultarUsuario");
        json.addProperty("token", tokenSessao);
        enviarSincrono(json);
    }

    private void abrirJanelaAtualizarDados() {
        if (tokenSessao.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Você precisa estar logado para atualizar seus dados!", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(this, "Atualizar Meus Dados", true);
        dialog.setSize(380, 220);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setLocationRelativeTo(this);

        JPanel pnlCampos = new JPanel(new GridLayout(3, 1, 5, 10));
        pnlCampos.setBorder(new EmptyBorder(15, 15, 5, 15));
        
        pnlCampos.add(new JLabel("Dica: Preencha apenas o que deseja mudar."));
        
        JPanel pnlNome = new JPanel(new BorderLayout(5, 0));
        pnlNome.add(new JLabel("Novo Nome:"), BorderLayout.WEST);
        JTextField txtNovoNome = new JTextField();
        pnlNome.add(txtNovoNome, BorderLayout.CENTER);
        
        JPanel pnlSenha = new JPanel(new BorderLayout(5, 0));
        pnlSenha.add(new JLabel("Nova Senha:"), BorderLayout.WEST);
        JTextField txtNovaSenha = new JTextField();
        pnlSenha.add(txtNovaSenha, BorderLayout.CENTER);

        pnlCampos.add(pnlNome);
        pnlCampos.add(pnlSenha);

        JPanel pnlBotoes = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnSalvar = new JButton("Salvar Alterações");
        JButton btnCancelar = new JButton("Cancelar");
        pnlBotoes.add(btnSalvar);
        pnlBotoes.add(btnCancelar);

        dialog.add(pnlCampos, BorderLayout.CENTER);
        dialog.add(pnlBotoes, BorderLayout.SOUTH);

        btnCancelar.addActionListener(ev -> dialog.dispose());
        
        btnSalvar.addActionListener(ev -> {
            String n = txtNovoNome.getText().trim();
            String s = txtNovaSenha.getText().trim();

            if (n.isEmpty()) n = txtNomeAuth.getText().trim();
            if (s.isEmpty()) s = txtSenhaAuth.getText().trim();

            if (n.isEmpty() || s.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Erro: Não sabemos qual é seu Nome ou Senha atual.\nPor favor, clique em 'Consultar Dados' na aba anterior primeiro.", "Erro", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JsonObject json = new JsonObject();
            json.addProperty("op", "atualizarUsuario");
            json.addProperty("token", tokenSessao);
            json.addProperty("nome", n);
            json.addProperty("senha", s);
            
            enviarSincrono(json);
            
            txtNomeAuth.setText(n);
            txtSenhaAuth.setText(s);

            dialog.dispose();
        });

        dialog.setVisible(true);
    }

    private void efetuarDelecaoUsuario() {
        if (tokenSessao.isEmpty()) return;
        JsonObject json = new JsonObject();
        json.addProperty("op", "deletarUsuario");
        json.addProperty("token", tokenSessao);
        String alvo = txtAlvoDeletar.getText().trim();
        if (!alvo.isEmpty()) json.addProperty("usuarioAlvo", alvo);
        enviarSincrono(json);
        txtAlvoDeletar.setText("");
    }

    private void efetuarLogout() {
        if (tokenSessao.isEmpty()) { encerrarConexaoLocal(); return; }
        JsonObject json = new JsonObject();
        json.addProperty("op", "logout");
        json.addProperty("token", tokenSessao);
        enviarSincrono(json);
    }

    private void postarNoMural() {
        if (txtMensagemChat.getText().trim().isEmpty() || tokenSessao.isEmpty()) return;
        JsonObject json = new JsonObject();
        json.addProperty("op", "enviarMensagem");
        json.addProperty("token", tokenSessao);
        json.addProperty("mensagem", txtMensagemChat.getText().trim());
        txtMensagemChat.setText("");
        enviarSincrono(json);
    }

    private void puxarMural() {
        if (tokenSessao.isEmpty()) return;
        JsonObject json = new JsonObject();
        json.addProperty("op", "receberMensagens");
        json.addProperty("token", tokenSessao);
        enviarSincrono(json);
    }

    private void encerrarConexaoLocal() {
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException e) {}
        tokenSessao = "";
        btnConectar.setEnabled(true);
        btnDesconectar.setEnabled(false);
        setCamposHabilitados(false);
        areaLog.append("Você saiu da fila.\n");
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
        btnAtualizarUser.setEnabled(b);
        btnConsultarUser.setEnabled(b); 
        
        txtAlvoDeletar.setEnabled(b);
        btnDeletarUser.setEnabled(b);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ciente_class().setVisible(true));
    }
}