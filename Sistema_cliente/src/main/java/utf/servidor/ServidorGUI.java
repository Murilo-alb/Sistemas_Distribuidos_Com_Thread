package utf.servidor;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class ServidorGUI extends JFrame {

    // --- INTERFACE GRÁFICA ---
    private JTextField txtPorta;
    private JButton btnIniciar;
    private JButton btnParar;
    private JTextArea areaLog;

    // --- REDE E BANCO DE DADOS ---
    private ServerSocket serverSocket;
    private boolean rodando = false;
    private BancoDeDados banco; 
    
    // --- LISTA DE CLIENTES (Para evitar os "Sockets Zumbis") ---
    private List<Socket> clientesAtivos = new ArrayList<>();

    public ServidorGUI() {
        setTitle("Servidor Profissional MVC - Entrega 1");
        setSize(600, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Inicializa o nosso Banco de Dados
        banco = new BancoDeDados();

        // --- PAINEL TOPO ---
        JPanel painelTopo = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        painelTopo.add(new JLabel("Porta do Servidor:"));
        txtPorta = new JTextField("12345", 8);
        painelTopo.add(txtPorta);

        btnIniciar = new JButton("Iniciar Servidor");
        btnParar = new JButton("Desligar");
        btnParar.setEnabled(false);

        painelTopo.add(btnIniciar);
        painelTopo.add(btnParar);
        add(painelTopo, BorderLayout.NORTH);

        // --- PAINEL CENTRO ---
        areaLog = new JTextArea();
        areaLog.setEditable(false);
        areaLog.setBackground(new Color(230, 245, 255)); 
        areaLog.setLineWrap(true);
        areaLog.setWrapStyleWord(true);

        JScrollPane scrollLog = new JScrollPane(areaLog);
        scrollLog.setBorder(BorderFactory.createTitledBorder("Monitoramento do Servidor"));
        add(scrollLog, BorderLayout.CENTER);

        // --- EVENTOS ---
        btnIniciar.addActionListener(e -> iniciarServidor());
        btnParar.addActionListener(e -> pararServidor());

        logSistema("SUCESSO: Sistema iniciado. Banco de Dados carregado.");
    }

    private void iniciarServidor() {
        int porta;
        try {
            porta = Integer.parseInt(txtPorta.getText());
        } catch (NumberFormatException e) {
            logSistema("ERRO: Porta inválida.");
            return;
        }

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(porta);
                rodando = true;

                SwingUtilities.invokeLater(() -> {
                    btnIniciar.setEnabled(false);
                    txtPorta.setEnabled(false);
                    btnParar.setEnabled(true);
                    logSistema("Servidor INICIADO na porta " + porta + ". Aguardando clientes...");
                });

                while (rodando) {
                    Socket socketCliente = serverSocket.accept();
                    
                    // 1. Adiciona o novo cliente na nossa lista de ativos
                    clientesAtivos.add(socketCliente);
                    
                    logSistema("NOVO CLIENTE: " + socketCliente.getInetAddress().getHostAddress() + " conectou.");
                    
                    // Passamos o cliente para o Tratador
                    new Thread(new TratadorCliente(socketCliente, banco, this::logSistema)).start();
                }

            } catch (SocketException se) {
                logSistema("Servidor DESLIGADO.");
            } catch (IOException e) {
                logSistema("ERRO CRÍTICO: " + e.getMessage());
            }
        }).start();
    }

    private void pararServidor() {
        rodando = false;
        try {
            // 1. Fecha a porta principal do servidor (impede conexões novas)
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            // 2. Derruba ativamente todos os clientes que já estavam dentro
            for (Socket cliente : clientesAtivos) {
                if (cliente != null && !cliente.isClosed()) {
                    cliente.close(); // Corta a conexão à força
                }
            }
            
            // 3. Limpa a lista, pois todos foram expulsos
            clientesAtivos.clear();
            
            btnIniciar.setEnabled(true);
            txtPorta.setEnabled(true);
            btnParar.setEnabled(false);
        } catch (IOException e) {
            logSistema("Erro ao parar servidor: " + e.getMessage());
        }
    }

    private void logSistema(String mensagem) {
        SwingUtilities.invokeLater(() -> {
            areaLog.append(mensagem + "\n");
            areaLog.setCaretPosition(areaLog.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ServidorGUI().setVisible(true);
        });
    }
}