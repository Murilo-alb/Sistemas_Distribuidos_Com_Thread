package utf.servidor;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServidorGUI extends JFrame {

    private JTextField txtPorta;
    private JButton btnIniciar, btnDesligar;
    private JTextArea areaMonitoramento;
    
    private ServerSocket serverSocket;
    private boolean rodando = false;
    private BancoDeDados banco;

    public ServidorGUI() {
        banco = new BancoDeDados(); 
        
        setTitle("Servidor SINGLE-THREAD (Fila Estrita)");
        setSize(700, 450);
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
        JScrollPane scroll = new JScrollPane(areaMonitoramento);
        scroll.setBorder(BorderFactory.createTitledBorder("Log de Rede (Monitoramento)"));
        add(scroll, BorderLayout.CENTER);

        btnIniciar.addActionListener(e -> iniciarServidor());
        btnDesligar.addActionListener(e -> desligarServidor());
    }

    private void iniciarServidor() {
        final int porta;
        try {
            porta = Integer.parseInt(txtPorta.getText());
        } catch (NumberFormatException e) {
            log("ERRO: Porta inválida.");
            return;
        }

        rodando = true;
        btnIniciar.setEnabled(false);
        btnDesligar.setEnabled(true);
        txtPorta.setEnabled(false);

        log("AVISO: Operando em FILA ESTRITA. Atendimento 100% síncrono.");

        // O laço de rede é isolado em um processo de fundo para não congelar os botões da tela.
        // O atendimento aos clientes CONTINUA SEM MULTITHREADING (um de cada vez).
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(porta);
                log("SUCESSO: Banco de Dados carregado na RAM.");
                log("Servidor rodando na porta " + porta + ".");

                while (rodando) {
                    log("\n[ CAIXA LIVRE ] Aguardando próximo cliente...");
                    
                    // Fica bloqueado aqui esperando conexão, mas a interface continua livre
                    Socket socketCliente = serverSocket.accept();
                    log("NOVO CLIENTE: " + socketCliente.getInetAddress().getHostAddress() + " conectou.");
                    
                    TratadorCliente tratador = new TratadorCliente(socketCliente, banco, this::log);
                    
                    // ATENDIMENTO BLOQUEANTE: Só sai daqui quando o cliente der logout ou fechar
                    tratador.processar(); 
                    
                    log("Cliente encerrou a conexão. Fila andou!");
                }
            } catch (IOException e) {
                if (rodando) {
                    log("ERRO no servidor: " + e.getMessage());
                }
            }
        }).start();
    }

    private void desligarServidor() {
        rodando = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            log("Servidor DESLIGADO.");
        } catch (IOException e) {
            log("Erro ao desligar: " + e.getMessage());
        }
        btnIniciar.setEnabled(true);
        btnDesligar.setEnabled(false);
        txtPorta.setEnabled(true);
    }

    public void log(String mensagem) {
        // Envia o texto de volta para a esteira da interface gráfica de forma segura
        SwingUtilities.invokeLater(() -> {
            areaMonitoramento.append(mensagem + "\n");
            areaMonitoramento.setCaretPosition(areaMonitoramento.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        SwingUtilities.invokeLater(() -> new ServidorGUI().setVisible(true));
    }
}