package utf.servidor;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ServidorGUI extends JFrame {

    private JTextField txtPorta;
    private JButton btnIniciar, btnDesligar;
    private JTextArea areaMonitoramento;
    private DefaultListModel<String> modelUsuariosLogados;
    private JList<String> listUsuariosLogados;
    
    private ServerSocket serverSocket;
    private boolean rodando = false;
    private BancoDeDados banco;

    // Gerencia as streams e os sockets dos clientes (Thread-Safe)
    private Map<String, PrintWriter> clientesConectados = new ConcurrentHashMap<>();
    private Map<String, Socket> socketsConectados = new ConcurrentHashMap<>();

    public ServidorGUI() {
        banco = new BancoDeDados(); 
        
        setTitle("Servidor MULTI-THREAD - EP3");
        setSize(850, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel painelTopo = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        painelTopo.add(new JLabel("Porta do Servidor:"));
        txtPorta = new JTextField("12345", 8);
        painelTopo.add(txtPorta);
        
        btnIniciar = new JButton("Iniciar Servidor");
        btnDesligar = new JButton("Desligar");
        btnDesligar.setEnabled(false);
        
        painelTopo.add(btnIniciar);
        painelTopo.add(btnDesligar);
        add(painelTopo, BorderLayout.NORTH);

        areaMonitoramento = new JTextArea();
        areaMonitoramento.setEditable(false);
        areaMonitoramento.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollLog = new JScrollPane(areaMonitoramento);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Log de Rede (Monitoramento)"));

        modelUsuariosLogados = new DefaultListModel<>();
        listUsuariosLogados = new JList<>(modelUsuariosLogados);
        JScrollPane scrollUsers = new JScrollPane(listUsuariosLogados);
        scrollUsers.setBorder(BorderFactory.createTitledBorder("Usuários Logados"));
        scrollUsers.setPreferredSize(new Dimension(200, 0));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollLog, scrollUsers);
        split.setResizeWeight(0.8);
        add(split, BorderLayout.CENTER);

        btnIniciar.addActionListener(e -> iniciarServidor());
        btnDesligar.addActionListener(e -> desligarServidor());
    }

    private void iniciarServidor(){
        final int porta;
        try{
            porta = Integer.parseInt(txtPorta.getText());
        }catch(NumberFormatException e){
            log("ERRO: Porta inválida.");
            return;
        }

        rodando = true;
        btnIniciar.setEnabled(false);
        btnDesligar.setEnabled(true);
        txtPorta.setEnabled(false);

        log("AVISO: Operando em MULTI-THREAD. Cada cliente roda em uma Thread isolada.");

        new Thread(() ->{
            try{
                serverSocket = new ServerSocket(porta);
                log("Servidor rodando na porta " + porta + ".");

                while(rodando){
                    Socket socketCliente = serverSocket.accept();
                    log("NOVO CLIENTE CONECTADO: " + socketCliente.getInetAddress().getHostAddress());
                    
                    TratadorCliente tratador = new TratadorCliente(socketCliente, banco, this);
                    new Thread(tratador).start(); 
                }
            }catch(IOException e){
                if(rodando) log("ERRO no servidor: " + e.getMessage());
            }
        }).start();
    }

    private void desligarServidor(){
        rodando = false;
        try{
            if(serverSocket != null && !serverSocket.isClosed()){
                serverSocket.close();
            }
            // Derruba todos os sockets abertos
            for (Socket s : socketsConectados.values()) {
                try { s.close(); } catch (Exception e) {}
            }
            log("Servidor DESLIGADO.");
        }catch(IOException e){
            log("Erro ao desligar: " + e.getMessage());
        }
        btnIniciar.setEnabled(true);
        btnDesligar.setEnabled(false);
        txtPorta.setEnabled(true);
        clientesConectados.clear();
        socketsConectados.clear();
        atualizarInterfaceUsuarios();
    }

    public void log(String mensagem){
        SwingUtilities.invokeLater(() ->{
            areaMonitoramento.append(mensagem + "\n");
            areaMonitoramento.setCaretPosition(areaMonitoramento.getDocument().getLength());
        });
    }

    //MÉTODOS DE GERENCIAMENTO
    public void registrarClienteAtivo(String usuario, PrintWriter out, Socket socket) {
        clientesConectados.put(usuario, out);
        socketsConectados.put(usuario, socket);
        atualizarInterfaceUsuarios();
    }

    public void removerClienteAtivo(String usuario) {
        if(usuario != null) {
            clientesConectados.remove(usuario);
            socketsConectados.remove(usuario);
            atualizarInterfaceUsuarios();
        }
    }

    public void forcarDesconexao(String usuario) {
        Socket s = socketsConectados.get(usuario);
        if (s != null) {
            try { 
                s.close(); // Fecha o TCP na força, ativando a exceção no cliente
            } catch (Exception e) {}
        }
        removerClienteAtivo(usuario);
    }

    private void atualizarInterfaceUsuarios() {
        SwingUtilities.invokeLater(() -> {
            modelUsuariosLogados.clear();
            for (String user : clientesConectados.keySet()) {
                modelUsuariosLogados.addElement(user);
            }
        });
    }

    public Set<String> getUsuariosOnline() {
        return clientesConectados.keySet();
    }

    public PrintWriter getStreamDestinatario(String usuario) {
        return clientesConectados.get(usuario);
    }

    public static void main(String[] args){
        try{ UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch(Exception e) {}
        SwingUtilities.invokeLater(() -> new ServidorGUI().setVisible(true));
    }
}