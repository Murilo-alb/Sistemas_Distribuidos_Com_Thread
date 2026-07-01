package utf.cliente;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class ciente_class extends JFrame { 

    private JTextField txtIP, txtPorta;
    private JButton btnConectar, btnDesconectarServidor;
    private JTextArea areaLogSistema, areaLogServidor; 
    private JTabbedPane painelAbas;

    // Autenticação e Usuário
    private JTextField txtNomeAuth, txtUserAuth, txtSenhaAuth;
    private JButton btnCadastrar, btnLogin, btnLogout;

    // Conta / Admin
    private JButton btnAtualizarUser, btnConsultarUser, btnDeletarMinhaConta;
    private JButton btnAdminListar, btnAdminAtualizar, btnAdminDeletar, btnAdminConsultar; 
    private JPanel pnlAdmin;

    // Chat
    private DefaultListModel<String> modelUsuariosOnline;
    private JList<String> listUsuariosOnline;
    private JTextArea areaChat;
    private JTextField txtMensagem;
    private JButton btnEnviarMsg, btnAtualizarLista, btnBroadcast;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Gson gson = new Gson();
    private String tokenSessao = "";
    private boolean isAdmin = false;
    private Thread listenerThread; 

    public ciente_class(){
        setTitle("Sistema Cliente - EP2 & EP3 Integrados");
        setSize(950, 800); 
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        addWindowListener(new WindowAdapter(){ 
        	public void windowClosing(WindowEvent e){
            encerrarConexaoLocal();
                System.exit(0);
            }
        });

        JPanel painelNorte = new JPanel(new FlowLayout());
        painelNorte.add(new JLabel("IP:"));
        txtIP = new JTextField("127.0.0.1", 10);
        painelNorte.add(txtIP);
        painelNorte.add(new JLabel("Porta:"));
        txtPorta = new JTextField("12345", 5);
        painelNorte.add(txtPorta);
        
        btnConectar = new JButton("🔌 Conectar");
        btnDesconectarServidor = new JButton("🛑 Desconectar");
        painelNorte.add(btnConectar);
        painelNorte.add(btnDesconectarServidor);
        add(painelNorte, BorderLayout.NORTH);

        areaLogSistema = new JTextArea();
        areaLogSistema.setEditable(false);
        JScrollPane scrollSys = new JScrollPane(areaLogSistema);
        scrollSys.setBorder(BorderFactory.createTitledBorder("Mural Sistema (Consultas e Retornos)"));

        areaLogServidor = new JTextArea();
        areaLogServidor.setEditable(false);
        areaLogServidor.setForeground(new Color(0, 102, 51)); 
        JScrollPane scrollLog = new JScrollPane(areaLogServidor);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Log Rede"));

        JSplitPane splitLogs = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollSys, scrollLog);
        splitLogs.setResizeWeight(0.5); 

        painelAbas = new JTabbedPane();

        // Aba 1: Auth
        JPanel abaAuth = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 20));
        abaAuth.add(new JLabel("Nome:")); 
        txtNomeAuth = new JTextField(10); 
        abaAuth.add(txtNomeAuth);
        abaAuth.add(new JLabel("User:")); 
        txtUserAuth = new JTextField(10); 
        abaAuth.add(txtUserAuth);
        abaAuth.add(new JLabel("Senha:")); 
        txtSenhaAuth = new JTextField(8); 
        abaAuth.add(txtSenhaAuth);
        
        btnLogin = new JButton("✅ Logar");
        btnCadastrar = new JButton("📝 Cadastrar");
        btnLogout = new JButton("🚪 Logout");
        abaAuth.add(btnLogin); 
        abaAuth.add(btnCadastrar); 
        abaAuth.add(btnLogout);
        painelAbas.addTab("🔑 Acesso", abaAuth);

        // Aba 2: Conta / Admin
        JPanel abaConta = new JPanel(new BorderLayout());
        JPanel pnlUsuario = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlUsuario.setBorder(BorderFactory.createTitledBorder("Meus Dados"));
        btnConsultarUser = new JButton("🔍 Consultar");
        btnAtualizarUser = new JButton("⚙️ Atualizar");
        btnDeletarMinhaConta = new JButton("🚨 Deletar Minha Conta");
        pnlUsuario.add(btnConsultarUser); 
        pnlUsuario.add(btnAtualizarUser); 
        pnlUsuario.add(btnDeletarMinhaConta);
        
        pnlAdmin = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlAdmin.setBorder(BorderFactory.createTitledBorder("Painel ADMIN"));
        btnAdminListar = new JButton("Listar Usuários");
        btnAdminConsultar = new JButton("Consultar Alvo");
        btnAdminAtualizar = new JButton("Atualizar Alvo");
        btnAdminDeletar = new JButton("Deletar Alvo");
        pnlAdmin.add(btnAdminListar); 
        pnlAdmin.add(btnAdminConsultar); 
        pnlAdmin.add(btnAdminAtualizar); 
        pnlAdmin.add(btnAdminDeletar);

        abaConta.add(pnlUsuario, BorderLayout.NORTH);
        abaConta.add(pnlAdmin, BorderLayout.CENTER);
        painelAbas.addTab("⚙️ Conta & Admin", abaConta);

        // Aba 3: CHAT
        JPanel abaChat = new JPanel(new BorderLayout());
        areaChat = new JTextArea();
        areaChat.setEditable(false);
        areaChat.setLineWrap(true);
        JScrollPane scrollChat = new JScrollPane(areaChat);
        scrollChat.setBorder(BorderFactory.createTitledBorder("Conversas"));

        modelUsuariosOnline = new DefaultListModel<>();
        listUsuariosOnline = new JList<>(modelUsuariosOnline);
        JScrollPane scrollUsers = new JScrollPane(listUsuariosOnline);
        scrollUsers.setBorder(BorderFactory.createTitledBorder("Usuários Online"));
        scrollUsers.setPreferredSize(new Dimension(200, 0));

        JPanel painelEnvio = new JPanel(new BorderLayout(5, 5));
        txtMensagem = new JTextField();
        btnEnviarMsg = new JButton("Enviar Privado");
        btnBroadcast = new JButton("Enviar /Todos");
        btnAtualizarLista = new JButton("↻ Atualizar Lista");

        JPanel painelBotoesChat = new JPanel(new FlowLayout());
        painelBotoesChat.add(btnAtualizarLista);
        painelBotoesChat.add(btnEnviarMsg);
        painelBotoesChat.add(btnBroadcast);

        painelEnvio.add(txtMensagem, BorderLayout.CENTER);
        painelEnvio.add(painelBotoesChat, BorderLayout.EAST);

        abaChat.add(scrollUsers, BorderLayout.WEST);
        abaChat.add(scrollChat, BorderLayout.CENTER);
        abaChat.add(painelEnvio, BorderLayout.SOUTH);
        painelAbas.addTab("💬 Chat (EP-3)", abaChat);

        JSplitPane splitMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitLogs, painelAbas);
        splitMain.setResizeWeight(0.4);
        add(splitMain, BorderLayout.CENTER);
        
        setEstadoDesconectadoDaRede();

        btnConectar.addActionListener(e -> conectar());
        btnDesconectarServidor.addActionListener(e -> encerrarConexaoLocal());
        btnCadastrar.addActionListener(e -> efetuarCadastro());
        btnLogin.addActionListener(e -> efetuarLogin());
        btnLogout.addActionListener(e -> efetuarLogout());
        
        btnConsultarUser.addActionListener(e -> consultarUsuario()); 
        btnAtualizarUser.addActionListener(e -> abrirJanelaAtualizarDados(false)); 
        btnDeletarMinhaConta.addActionListener(e -> efetuarDelecaoUsuario(false));

        btnAdminListar.addActionListener(e -> listarUsuariosAdmin());
        btnAdminConsultar.addActionListener(e -> consultarUsuarioAdmin());
        btnAdminAtualizar.addActionListener(e -> abrirJanelaAtualizarDados(true));
        btnAdminDeletar.addActionListener(e -> efetuarDelecaoUsuario(true));

        btnAtualizarLista.addActionListener(e -> pedirListaUsuarios());
        btnEnviarMsg.addActionListener(e -> enviarMensagemPrivada());
        btnBroadcast.addActionListener(e -> enviarMensagemBroadcast());
    }

    private void setEstadoDesconectadoDaRede(){
        btnConectar.setEnabled(true);
        btnDesconectarServidor.setEnabled(false);
        txtIP.setEnabled(true); 
        txtPorta.setEnabled(true);
        tokenSessao = ""; 
        isAdmin = false;
        btnLogin.setEnabled(false); 
        btnCadastrar.setEnabled(false); 
        btnLogout.setEnabled(false);
        painelAbas.setEnabledAt(1, false); 
        painelAbas.setEnabledAt(2, false); 
        painelAbas.setSelectedIndex(0);
        areaLogSistema.append("SISTEMA: Desconectado.\n");
    }

    private void setEstadoConectadoMasDeslogado(){
        btnConectar.setEnabled(false);
        btnDesconectarServidor.setEnabled(true);
        txtIP.setEnabled(false); 
        txtPorta.setEnabled(false);
        tokenSessao = ""; 
        isAdmin = false;
        btnLogin.setEnabled(true); 
        btnCadastrar.setEnabled(true); 
        btnLogout.setEnabled(false);
        painelAbas.setEnabledAt(1, false); 
        painelAbas.setEnabledAt(2, false); 
        painelAbas.setSelectedIndex(0);
        modelUsuariosOnline.clear();
        areaChat.setText("");
    }

    private void setEstadoLogadoNaConta(){
        btnLogin.setEnabled(false); 
        btnCadastrar.setEnabled(false); 
        btnLogout.setEnabled(true);
        painelAbas.setEnabledAt(1, true); 
        painelAbas.setEnabledAt(2, true); 
        pnlAdmin.setVisible(isAdmin); 
        painelAbas.setSelectedIndex(2); 
        pedirListaUsuarios(); 
    }

    private void conectar(){
        try{
            socket = new Socket(txtIP.getText(), Integer.parseInt(txtPorta.getText()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            areaLogSistema.append("✅ Conectado ao Servidor!\n");
            setEstadoConectadoMasDeslogado();

            listenerThread = new Thread(() -> {
                try {
                    String linha;
                    while ((linha = in.readLine()) != null) {
                        final String resJson = linha;
                        SwingUtilities.invokeLater(() -> processarMensagemServidor(resJson));
                    }
                    // Se chegar aqui, o servidor fechou a conexão (readLine retornou null)
                    throw new IOException("Servidor encerrou a conexão");
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> {
                        areaLogSistema.append("❌ Desconectado pelo Administrador ou Servidor.\n");
                        // FORÇA O RETORNO PARA A TELA DE LOGIN
                        setEstadoDesconectadoDaRede(); 
                        JOptionPane.showMessageDialog(this,"Sua conta foi removida ou a conexão foi encerrada.");
                    });
                }
            });
            listenerThread.start();

        }catch (Exception ex) { 
            areaLogSistema.append("❌ Erro ao conectar.\n"); 
        }
    }

    private void encerrarConexaoLocal(){
        try{ 
            if(socket != null && !socket.isClosed()){
                if(!tokenSessao.isEmpty()) efetuarLogout();
                socket.close(); 
            }
        }catch (IOException e) {
        }finally{
            setEstadoDesconectadoDaRede();
        }
    }

    private void processarMensagemServidor(String jsonStr) {
        areaLogServidor.append("<- RECEBEU: " + jsonStr + "\n");
        try {
            JsonObject res = gson.fromJson(jsonStr, JsonObject.class);

            if (res.has("op") && res.get("op").getAsString().equals("receberMensagem")) {
                String remetente = res.get("remetente").getAsString();
                String msg = res.get("mensagem").getAsString();
                areaChat.append("💬 [" + remetente + "]: " + msg + "\n");
                areaChat.setCaretPosition(areaChat.getDocument().getLength());
                return;
            }
       
            if (res.has("lista_usuarios")) {
                JsonArray arr = res.getAsJsonArray("lista_usuarios");
                
                if(arr.size() > 0 && arr.get(0).isJsonPrimitive()) {
                    modelUsuariosOnline.clear();
                    for (JsonElement e : arr) modelUsuariosOnline.addElement(e.getAsString());
                } else if(arr.size() > 0 && arr.get(0).isJsonObject()) {
                    areaLogSistema.setText("================ LISTA DE USUÁRIOS (ADMIN) ================\n");
                    for (JsonElement e : arr) {
                        JsonObject u = e.getAsJsonObject();
                        areaLogSistema.append(String.format("👤 Nome: %s | Usuario: %s\n", u.get("nome").getAsString(), u.get("usuario").getAsString()));
                    }
                } else {
                    modelUsuariosOnline.clear();
                }
                return;
            }

            if (res.has("nome") && res.has("usuario")) {
                areaLogSistema.append("🔍 Consulta efetuada - Nome: " + res.get("nome").getAsString() + " | Usuário: " + res.get("usuario").getAsString() + "\n");
                return;
            }

            if (res.has("resposta")) {
                String status = res.get("resposta").getAsString();
                
                if (status.equals("200")) {
                    if (res.has("token")) {
                        tokenSessao = res.get("token").getAsString();
                        isAdmin = tokenSessao.equals("adm"); 
                        setEstadoLogadoNaConta();
                        areaLogSistema.append("✅ Login bem sucedido!\n");
                    } else if (res.has("mensagem")) {
                        String msg = res.get("mensagem").getAsString();
                        areaLogSistema.append("SISTEMA (200): " + msg + "\n");
                        if(msg.equals("logout efetuado") || msg.equals("Deletado com sucesso")) {
                            setEstadoConectadoMasDeslogado();
                        }
                    }
                } else if (res.has("mensagem")) {
                    areaLogSistema.append("⚠️ SISTEMA ("+ status +"): " + res.get("mensagem").getAsString() + "\n");
                }
            }

        } catch (Exception e) {
            areaLogSistema.append("Erro ao processar JSON: " + jsonStr + "\n");
        }
    }

    private void enviarAssincrono(JsonObject requisicao){
        if (out != null) {
            out.println(requisicao.toString());
            areaLogServidor.append("-> ENVIADO: " + requisicao.toString() + "\n");
        }
    }

    private void efetuarCadastro(){
        JsonObject json = new JsonObject();
        json.addProperty("op", "cadastrarUsuario");
        json.addProperty("nome", txtNomeAuth.getText().trim());
        json.addProperty("usuario", txtUserAuth.getText().trim());
        json.addProperty("senha", txtSenhaAuth.getText().trim());
        enviarAssincrono(json);
    }

    private void efetuarLogin(){
        JsonObject json = new JsonObject();
        json.addProperty("op", "login");
        json.addProperty("usuario", txtUserAuth.getText().trim());
        json.addProperty("senha", txtSenhaAuth.getText().trim());
        enviarAssincrono(json);
    }

    private void efetuarLogout(){
        JsonObject json = new JsonObject();
        json.addProperty("op", "logout");
        json.addProperty("token", tokenSessao);
        enviarAssincrono(json);
    }

    private void consultarUsuario(){
        JsonObject json = new JsonObject();
        json.addProperty("op", "consultarUsuario");
        json.addProperty("token", tokenSessao);
        enviarAssincrono(json);
    }

    private void listarUsuariosAdmin(){
        JsonObject json = new JsonObject();
        json.addProperty("op", "consultarUsuariosAdmin");
        json.addProperty("token", "adm");
        enviarAssincrono(json);
    }
    
    private void consultarUsuarioAdmin(){
        String alvo = JOptionPane.showInputDialog(this, "ADMIN: Digite o [usuario] do alvo:");
        if(alvo != null && !alvo.trim().isEmpty()) {
            JsonObject json = new JsonObject();
            json.addProperty("op", "consultarUsuarioAdmin");
            json.addProperty("token", "adm");
            json.addProperty("usuario", alvo);
            enviarAssincrono(json);
        }
    }

    private void abrirJanelaAtualizarDados(boolean comoAdmin){
        String alvo = "";
        if(comoAdmin){
            alvo = JOptionPane.showInputDialog(this, "ADMIN: Digite o [usuario] do alvo que deseja alterar:");
            if(alvo == null || alvo.trim().isEmpty()) return;
        }

        String tituloJanela = comoAdmin ? "ADMIN: Atualizar Alvo" : "Atualizar Meus Dados";

        JDialog dialog = new JDialog(this, tituloJanela, true);
        dialog.setSize(380, 220); dialog.setLayout(new BorderLayout());

        JPanel pnlCampos = new JPanel(new GridLayout(3, 1));
        JTextField txtNovoNome = new JTextField();
        JTextField txtNovaSenha = new JTextField();
        String textoLabelNome = comoAdmin ? "Novo Nome (vazio para manter):" : "Novo Nome:";
        String textoLabelSenha = comoAdmin ? "Nova Senha 6 dígitos (vazio para manter):" : "Nova Senha (exatos 6 dígitos):";
        
        pnlCampos.add(new JLabel(textoLabelNome));
        pnlCampos.add(txtNovoNome);
        pnlCampos.add(new JLabel(textoLabelSenha));
        pnlCampos.add(txtNovaSenha);

        JButton btnSalvar = new JButton("Salvar Alterações");
        String finalAlvo = alvo;
        
        btnSalvar.addActionListener(ev ->{
            String n = txtNovoNome.getText().trim();
            String s = txtNovaSenha.getText().trim();

            JsonObject json = new JsonObject();
            if(comoAdmin){
                json.addProperty("op", "atualizarUsuarioAdmin");
                json.addProperty("token", "adm");
                json.addProperty("usuario", finalAlvo);
                
                if(!n.isEmpty()) json.addProperty("nome", n);
                else json.add("nome", com.google.gson.JsonNull.INSTANCE);
                
                if(!s.isEmpty()) json.addProperty("senha", s);
                else json.add("senha", com.google.gson.JsonNull.INSTANCE);
            }else{
                if(n.isEmpty() || s.isEmpty()){
                    JOptionPane.showMessageDialog(dialog, "Usuário Comum não pode enviar campos vazios!");
                    return;
                }
                json.addProperty("op", "atualizarUsuario");
                json.addProperty("token", tokenSessao);
                json.addProperty("nome", n);
                json.addProperty("senha", s);
            }
            enviarAssincrono(json);
            dialog.dispose();
        });

        dialog.add(pnlCampos, BorderLayout.CENTER);
        dialog.add(btnSalvar, BorderLayout.SOUTH);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void efetuarDelecaoUsuario(boolean comoAdmin){
        if(comoAdmin){
            String alvo = JOptionPane.showInputDialog(this, "ADMIN: Digite o [usuario] que deseja deletar:");
            if(alvo != null && !alvo.trim().isEmpty()){
                JsonObject json = new JsonObject();
                json.addProperty("op", "deletarUsuarioAdmin");
                json.addProperty("token", "adm");
                json.addProperty("usuario", alvo);
                enviarAssincrono(json);
            }
        }else{
            if(JOptionPane.showConfirmDialog(this, "Deletar sua conta permanentemente?") == 0) {
                JsonObject json = new JsonObject();
                json.addProperty("op", "deletarUsuario");
                json.addProperty("token", tokenSessao);
                enviarAssincrono(json);
            }
        }
    }

    private void pedirListaUsuarios() {
        JsonObject json = new JsonObject();
        json.addProperty("op", "listarUsuariosLogados");
        json.addProperty("token", tokenSessao);
        enviarAssincrono(json);
    }

    private void enviarMensagemPrivada() {
        String alvo = listUsuariosOnline.getSelectedValue();
        String msg = txtMensagem.getText().trim();
        
        if (alvo == null) {
            JOptionPane.showMessageDialog(this, "Selecione um usuário na lista para enviar privado.");
            return;
        }
        if (msg.isEmpty()) return;

        JsonObject json = new JsonObject();
        json.addProperty("op", "enviarMensagem");
        json.addProperty("token", tokenSessao);
        json.addProperty("destinatario", alvo);
        json.addProperty("mensagem", msg);
        enviarAssincrono(json);

        areaChat.append("Você -> [" + alvo + "]: " + msg + "\n");
        txtMensagem.setText("");
    }

    private void enviarMensagemBroadcast() {
        String msg = txtMensagem.getText().trim();
        if (msg.isEmpty()) return;

        JsonObject json = new JsonObject();
        json.addProperty("op", "enviarMensagem");
        json.addProperty("token", tokenSessao);
        json.addProperty("destinatario", "/todos");
        json.addProperty("mensagem", msg);
        enviarAssincrono(json);

        areaChat.append("Você (chat geral) -> [TODOS]: " + msg + "\n");
        txtMensagem.setText("");
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> new ciente_class().setVisible(true));
    }
}