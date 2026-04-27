package utf.cliente;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

public class ciente_class extends JFrame {

    // Componentes de Conexão
    private JTextField txtIP, txtPorta;
    private JButton btnConectar;
    private JTextArea areaLog;

    // Componentes de Chat (Simplificado)
    private JTextField txtMensagemChat;
    private JButton btnEnviarChat;

    // Componentes de Comandos Brutos
    private JTextField txtComando;
    private JButton btnEnviarComando;

    // Variáveis de Controle
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson = new Gson();
    private String tokenSessao = "";
    private String meuUsuario = "Eu"; 

    public ciente_class() {
        setTitle("Sistema de Chat UTFPR - Murilo");
        setSize(600, 550);
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
        painelNorte.add(btnConectar);
        add(painelNorte, BorderLayout.NORTH);

        // --- PAINEL CENTRAL: Histórico ---
        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setLineWrap(true);
        areaLog.setWrapStyleWord(true);
        areaLog.setBackground(new Color(245, 245, 245));
        JScrollPane scroll = new JScrollPane(areaLog);
        scroll.setBorder(BorderFactory.createTitledBorder("Histórico de Conversa / Log"));
        add(scroll, BorderLayout.CENTER);

        // --- PAINEL SUL: Entradas ---
        JPanel painelSul = new JPanel();
        painelSul.setLayout(new BoxLayout(painelSul, BoxLayout.Y_AXIS));
        
        // 1. Área de Chat
        JPanel painelChat = new JPanel(new BorderLayout(5, 5));
        painelChat.setBorder(BorderFactory.createTitledBorder("Bate-Papo (Direto)"));
        txtMensagemChat = new JTextField();
        btnEnviarChat = new JButton("Enviar Mensagem");
        painelChat.add(txtMensagemChat, BorderLayout.CENTER);
        painelChat.add(btnEnviarChat, BorderLayout.EAST);
        
        // 2. Área de Comandos
        JPanel painelComandos = new JPanel(new BorderLayout(5, 5));
        painelComandos.setBorder(BorderFactory.createTitledBorder("Comandos (op;p1;p2)"));
        txtComando = new JTextField();
        btnEnviarComando = new JButton("Executar");
        painelComandos.add(txtComando, BorderLayout.CENTER);
        painelComandos.add(btnEnviarComando, BorderLayout.EAST);

        painelSul.add(painelChat);
        painelSul.add(painelComandos);
        add(painelSul, BorderLayout.SOUTH);

        // Estado Inicial
        setCamposHabilitados(false);

        // Eventos
        btnConectar.addActionListener(e -> conectar());
        btnEnviarChat.addActionListener(e -> enviarMensagemDireta());
        txtMensagemChat.addActionListener(e -> enviarMensagemDireta());
        btnEnviarComando.addActionListener(e -> processarComando());
        txtComando.addActionListener(e -> processarComando());
    }

    private void conectar() {
        try {
            socket = new Socket(txtIP.getText(), Integer.parseInt(txtPorta.getText()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            
            areaLog.append("✅ Conectado ao servidor com sucesso!\n");
            btnConectar.setEnabled(false);
            setCamposHabilitados(true);
            
            new Thread(this::escutarServidor).start();
        } catch (Exception ex) {
            areaLog.append("❌ Erro ao conectar: " + ex.getMessage() + "\n");
        }
    }

    // --- ENVIAR MENSAGEM (CHAT) ---
    private void enviarMensagemDireta() {
        String msg = txtMensagemChat.getText().trim();
        if (msg.isEmpty() || tokenSessao.isEmpty()) {
            if (tokenSessao.isEmpty()) areaLog.append("⚠️ Faça login primeiro!\n");
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("op", "enviarMensagem");
        json.addProperty("token", tokenSessao);
        json.addProperty("texto", msg);

        out.println(json.toString());
        areaLog.append("\n[ Você ]: " + msg + "\n"); // Mostra no seu próprio histórico
        txtMensagemChat.setText("");
        areaLog.setCaretPosition(areaLog.getDocument().getLength());
    }

    // --- PROCESSAR COMANDOS (LOGIN, CADASTRO, ETC) ---
    private void processarComando() {
        String texto = txtComando.getText().trim();
        if (texto.isEmpty()) return;

        String[] p = texto.split(";");
        String op = p[0];
        JsonObject json = new JsonObject();
        json.addProperty("op", op);

        try {
            switch (op) {
                case "cadastrarUsuario":
                    json.addProperty("nome", p[1]);
                    json.addProperty("usuario", p[2]);
                    json.addProperty("senha", p[3]);
                    break;
                case "login":
                    json.addProperty("usuario", p[1]);
                    json.addProperty("senha", p[2]);
                    this.meuUsuario = p[1]; // Salva seu nome para o chat
                    break;
                case "consultarUsuario":
                case "logout":
                case "deletarUsuario":
                    json.addProperty("token", tokenSessao);
                    if (op.equals("deletarUsuario") && p.length > 1) {
                        json.addProperty("usuarioAlvo", p[1]); // Regra do Admin
                    }
                    break;
                case "atualizarUsuario":
                    json.addProperty("token", tokenSessao);
                    json.addProperty("nome", p[1]);
                    json.addProperty("senha", p[2]);
                    break;
                default:
                    areaLog.append("❓ Comando desconhecido.\n");
                    return;
            }
            out.println(json.toString());
            txtComando.setText("");
        } catch (Exception ex) {
            areaLog.append("⚠️ Formato de comando inválido.\n");
        }
    }

    private void escutarServidor() {
        try {
            String linha;
            while ((linha = in.readLine()) != null) {
                JsonObject res = gson.fromJson(linha, JsonObject.class);
                
                // Se for mensagem de chat vindo de alguém
                if (res.has("op") && res.get("op").getAsString().equals("receberMensagem")) {
                    String remetente = res.get("remetente").getAsString();
                    String texto = res.get("texto").getAsString();
                    areaLog.append("\n💬 [ " + remetente + " ]: " + texto + "\n");
                } 
                // Se for resposta de sistema
                else {
                    String status = res.has("resposta") ? res.get("resposta").getAsString() : "---";
                    String msg = res.has("mensagem") ? res.get("mensagem").getAsString() : "";
                    
                    if (res.has("token")) {
                        tokenSessao = res.get("token").getAsString();
                    }

                    // Ignora o log de "Entregue" para não poluir o chat
                    if (!msg.equals("Entregue")) {
                        areaLog.append("SISTEMA (" + status + "): " + msg + "\n");
                    }
                }
                areaLog.setCaretPosition(areaLog.getDocument().getLength());
            }
        } catch (IOException e) {
            areaLog.append("🔴 Conexão encerrada.\n");
            setCamposHabilitados(false);
            btnConectar.setEnabled(true);
        }
    }

    private void setCamposHabilitados(boolean b) {
        txtMensagemChat.setEnabled(b);
        btnEnviarChat.setEnabled(b);
        txtComando.setEnabled(b);
        btnEnviarComando.setEnabled(b);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ciente_class().setVisible(true));
    }
}